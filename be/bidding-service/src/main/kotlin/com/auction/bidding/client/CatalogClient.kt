package com.auction.bidding.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.MDC
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Cố ý KHÔNG phụ thuộc module catalog-service ở compile-time (không có "shared contract"
 * library) — mỗi service build/deploy độc lập hoàn toàn, chỉ ràng buộc nhau qua REST
 * contract (JSON field name phải khớp thủ công). Đây là đánh đổi có chủ đích, xem
 * ARCHITECTURE_DESIGN.md mục 4, hàng "Bảo mật giữa các service" — cùng logic áp dụng
 * cho việc không share model class.
 *
 * Timeout kết nối/đọc được đặt tường minh: trước đây RestClient không có timeout nào nên
 * retry và circuit breaker chỉ phản ứng khi lời gọi THẤT BẠI, không phản ứng khi lời gọi TREO.
 * Mỗi lời gọi cũng gắn X-Internal-Token (endpoint /internal/... yêu cầu) và chuyển tiếp X-Trace-Id.
 */
@Component
class CatalogClient(
    @Value("\${catalog-service.base-url}") baseUrl: String,
    @Value("\${catalog-service.connect-timeout-ms:300}") connectTimeoutMs: Int,
    @Value("\${catalog-service.read-timeout-ms:800}") readTimeoutMs: Int,
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

    @CircuitBreaker(name = "catalog")
    @Retry(name = "catalog")
    fun getProduct(productId: UUID): CatalogProductInfo? =
        client.get()
            .uri("/internal/products/{id}", productId)
            .retrieve()
            .body(CatalogProductInfo::class.java)

    // Tên + ảnh cho list "Đang tham gia" / "Đã thắng" — 1 lời gọi / trang.
    // catalog lỗi -> trả map rỗng, list vẫn hiển thị (chỉ thiếu tên/ảnh) thay vì hỏng cả màn.
    @CircuitBreaker(name = "catalog", fallbackMethod = "fallbackBriefs")
    fun getProductBriefs(productIds: List<UUID>): Map<UUID, ProductBrief> {
        if (productIds.isEmpty()) return emptyMap()
        val items = client.post()
            .uri("/internal/products/briefs")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("productIds" to productIds))
            .retrieve()
            .body(Array<ProductBrief>::class.java)
            ?: return emptyMap()
        return items.associateBy { it.id }
    }

    @Suppress("UNUSED_PARAMETER")
    fun fallbackBriefs(productIds: List<UUID>, ex: Exception): Map<UUID, ProductBrief> {
        LoggerFactory.getLogger(CatalogClient::class.java)
            .warn("Không lấy được tên/ảnh cho {} sản phẩm: {}", productIds.size, ex.toString())
        return emptyMap()
    }

    data class ProductBrief(val id: UUID, val title: String, val imageUrl: String?)

    // Local DTO khớp field name JSON của catalog-service InternalProductInfo — chỉ cần
    // những field bidding-service thực sự dùng (status so sánh bằng String "ACTIVE").
    data class CatalogProductInfo(
        val id: UUID,
        val sellerId: UUID,
        val startPrice: BigDecimal,
        val status: String,
        val auctionEndAt: Instant?,
        val auctionStartAt: Instant? = null,
        val currency: String = "VND"
    )
}
