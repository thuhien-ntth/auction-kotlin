package com.auction.catalog.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "seller_id", nullable = false)
    val sellerId: UUID,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var description: String = "",

    @Column(nullable = false)
    var category: String = "OTHER",

    @Column(name = "start_price", nullable = false)
    var startPrice: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProductStatus = ProductStatus.PENDING_APPROVAL,

    
    
    @Column(name = "auction_start_at", nullable = false)
    var auctionStartAt: Instant,

    @Column(name = "auction_end_at", nullable = false)
    var auctionEndAt: Instant,

    @Column(name = "rejection_reason")
    var rejectionReason: String? = null,

    
    
    @Column(name = "image_path")
    var imagePath: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    
    @Column(nullable = false, length = 3)
    var currency: String = "VND"
)
