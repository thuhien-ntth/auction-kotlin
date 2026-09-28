package com.auction.catalog.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.io.Serializable
import java.math.BigDecimal
import java.util.UUID

/**
 * Gọi sang bidding-service — API Composition pattern (ARCHITECTURE_DESIGN.md mục 3.2/4).
 *
 * - Có timeout kết nối/đọc thật cho RestClient (trước đây không có nên lời gọi có thể treo vô hạn).
 * - Cache Redis TTL ngắn cho currentPrice, NHƯNG kết quả fallback (bidding-service lỗi) không bao giờ
 *   được cache (unless = "#result.degraded"), tránh giá khởi điểm sai bị giữ thêm vài giây sau lỗi thoáng qua.
 * - KHÔNG dùng sync = true: Spring cấm kết hợp sync = true với unless (IllegalStateException lúc gọi), khiến mọi
 *   lần đọc rơi vào fallback. Đánh đổi: khi cache hết hạn, nhiều luồng có thể cùng hỏi bidding-service một lượt (TTL 3 s, lời gọi rẻ).
 * - Gắn X-Internal-Token và chuyển tiếp X-Trace-Id cho mọi lời gọi.
 */
@Component
class BiddingClient(
    @Value("\${bidding-service.base-url}") baseUrl: String,
    @Value("\${bidding-service.connect-timeout-ms:300}") connectTimeoutMs: Int,
    @Value("\${bidding-service.read-timeout-ms:800}") readTimeoutMs: Int,
    @Value("\${internal.token:dev-internal-token}") internalToken: String
) {
    private val client: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(connectTimeoutMs)
                setReadTimeout(readTimeoutMs)
            }
        )
        .defaultHeader("X-Internal-Token", internalToken)
        .requestInterceptor { request, body, execution ->
            MDC.get("traceId")?.let { request.headers.set("X-Trace-Id", it) }
            execution.execute(request, body)
        }
        .build()

    @Cacheable(cacheNames = ["currentPriceV2"], key = "#productId", unless = "#result.degraded")
    @CircuitBreaker(name = "bidding", fallbackMethod = "fallbackPrice")
    @Retry(name = "bidding")
    fun getCurrentPrice(productId: UUID): CurrentPrice {
        val body = client.get()
            .uri("/internal/products/{id}/current-price", productId)
            .retrieve()
            .body(CurrentPriceResponse::class.java)
        // price = null nghĩa là chưa có bid nào (đây là kết quả hợp lệ, được phép cache)
        return CurrentPrice(price = body?.currentPrice)
    }

    @Suppress("UNUSED_PARAMETER")
    fun fallbackPrice(productId: UUID, ex: Exception): CurrentPrice {
        // bidding-service không phản hồi kịp -> caller dùng giá khởi điểm; degraded = true để KHÔNG cache
        log.warn("Fallback giá hiện tại cho product={} do lỗi: {}", productId, ex.toString())
        return CurrentPrice(price = null, degraded = true)
    }

    // Dùng bởi AuctionScheduler: lấy kết quả của cả LÔ sản phẩm trong 1 lời gọi (thay vì N lời gọi).
    // KHÔNG cache — quyết định ghi một lần, phải luôn dùng dữ liệu mới nhất.
    // Trả về null khi bidding-service không phản hồi được -> job dừng lượt này và thử lại lượt sau,
    // thay vì đoán sai trạng thái. Sản phẩm vắng mặt trong Map = chưa từng có bid.
    @CircuitBreaker(name = "bidding", fallbackMethod = "fallbackResults")
    @Retry(name = "bidding")
    fun getAuctionResults(productIds: List<UUID>): Map<UUID, AuctionResult>? {
        val items = client.post()
            .uri("/internal/products/results")
            .contentType(MediaType.APPLICATION_JSON)
            .body(ResultsRequest(productIds))
            .retrieve()
            .body(Array<AuctionResultItem>::class.java)
            ?: return null
        return items.associate { it.productId to AuctionResult(it.winnerId, it.finalPrice) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun fallbackResults(productIds: List<UUID>, ex: Exception): Map<UUID, AuctionResult>? {
        log.warn("Không lấy được kết quả phiên cho {} sản phẩm: {}", productIds.size, ex.toString())
        return null
    }

    // Số người tham gia đấu giá cho cả trang danh sách (1 lời gọi / trang, không N+1).
    // Lỗi -> trả map rỗng (hiển thị 0) chứ không làm hỏng màn tìm kiếm.
    @CircuitBreaker(name = "bidding", fallbackMethod = "fallbackBidderCounts")
    fun getBidderCounts(productIds: List<UUID>): Map<UUID, Long> {
        if (productIds.isEmpty()) return emptyMap()
        val items = client.post()
            .uri("/internal/products/bidder-counts")
            .contentType(MediaType.APPLICATION_JSON)
            .body(ResultsRequest(productIds))
            .retrieve()
            .body(Array<BidderCountItem>::class.java)
            ?: return emptyMap()
        return items.associate { it.productId to it.bidderCount }
    }

    @Suppress("UNUSED_PARAMETER")
    fun fallbackBidderCounts(productIds: List<UUID>, ex: Exception): Map<UUID, Long> {
        log.warn("Không lấy được số người tham gia cho {} sản phẩm: {}", productIds.size, ex.toString())
        return emptyMap()
    }

    companion object {
        private val log = LoggerFactory.getLogger(BiddingClient::class.java)
    }

    // price = null khi chưa có bid nào; degraded = true khi đây là giá trị fallback do lỗi
    data class CurrentPrice(val price: BigDecimal?, val degraded: Boolean = false) : Serializable

    data class CurrentPriceResponse(val currentPrice: BigDecimal?)

    data class ResultsRequest(val productIds: List<UUID>)
    data class AuctionResultItem(val productId: UUID, val winnerId: UUID?, val finalPrice: BigDecimal?)
    data class BidderCountItem(val productId: UUID, val bidderCount: Long)
    data class AuctionResult(val winnerId: UUID?, val finalPrice: BigDecimal?)
}
