package com.auction.bidding.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID


@Entity
@Table(name = "auction_state")
class AuctionState(
    @Id
    @Column(name = "product_id")
    val productId: UUID,

    @Column(name = "seller_id", nullable = false)
    val sellerId: UUID,

    @Column(name = "current_price", nullable = false)
    var currentPrice: BigDecimal,

    @Column(name = "current_bidder_id")
    var currentBidderId: UUID? = null,

    @Column(name = "auction_end_at", nullable = false)
    val auctionEndAt: Instant,

    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(nullable = false, length = 3)
    val currency: String = "VND"
)
