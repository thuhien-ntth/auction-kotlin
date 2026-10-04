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

    
    
    @Query(
        """
        SELECT b.productId, COUNT(DISTINCT b.bidderId) FROM Bid b
        WHERE b.productId IN :productIds AND b.accepted = true
        GROUP BY b.productId
        """
    )
    fun countBiddersByProductIds(@Param("productIds") productIds: Collection<UUID>): List<Array<Any>>

    
    
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
