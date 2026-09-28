package com.auction.bidding

import com.auction.bidding.client.CatalogClient
import com.auction.bidding.support.BiddingIntegrationTestBase
import com.auction.bidding.support.JwtTestSupport
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.BDDMockito.given
import java.io.File
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * Benchmark ĐẦU-CUỐI của bidding-service thật (Tomcat + JwtAuthFilter + Hibernate + HikariCP + PostgreSQL),
 * chỉ mock CatalogClient. Mặc định TẮT; bật bằng biến môi trường BENCH_REAL=true và trỏ datasource sang PostgreSQL:
 *   SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD,
 *   SPRING_JPA_HIBERNATE_DDL_AUTO=none, SPRING_FLYWAY_ENABLED=true
 * Tham số: BENCH_PLAN="random:50,ascending:200,..." ; BENCH_REPS=10 ; BENCH_OUT=/duong/dan/ket_qua.json
 */
@EnabledIfEnvironmentVariable(named = "BENCH_REAL", matches = "true")
class RealPostgresLoadBench : BiddingIntegrationTestBase() {

    @org.springframework.boot.test.web.server.LocalServerPort
    private var port: Int = 0

    @Test
    fun bench() {
        val plan = (System.getenv("BENCH_PLAN") ?: "random:50").split(",").map { val (o, n) = it.split(":"); o to n.toInt() }
        val reps = (System.getenv("BENCH_REPS") ?: "3").toInt()
        val out = System.getenv("BENCH_OUT") ?: "bench_real.json"
        val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10)).build()
        val rows = mutableListOf<String>()
        // khởi động (JIT, pool) trước khi đo
        run(client, "random", 30, 0)
        for ((order, n) in plan) for (rep in 1..reps) {
            val r = run(client, order, n, rep)
            rows.add(r); println("BENCH $r")
        }
        File(out).writeText("[" + rows.joinToString(",") + "]")
    }

    private fun run(client: HttpClient, order: String, n: Int, rep: Int): String {
        bidRepository.deleteAll(); auctionStateRepository.deleteAll()
        val productId = UUID.randomUUID(); val seller = UUID.randomUUID(); val base = BigDecimal("1000000")
        given(catalogClient.getProduct(productId)).willReturn(
            CatalogClient.CatalogProductInfo(productId, seller, base, "ACTIVE", Instant.now().plusSeconds(3600)))
        val preinit = System.getenv("BENCH_PREINIT") != "false"
        if (preinit) {   // 1 bid khởi tạo auction_state trước, không tính vào số đo (giá 500 < mọi giá đo)
            val warm = UUID.randomUUID()
            client.send(HttpRequest.newBuilder(URI("http://localhost:$port/products/$productId/bids"))
                .header("Authorization", "Bearer ${JwtTestSupport.tokenFor(warm, false)}").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""{"amount": ${base.add(BigDecimal(500))}}""")).build(), HttpResponse.BodyHandlers.discarding())
        }
        var plan = (1..n).map { UUID.randomUUID() to it }
        plan = if (order == "random") plan.shuffled() else plan.sortedBy { it.second }
        val tokens = plan.associate { it.first to JwtTestSupport.tokenFor(it.first, false) }
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(n)
        val futures = plan.map { (bidder, k) ->
            pool.submit<Pair<Int, Double>> {
                val req = HttpRequest.newBuilder(URI("http://localhost:$port/products/$productId/bids"))
                    .header("Authorization", "Bearer ${tokens[bidder]}").header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""{"amount": ${base.add(BigDecimal(k * 1000))}}""")).build()
                start.await()
                val t0 = System.nanoTime()
                val resp = client.send(req, HttpResponse.BodyHandlers.discarding())
                resp.statusCode() to (System.nanoTime() - t0) / 1e6
            }
        }
        Thread.sleep(300)
        val w0 = System.nanoTime(); start.countDown()
        val res = futures.map { it.get() }
        val wall = (System.nanoTime() - w0) / 1e9
        pool.shutdown()
        val lat = res.map { it.second }.sorted()
        val cnt = res.groupingBy { it.first }.eachCount()
        val state = auctionStateRepository.findById(productId).orElse(null)
        val rowsAcc = bidRepository.findAll().count { it.productId == productId && it.accepted }
        val rowsRej = bidRepository.findAll().count { it.productId == productId && !it.accepted }
        val correct = state != null && state.currentPrice.compareTo(base.add(BigDecimal(n * 1000))) == 0
        val ok = cnt[200] ?: 0; val rej = cnt[409] ?: 0; val busy = cnt[503] ?: 0
        val other = res.size - ok - rej - busy
        val audit = rowsAcc == ok + (if (preinit) 1 else 0) && (rowsRej == rej || System.getenv("BIDDING_RECORD_REJECTED_BIDS") == "false")
        return """{"order":"$order","n":$n,"rep":$rep,"preinit":$preinit,"ok":$ok,"rejected":$rej,"busy503":$busy,"other":$other,""" +
            """"avg_ms":${lat.average()},"p95_ms":${lat[minOf(lat.size - 1, (0.95 * lat.size).toInt())]},"p99_ms":${lat[minOf(lat.size - 1, (0.99 * lat.size).toInt())]},""" +
            """"throughput":${res.size / wall},"wall_s":$wall,"final_price_correct":$correct,"rows_accepted":$rowsAcc,"rows_rejected":$rowsRej,"audit_ok":$audit}"""
    }
}
