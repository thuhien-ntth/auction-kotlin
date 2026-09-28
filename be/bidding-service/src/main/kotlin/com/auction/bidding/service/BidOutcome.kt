package com.auction.bidding.service

import com.auction.bidding.dto.BidResponse

/**
 * Kết quả của MỘT lần thử đặt giá trong 1 transaction (BidTransactionExecutor.attempt).
 *
 * Trước đây các trường hợp bị từ chối được biểu diễn bằng cách ném ResponseStatusException
 * ngay trong transaction — Spring rollback mặc định với RuntimeException nên bản ghi
 * Bid(accepted=false) vừa INSERT bị hủy theo, lịch sử/audit mất các bid bị từ chối.
 * Trả kết quả thay vì ném ngoại lệ để transaction commit bình thường; việc đổi kết quả
 * thành mã HTTP (409...) diễn ra ở BiddingService, NGOÀI transaction.
 */
sealed interface BidOutcome {
    data class Accepted(val bid: BidResponse) : BidOutcome
    data class Rejected(val reason: RejectReason, val message: String) : BidOutcome

    /** Chưa có auction_state cho sản phẩm này — BiddingService sẽ khởi tạo rồi thử lại. */
    object StateMissing : BidOutcome
}

enum class RejectReason {
    AUCTION_ENDED,
    SELLER_CANNOT_BID,
    ALREADY_HIGHEST_BIDDER,
    AMOUNT_TOO_LOW
}
