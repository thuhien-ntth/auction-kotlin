package com.auction.bidding.repository

import com.auction.bidding.domain.Bid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface BidRepository : JpaRepository<Bid, UUID> {

    fun findByProductIdOrderByCreatedAtDesc(productId: UUID, pageable: Pageable): Page<Bid>

    // Số người tham gia (bidder khác nhau có bid hợp lệ) của một lô sản phẩm — 1 query GROUP BY.
    // Mỗi phần tử: [productId: UUID, count: Long]. Sản phẩm chưa có bid sẽ không có trong kết quả.
    @Query(
        """
        SELECT b.productId, COUNT(DISTINCT b.bidderId) FROM Bid b
        WHERE b.productId IN :productIds AND b.accepted = true
        GROUP BY b.productId
        """
    )
    fun countBiddersByProductIds(@Param("productIds") productIds: Collection<UUID>): List<Array<Any>>

    // Participating list (USMPS0020000): các sản phẩm khác nhau user đã từng đặt giá hợp lệ
    // VÀ phiên đấu giá chưa kết thúc (sản phẩm đã kết thúc thuộc "Won" hoặc lịch sử, không phải "đang tham gia").
    @Query(
        value = """
        SELECT DISTINCT b.productId FROM Bid b, AuctionState s
        WHERE s.productId = b.productId
          AND b.bidderId = :bidderId
          AND b.accepted = true
          AND s.auctionEndAt > :now
        """,
        countQuery = """
        SELECT COUNT(DISTINCT b.productId) FROM Bid b, AuctionState s
        WHERE s.productId = b.productId
          AND b.bidderId = :bidderId
          AND b.accepted = true
          AND s.auctionEndAt > :now
        """
    )
    fun findActiveParticipatingProductIds(
        @Param("bidderId") bidderId: UUID,
        @Param("now") now: Instant,
        pageable: Pageable
    ): Page<UUID>
}
