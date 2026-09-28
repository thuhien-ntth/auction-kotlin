package com.auction.catalog.dto

import com.auction.catalog.domain.Product
import com.auction.catalog.domain.ProductStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateProductRequest(
    @field:NotBlank(message = "Vui lòng nhập tên sản phẩm") val title: String,
    val description: String = "",
    val category: String = "OTHER",
    @field:DecimalMin(value = "0.01", message = "Giá khởi điểm phải lớn hơn 0") val startPrice: BigDecimal,
    // VND / USD / EUR — mặc định VND để client cũ không gửi field này vẫn chạy
    val currency: String = "VND",
    // Thời gian đấu giá do người bán đặt (ISO 8601, ví dụ 2026-09-21T10:00:00Z)
    @field:NotNull(message = "Vui lòng chọn thời gian bắt đầu") val auctionStartAt: Instant?,
    @field:NotNull(message = "Vui lòng chọn thời gian kết thúc") val auctionEndAt: Instant?
)

data class RejectProductRequest(@field:NotBlank(message = "Vui lòng nhập lý do từ chối") val reason: String)

// Dùng cho Product detail (USEDS0030000): ghép currentPrice lấy từ bidding-service.
data class ProductResponse(
    val id: UUID,
    val sellerId: UUID,
    val title: String,
    val description: String,
    val category: String,
    val startPrice: BigDecimal,
    val currency: String,
    val auctionStartAt: Instant,
    val auctionEndAt: Instant,
    val currentPrice: BigDecimal,
    val status: ProductStatus,
    val rejectionReason: String?,
    val imageUrl: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(p: Product, currentPrice: BigDecimal) = ProductResponse(
            id = p.id!!,
            sellerId = p.sellerId,
            title = p.title,
            description = p.description,
            category = p.category,
            startPrice = p.startPrice,
            currency = p.currency,
            auctionStartAt = p.auctionStartAt,
            auctionEndAt = p.auctionEndAt,
            currentPrice = currentPrice,
            status = p.status,
            rejectionReason = p.rejectionReason,
            imageUrl = imageUrlOf(p),
            createdAt = p.createdAt
        )
    }
}

// Dùng cho danh sách (Product search, Exhibition management) — KHÔNG gọi bidding-service
// cho từng dòng để tránh N+1 cross-service call (tối ưu hiệu năng, xem mục 4 trong doc thiết kế).
data class ProductSummaryResponse(
    val id: UUID,
    val title: String,
    val category: String,
    val startPrice: BigDecimal,
    val currency: String,
    val status: ProductStatus,
    val auctionStartAt: Instant,
    val auctionEndAt: Instant,
    val imageUrl: String?,
    // Chỉ có giá trị khi status = REJECTED
    val rejectionReason: String? = null,
    // Số người tham gia đấu giá (bidder khác nhau) — chỉ điền ở màn tìm kiếm, lấy batch từ bidding-service
    val bidderCount: Long = 0
) {
    companion object {
        fun from(p: Product) = ProductSummaryResponse(
            id = p.id!!,
            title = p.title,
            category = p.category,
            startPrice = p.startPrice,
            currency = p.currency,
            status = p.status,
            auctionStartAt = p.auctionStartAt,
            auctionEndAt = p.auctionEndAt,
            imageUrl = imageUrlOf(p),
            rejectionReason = p.rejectionReason
        )
    }
}

// Đường dẫn TƯƠNG ĐỐI (không có "/" đầu, không có "/api") tới endpoint GET .../products/{id}/image
// của chính catalog-service — FE tự ghép với base URL đang dùng cho Retrofit
// (giống cách "products/{id}/bids" đã được ghép). Trả null nếu sản phẩm chưa có ảnh, để FE
// hiển thị placeholder thay vì gọi 1 request chắc chắn 404.
private fun imageUrlOf(p: Product): String? =
    if (p.imagePath != null) "products/${p.id}/image" else null

// Endpoint nội bộ cho bidding-service khi cần validate 1 bid (xem InternalController)
data class InternalProductInfo(
    val id: UUID,
    val sellerId: UUID,
    val startPrice: BigDecimal,
    val status: ProductStatus,
    val auctionStartAt: Instant,
    val auctionEndAt: Instant,
    val currency: String = "VND"
)

// Batch cho bidding-service: tên + ảnh sản phẩm để hiển thị list "Đang tham gia" / "Đã thắng"
data class ProductBriefRequest(val productIds: List<UUID>)
data class InternalProductBrief(
    val id: UUID,
    val title: String,
    val imageUrl: String?
) {
    companion object {
        fun from(p: Product) = InternalProductBrief(p.id!!, p.title, imageUrlOf(p))
    }
}
