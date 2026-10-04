package com.auction.bidding.service

import com.auction.bidding.dto.BidResponse


sealed interface BidOutcome {
    data class Accepted(val bid: BidResponse) : BidOutcome
    data class Rejected(val reason: RejectReason, val message: String) : BidOutcome

    
    object StateMissing : BidOutcome
}

enum class RejectReason {
    AUCTION_ENDED,
    SELLER_CANNOT_BID,
    ALREADY_HIGHEST_BIDDER,
    AMOUNT_TOO_LOW
}
