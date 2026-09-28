package com.auction.catalog.repository

import com.auction.catalog.domain.Product
import com.auction.catalog.domain.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

interface ProductRepository : JpaRepository<Product, UUID> {

    fun findBySellerId(sellerId: UUID, pageable: Pageable): Page<Product>

    fun findBySellerIdAndStatus(sellerId: UUID, status: ProductStatus, pageable: Pageable): Page<Product>

    // Product search (USEDS001F003): tìm theo tên trong tập status cho trước, có filter category.
    // - Người dùng thường: chỉ ACTIVE (đang trong thời gian đấu giá)
    // - Admin: tất cả sản phẩm mọi trạng thái, xem ProductService.search
    // Migration V3 tạo index gin(LOWER(title) gin_trgm_ops) khớp đúng biểu thức LOWER(p.title) dưới đây.
    @Query(
        """
        SELECT p FROM Product p
        WHERE p.status IN :statuses
        AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        AND (:category IS NULL OR p.category = :category)
        """
    )
    fun search(
        @Param("keyword") keyword: String?,
        @Param("category") category: String?,
        @Param("statuses") statuses: Collection<ProductStatus>,
        pageable: Pageable
    ): Page<Product>

    fun findByStatus(status: ProductStatus, pageable: Pageable): Page<Product>

    // Admin: danh sách sản phẩm đã được xét duyệt (đã duyệt / bị từ chối)
    fun findByStatusIn(statuses: Collection<ProductStatus>, pageable: Pageable): Page<Product>

    // Dùng bởi AuctionScheduler: sản phẩm đã duyệt (APPROVED) hoặc đang đấu giá (ACTIVE) mà thời gian đấu giá đã hết.
    // APPROVED cũng được đóng để không sót sản phẩm chưa kịp mở (ví dụ hệ thống tạm dừng qua giờ kết thúc).
    // Lấy theo lô, hết hạn sớm nhất trước.
    @Query(
        """
        SELECT p FROM Product p
        WHERE p.status IN ('APPROVED', 'ACTIVE')
        AND p.auctionEndAt <= :now
        ORDER BY p.auctionEndAt ASC
        """
    )
    fun findExpired(@Param("now") now: Instant, pageable: Pageable): List<Product>

    // Tự mở đấu giá: APPROVED -> ACTIVE khi đã đến giờ bắt đầu và chưa hết giờ. Một câu UPDATE cho cả lô,
    // idempotent, an toàn khi chạy nhiều instance. Trả về số sản phẩm vừa được mở.
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE Product p SET p.status = 'ACTIVE', p.updatedAt = :now
        WHERE p.status = 'APPROVED' AND p.auctionStartAt <= :now AND p.auctionEndAt > :now
        """
    )
    fun openStarted(@Param("now") now: Instant): Int

    // Đóng có điều kiện: chỉ đổi khi sản phẩm vẫn ở APPROVED/ACTIVE. Trả về 0 nếu instance khác đã đóng trước.
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        "UPDATE Product p SET p.status = :target, p.updatedAt = :now " +
            "WHERE p.id = :id AND p.status IN ('APPROVED', 'ACTIVE')"
    )
    fun closeIfOpen(
        @Param("id") id: UUID,
        @Param("target") target: ProductStatus,
        @Param("now") now: Instant
    ): Int
}
