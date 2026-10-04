package com.auction.bidding.repository

import com.auction.bidding.domain.AuctionState
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface AuctionStateRepository : JpaRepository<AuctionState, UUID> {

    
    fun findByCurrentBidderIdAndAuctionEndAtBefore(bidderId: UUID, now: Instant, pageable: Pageable): Page<AuctionState>
}
