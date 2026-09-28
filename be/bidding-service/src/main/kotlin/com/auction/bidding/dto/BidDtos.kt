package com.auction.bidding.dto

import com.auction.bidding.domain.AuctionState
import com.auction.bidding.domain.Bid
import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PlaceBidRequest(@field:DecimalMin(value = "0.01", message = "Giá đặt phải lớn hơn 0") val amount: BigDecimal)

data class BidResponse(
    val id: UUID,
    val productId: UUID,
    val bidderId: UUID,
    val amount: BigDecimal,
    val accepted: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun from(b: Bid) = BidResponse(b.id!!, b.productId, b.bidderId, b.amount, b.accepted, b.createdAt)
    }
}

data class AuctionStateResponse(
    val productId: UUID,
    val currentPrice: BigDecimal,
    val currentBidderId: UUID?,
    val auctionEndAt: Instant,
    val currency: String = "VND",
    // Ghép từ catalog-service (batch) để app hiển thị đúng tên + ảnh
    val title: String? = null,
    val imageUrl: String? = null
) {
    companion object {
        fun from(s: AuctionState) = AuctionStateResponse(s.productId, s.currentPrice, s.currentBidderId, s.auctionEndAt, s.currency)
    }
}
