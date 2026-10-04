package com.auction.bidding

import com.auction.bidding.client.CatalogClient
import com.auction.bidding.dto.PlaceBidRequest
import com.auction.bidding.support.BiddingIntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class ConcurrentBiddingTest : BiddingIntegrationTestBase() {

    @Test
    fun `C1 nhieu bidder dong thoi khong lam mat cap nhat`() {
        val productId = UUID.randomUUID()
        val seller = UUID.randomUUID()
        val base = BigDecimal("1000000")
        given(catalogClient.getProduct(productId)).willReturn(
            CatalogClient.CatalogProductInfo(productId, seller, base, "ACTIVE", Instant.now().plusSeconds(3600))
        )

        val n = 30
        val amounts = (1..n).associate { UUID.randomUUID() to base.add(BigDecimal(it * 1000)) }
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(n)
        val futures = amounts.map { (bidder, amount) ->
            pool.submit<Pair<HttpStatus, BigDecimal>> {
                start.await()
                val headers = authHeaders(bidder).apply { contentType = MediaType.APPLICATION_JSON }
                val response = restTemplate.exchange(
                    "/products/{id}/bids", HttpMethod.POST,
                    HttpEntity(PlaceBidRequest(amount), headers), String::class.java, productId
                )
                HttpStatus.valueOf(response.statusCode.value()) to amount
            }
        }
        start.countDown()
        val results = futures.map { it.get(60, TimeUnit.SECONDS) }
        pool.shutdown()

        
        assertThat(results.map { it.first })
            .allMatch { it == HttpStatus.OK || it == HttpStatus.CONFLICT || it == HttpStatus.SERVICE_UNAVAILABLE }

        val accepted = results.filter { it.first == HttpStatus.OK }.map { it.second }
        assertThat(accepted).isNotEmpty()

        
        val state = auctionStateRepository.findById(productId).orElseThrow()
        assertThat(state.currentPrice).isEqualByComparingTo(accepted.max())

        
        val acceptedRows = bidRepository.findAll().filter { it.productId == productId && it.accepted }
        assertThat(acceptedRows).hasSize(accepted.size)
        assertThat(acceptedRows.map { it.amount }).allMatch { it <= state.currentPrice }
    }
}
