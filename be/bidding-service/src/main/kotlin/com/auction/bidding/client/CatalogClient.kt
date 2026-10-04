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
