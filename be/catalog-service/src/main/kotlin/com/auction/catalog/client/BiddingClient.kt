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
        
        return CurrentPrice(price = body?.currentPrice)
    }

    @Suppress("UNUSED_PARAMETER")
    fun fallbackPrice(productId: UUID, ex: Exception): CurrentPrice {
        
        log.warn("Fallback giá hiện tại cho product={} do lỗi: {}", productId, ex.toString())
        return CurrentPrice(price = null, degraded = true)
    }

    
    
    
    
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

    
    data class CurrentPrice(val price: BigDecimal?, val degraded: Boolean = false) : Serializable

    data class CurrentPriceResponse(val currentPrice: BigDecimal?)

    data class ResultsRequest(val productIds: List<UUID>)
    data class AuctionResultItem(val productId: UUID, val winnerId: UUID?, val finalPrice: BigDecimal?)
    data class BidderCountItem(val productId: UUID, val bidderCount: Long)
    data class AuctionResult(val winnerId: UUID?, val finalPrice: BigDecimal?)
}
