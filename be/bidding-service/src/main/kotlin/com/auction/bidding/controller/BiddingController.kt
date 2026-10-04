package com.auction.bidding.controller

import com.auction.bidding.dto.AuctionStateResponse
import com.auction.bidding.dto.BidResponse
import com.auction.bidding.dto.PlaceBidRequest
import com.auction.bidding.security.requireMember
import com.auction.bidding.service.BiddingService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class BiddingController(private val biddingService: BiddingService) {

    
    @PostMapping("/products/{id}/bids")
    fun placeBid(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @Valid @RequestBody body: PlaceBidRequest
    ): ResponseEntity<BidResponse> {
        val user = request.requireMember()
        return ResponseEntity.ok(biddingService.placeBid(id, user.id, body.amount))
    }

    
    @GetMapping("/products/{id}/bids")
    fun history(@PathVariable id: UUID, pageable: Pageable): Page<BidResponse> =
        biddingService.history(id, pageable)

    
    @GetMapping("/my/bids/participating")
    fun participating(request: HttpServletRequest, pageable: Pageable): Page<AuctionStateResponse> {
        val user = request.requireMember()
        return biddingService.participating(user.id, pageable)
    }

    
    @GetMapping("/my/bids/won")
    fun won(request: HttpServletRequest, pageable: Pageable): Page<AuctionStateResponse> {
        val user = request.requireMember()
        return biddingService.won(user.id, pageable)
    }
}
