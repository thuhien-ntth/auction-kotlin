package com.auction.catalog.job

import com.auction.catalog.client.BiddingClient
import com.auction.catalog.domain.ProductStatus
import com.auction.catalog.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant


@Component
class AuctionScheduler(
    private val productRepository: ProductRepository,
    private val biddingClient: BiddingClient,
    @Value("\${auction-closing.batch-size:100}") private val batchSize: Int
) {
    private val log = LoggerFactory.getLogger(AuctionScheduler::class.java)

    @Scheduled(fixedDelayString = "\${auction-closing.interval-ms:5000}")
    fun tick() {
        openStartedAuctions()
        closeEndedAuctions()
    }

    fun openStartedAuctions() {
        val opened = productRepository.openStarted(Instant.now())
        if (opened > 0) log.info("Mở đấu giá cho {} sản phẩm (đến giờ bắt đầu)", opened)
    }

    fun closeEndedAuctions() {
        var round = 0
        while (round++ < MAX_ROUNDS_PER_RUN) {
            val batch = productRepository.findExpired(Instant.now(), PageRequest.of(0, batchSize))
            if (batch.isEmpty()) return

            val ids = batch.mapNotNull { it.id }
            
            val results = biddingClient.getAuctionResults(ids)
            if (results == null) {
                log.warn("Bỏ qua lượt đóng phiên: không lấy được kết quả từ bidding-service ({} sản phẩm)", ids.size)
                return
            }

            var closed = 0
            for (id in ids) {
                val winnerId = results[id]?.winnerId
                val target = if (winnerId != null) ProductStatus.SOLD else ProductStatus.ENDED_NO_BID
                val updated = productRepository.closeIfOpen(id, target, Instant.now())
                if (updated > 0) {
                    closed += updated
                    log.info("Đóng phiên đấu giá product={} -> {} (winner={})", id, target, winnerId)
                }
            }
            
            if (batch.size < batchSize || closed == 0) return
        }
    }

    companion object {
        private const val MAX_ROUNDS_PER_RUN = 20
    }
}
