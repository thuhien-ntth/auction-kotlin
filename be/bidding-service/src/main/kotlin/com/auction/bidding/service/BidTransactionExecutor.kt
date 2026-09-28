package com.auction.bidding.service

import com.auction.bidding.domain.AuctionState
import com.auction.bidding.domain.Bid
import com.auction.bidding.dto.BidResponse
import com.auction.bidding.repository.AuctionStateRepository
import com.auction.bidding.repository.BidRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Tách riêng thành 1 bean độc lập (thay vì 1 private method trong BiddingService) vì
 * @Transactional của Spring dựa trên proxy: gọi qua "this." trong cùng class (self-invocation)
 * sẽ KHÔNG đi qua proxy nên transaction sẽ không được áp dụng — lỗi kinh điển khi dùng
 * Spring AOP. BiddingService gọi sang bean này để đảm bảo mỗi lần thử đặt giá chạy trong
 * đúng 1 transaction riêng (cần thiết để optimistic lock retry hoạt động đúng).
 *
 * Phương thức KHÔNG ném ngoại lệ nghiệp vụ: bid bị từ chối được ghi lại (nếu bật
 * bidding.record-rejected-bids) rồi trả BidOutcome.Rejected, transaction commit bình thường.
 * Chỉ ObjectOptimisticLockingFailureException (xung đột version) lan ra ngoài để BiddingService retry.
 */
@Component
class BidTransactionExecutor(
    private val auctionStateRepository: AuctionStateRepository,
    private val bidRepository: BidRepository,
    @Value("\${bidding.record-rejected-bids:true}") private val recordRejectedBids: Boolean
) {
    @Transactional
    fun attempt(productId: UUID, bidderId: UUID, amount: BigDecimal): BidOutcome {
        val state = auctionStateRepository.findById(productId).orElse(null)
            ?: return BidOutcome.StateMissing

        val now = Instant.now()
        val rejection = rejectionOf(state, bidderId, amount, now)
        if (rejection != null) {
            if (recordRejectedBids) {
                bidRepository.save(Bid(productId = productId, bidderId = bidderId, amount = amount, accepted = false))
            }
            return BidOutcome.Rejected(rejection.first, rejection.second)
        }

        val bid = bidRepository.save(Bid(productId = productId, bidderId = bidderId, amount = amount, accepted = true))
        state.currentPrice = amount
        state.currentBidderId = bidderId
        state.updatedAt = now
        auctionStateRepository.save(state) // @Version -> ném ObjectOptimisticLockingFailureException nếu bị race
        return BidOutcome.Accepted(BidResponse.from(bid))
    }

    // Thứ tự kiểm tra quyết định thông điệp trả về khi vi phạm nhiều điều kiện cùng lúc.
    private fun rejectionOf(
        state: AuctionState,
        bidderId: UUID,
        amount: BigDecimal,
        now: Instant
    ): Pair<RejectReason, String>? = when {
        !now.isBefore(state.auctionEndAt) ->
            RejectReason.AUCTION_ENDED to "Phiên đấu giá đã kết thúc"
        bidderId == state.sellerId ->
            RejectReason.SELLER_CANNOT_BID to "Người bán không được đặt giá cho sản phẩm của chính mình"
        bidderId == state.currentBidderId ->
            RejectReason.ALREADY_HIGHEST_BIDDER to "Bạn đang là người trả giá cao nhất, hãy chờ người khác trả giá trước khi đặt tiếp"
        amount <= state.currentPrice ->
            RejectReason.AMOUNT_TOO_LOW to "Giá đặt phải cao hơn giá hiện tại (${state.currentPrice})"
        else -> null
    }
}
