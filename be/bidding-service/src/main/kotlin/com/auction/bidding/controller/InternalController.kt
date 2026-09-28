package com.auction.bidding.controller

import com.auction.bidding.repository.AuctionStateRepository
import com.auction.bidding.repository.BidRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

// Chỉ gọi trong docker network nội bộ và yêu cầu header X-Internal-Token (kiểm tra ở JwtAuthFilter).
// Dùng bởi catalog-service: đọc giá (API composition) và lấy kết quả phiên (AuctionScheduler).
@RestController
@RequestMapping("/internal")
class InternalController(
    private val auctionStateRepository: AuctionStateRepository,
    private val bidRepository: BidRepository
) {

    @GetMapping("/products/{id}/current-price")
    fun currentPrice(@PathVariable id: UUID): CurrentPriceResponse {
        val state = auctionStateRepository.findById(id)
        // Chưa có bid nào -> chưa có AuctionState -> coi như chưa có giá hiện tại (catalog-service tự fallback về startPrice)
        return CurrentPriceResponse(state.map { it.currentPrice }.orElse(null))
    }

    @GetMapping("/products/{id}/result")
    fun result(@PathVariable id: UUID): AuctionResultResponse {
        val state = auctionStateRepository.findById(id)
        return AuctionResultResponse(
            winnerId = state.map { it.currentBidderId }.orElse(null),
            finalPrice = state.map { it.currentPrice }.orElse(null)
        )
    }

    // Phiên bản batch của /result cho AuctionScheduler: 1 lời gọi cho cả lô sản phẩm thay vì N lời gọi.
    // Sản phẩm không có trong kết quả = chưa từng có auction_state = chưa có bid nào.
    @PostMapping("/products/results")
    fun results(@RequestBody body: ResultsRequest): List<AuctionResultItem> {
        val ids = body.productIds.distinct().take(MAX_BATCH)
        return auctionStateRepository.findAllById(ids).map {
            AuctionResultItem(productId = it.productId, winnerId = it.currentBidderId, finalPrice = it.currentPrice)
        }
    }

    // Batch: số người tham gia đấu giá cho cả trang danh sách sản phẩm (catalog gọi 1 lần / trang).
    @PostMapping("/products/bidder-counts")
    fun bidderCounts(@RequestBody body: ResultsRequest): List<BidderCountItem> {
        val ids = body.productIds.distinct().take(MAX_BATCH)
        if (ids.isEmpty()) return emptyList()
        return bidRepository.countBiddersByProductIds(ids).map {
            BidderCountItem(productId = it[0] as UUID, bidderCount = (it[1] as Number).toLong())
        }
    }

    data class BidderCountItem(val productId: UUID, val bidderCount: Long)

    data class CurrentPriceResponse(val currentPrice: BigDecimal?)

    data class AuctionResultResponse(val winnerId: UUID?, val finalPrice: BigDecimal?)

    data class ResultsRequest(val productIds: List<UUID>)

    data class AuctionResultItem(val productId: UUID, val winnerId: UUID?, val finalPrice: BigDecimal?)

    companion object {
        private const val MAX_BATCH = 500
    }
}
