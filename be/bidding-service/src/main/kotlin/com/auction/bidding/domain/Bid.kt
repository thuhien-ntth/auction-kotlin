package com.auction.bidding.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID


@Entity
@Table(name = "bids")
class Bid(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "product_id", nullable = false)
    val productId: UUID,

    @Column(name = "bidder_id", nullable = false)
    val bidderId: UUID,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    val accepted: Boolean,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
