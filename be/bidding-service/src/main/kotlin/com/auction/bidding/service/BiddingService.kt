package com.auction.bidding.service

import com.auction.bidding.client.CatalogClient
import com.auction.bidding.domain.AuctionState
import com.auction.bidding.dto.AuctionStateResponse
import com.auction.bidding.dto.BidResponse
import com.auction.bidding.repository.AuctionStateRepository
import com.auction.bidding.repository.BidRepository
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

/**
 * Trung tâm xử lý bài toán kiến trúc chính của đồ án: đặt giá đồng thời trên cùng 1
 * sản phẩm không được để "mất update" (2 bidder cùng thắng, hoặc giá bị ghi đè sai).
 * Chiến lược: optimistic locking (AuctionState.version, xử lý transaction thật sự trong
 * BidTransactionExecutor) + retry vòng lặp ngắn ở tầng này thay vì giữ khoá DB (pessimistic)
 * suốt thời gian xử lý — xem lý do & so sánh thực nghiệm ở báo cáo (mục 6).
 *
 * Cải tiến so với bản đầu:
 *  - Đường nóng chỉ đọc auction_state 1 lần (trong transaction); việc khởi tạo auction_state
 *    chỉ xảy ra khi executor báo StateMissing, không còn existsById cho mỗi bid.
 *  - Backoff lũy thừa có jitter, số lần thử cấu hình được; hết lượt thử trả 503 (không phải 500).
 *  - Lỗi khi gọi catalog-service được ánh xạ sang 404/503 thay vì 500 chung chung.
 *  - Micrometer counter cho mỗi kết quả và mỗi lần xung đột version.
 */
@Service
class BiddingService(
    private val auctionStateRepository: AuctionStateRepository,
    private val bidRepository: BidRepository,
    private val catalogClient: CatalogClient,
    private val transactionExecutor: BidTransactionExecutor,
    private val meterRegistry: MeterRegistry,
    @Value("\${bidding.max-optimistic-retry:8}") private val maxRetry: Int,
    @Value("\${bidding.retry-base-ms:10}") private val retryBaseMs: Long,
    @Value("\${bidding.retry-max-ms:200}") private val retryMaxMs: Long
) {
    fun placeBid(productId: UUID, bidderId: UUID, amount: BigDecimal): BidResponse {
        var conflicts = 0
        var stateInitialized = false
        while (true) {
            val outcome = try {
                transactionExecutor.attempt(productId, bidderId, amount)
            } catch (ex: ObjectOptimisticLockingFailureException) {
                conflicts++
                meterRegistry.counter("bidding.optimistic.conflicts").increment()
                if (conflicts >= maxRetry) {
                    countOutcome("retry_exhausted")
                    throw ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Phiên đấu giá đang có quá nhiều lượt đặt giá, vui lòng thử lại"
                    )
                }
                backoff(conflicts)
                continue
            }

            when (outcome) {
                is BidOutcome.Accepted -> {
                    countOutcome("accepted")
                    return outcome.bid
                }
                is BidOutcome.Rejected -> {
                    countOutcome(outcome.reason.name.lowercase())
                    throw ResponseStatusException(HttpStatus.CONFLICT, outcome.message)
                }
                BidOutcome.StateMissing -> {
                    if (stateInitialized) {
                        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm chưa có trong hệ thống đấu giá")
                    }
                    initializeAuctionState(productId)
                    stateInitialized = true
                }
            }
        }
    }

    private fun countOutcome(name: String) {
        meterRegistry.counter("bidding.bids", "outcome", name).increment()
    }

    // Backoff lũy thừa (base * 2^attempt, trần retryMaxMs) với jitter đầy đủ: ngủ ngẫu nhiên trong [1, trần].
    private fun backoff(attempt: Int) {
        val ceiling = minOf(retryMaxMs, retryBaseMs shl minOf(attempt, 10)).coerceAtLeast(1L)
        try {
            Thread.sleep(Random.nextLong(1, ceiling + 1))
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Yêu cầu bị gián đoạn, vui lòng thử lại")
        }
    }

    // Chỉ chạy ở lần bid đầu tiên của 1 sản phẩm.
    private fun initializeAuctionState(productId: UUID) {
        val fetched = try {
            catalogClient.getProduct(productId)
        } catch (ex: HttpClientErrorException.NotFound) {
            null
        } catch (ex: CallNotPermittedException) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ sản phẩm tạm thời không khả dụng, vui lòng thử lại")
        } catch (ex: RestClientException) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ sản phẩm tạm thời không khả dụng, vui lòng thử lại")
        }
        val product = fetched ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm")

        val auctionEndAt = product.auctionEndAt
        if (product.status != "ACTIVE" || auctionEndAt == null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Phiên đấu giá chưa mở hoặc đã đóng")
        }
        val startAt = product.auctionStartAt
        if (startAt != null && Instant.now().isBefore(startAt)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Phiên đấu giá chưa bắt đầu")
        }
        try {
            auctionStateRepository.saveAndFlush(
                AuctionState(
                    productId = product.id,
                    sellerId = product.sellerId,
                    currentPrice = product.startPrice,
                    auctionEndAt = auctionEndAt,
                    currency = product.currency,
                )
            )
        } catch (ex: DataAccessException) {
            // 2 request đầu tiên khởi tạo song song: bên thua đụng khoá trùng, đó là bình thường.
            // Chỉ nuốt lỗi khi hàng đã tồn tại; mọi lỗi khác (mất kết nối DB...) phải lan ra.
            if (!auctionStateRepository.existsById(productId)) throw ex
        }
    }

    fun history(productId: UUID, pageable: Pageable): Page<BidResponse> =
        bidRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable).map(BidResponse::from)

    // Participating (USMPS0020000): sản phẩm user đã đặt giá hợp lệ và phiên CHƯA kết thúc
    fun participating(bidderId: UUID, pageable: Pageable): Page<AuctionStateResponse> {
        val productIdsPage = bidRepository.findActiveParticipatingProductIds(bidderId, Instant.now(), pageable)
        val states = auctionStateRepository.findAllById(productIdsPage.content).associateBy { it.productId }
        return withProductInfo(productIdsPage.map { pid -> AuctionStateResponse.from(states.getValue(pid)) })
    }

    // Won list (USMPS0050000)
    fun won(bidderId: UUID, pageable: Pageable): Page<AuctionStateResponse> =
        withProductInfo(
            auctionStateRepository.findByCurrentBidderIdAndAuctionEndAtBefore(bidderId, Instant.now(), pageable)
                .map(AuctionStateResponse::from)
        )

    // Gắn tên + ảnh sản phẩm cho cả trang bằng 1 lời gọi batch sang catalog-service (không N+1)
    private fun withProductInfo(page: Page<AuctionStateResponse>): Page<AuctionStateResponse> {
        val briefs = catalogClient.getProductBriefs(page.content.map { it.productId })
        return page.map { s -> briefs[s.productId]?.let { s.copy(title = it.title, imageUrl = it.imageUrl) } ?: s }
    }
}
