package com.auction.app.network.model

import java.math.BigDecimal
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String
)

data class VerifyRequest(
    val email: String,
    val token: String
)

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val accessToken: String,
    val userId: String,
    val fullName: String,
    val isAdmin: Boolean
)

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0
)

data class ProductSummary(
    val id: String,
    val title: String,
    val category: String,
    val startPrice: BigDecimal,
    val status: String,
    val auctionStartAt: String?,
    val auctionEndAt: String?,
    val imageUrl: String? = null,
    val rejectionReason: String? = null,
    val currency: String = "VND",
    val bidderCount: Long = 0
)

data class ProductDetail(
    val id: String,
    val sellerId: String,
    val title: String,
    val description: String,
    val category: String,
    val startPrice: BigDecimal,
    val currentPrice: BigDecimal,
    val status: String,
    val rejectionReason: String?,
    val createdAt: String,
    val auctionStartAt: String?,
    val auctionEndAt: String?,
    val imageUrl: String? = null,
    val currency: String = "VND"
)

data class CreateProductRequest(
    val title: String,
    val description: String,
    val category: String,
    val startPrice: BigDecimal,
    val auctionStartAt: String?,
    val auctionEndAt: String?,
    val currency: String = "VND"
)

data class PlaceBidRequest(val amount: BigDecimal)
data class RejectProductRequest(val reason: String)
data class BidResponse(
    val id: String,
    val productId: String,
    val bidderId: String,
    val amount: BigDecimal,
    val accepted: Boolean,
    val createdAt: String
)

data class AuctionStateResponse(
    val productId: String,
    val currentPrice: BigDecimal,
    val currentBidderId: String?,
    val auctionEndAt: String,
    val currency: String = "VND",
    val title: String? = null,
    val imageUrl: String? = null
)

data class UserResponse(
    val id: String,
    val email: String,
    val fullName: String,
    val isAdmin: Boolean
)

data class CategoryDto(
    val id: String,
    val name: String
)

