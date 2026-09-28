package com.auction.catalog

import com.auction.catalog.client.BiddingClient
import com.auction.catalog.domain.Product
import com.auction.catalog.domain.ProductStatus
import com.auction.catalog.job.AuctionScheduler
import com.auction.catalog.repository.ProductRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Kiểm tra logic tự mở/đóng phiên: gọi bidding-service 1 lần cho cả lô, SOLD/ENDED_NO_BID đúng,
 * và không đụng vào dữ liệu khi bidding-service không phản hồi.
 *
 * Mockito matcher trả về null còn tham số Kotlin là non-null, nên dùng các hàm bọc trả về giá trị thật.
 */
class AuctionSchedulerTest {

    private val repo = mock(ProductRepository::class.java)
    private val client = mock(BiddingClient::class.java)
    private val job = AuctionScheduler(repo, client, 100)

    private fun anyInstant(): Instant { ArgumentMatchers.any(Instant::class.java); return Instant.EPOCH }
    private fun anyPageable(): Pageable { ArgumentMatchers.any(Pageable::class.java); return PageRequest.of(0, 1) }
    private fun anyUuid(): UUID { ArgumentMatchers.any(UUID::class.java); return UUID(0, 0) }
    private fun anyStatus(): ProductStatus { ArgumentMatchers.any(ProductStatus::class.java); return ProductStatus.ACTIVE }
    private fun <T> eqK(v: T): T { ArgumentMatchers.eq(v); return v }

    private fun product(id: UUID) = Product(
        id = id, sellerId = UUID.randomUUID(), title = "p", startPrice = BigDecimal("100"),
        auctionStartAt = Instant.now().minusSeconds(120), auctionEndAt = Instant.now().minusSeconds(1),
        status = ProductStatus.ACTIVE
    )

    @Test
    fun `closes sold and no-bid products with one batched call`() {
        val sold = UUID.randomUUID()
        val noBid = UUID.randomUUID()
        `when`(repo.findExpired(anyInstant(), anyPageable()))
            .thenReturn(listOf(product(sold), product(noBid)))
        `when`(client.getAuctionResults(listOf(sold, noBid))).thenReturn(
            mapOf(
                sold to BiddingClient.AuctionResult(UUID.randomUUID(), BigDecimal("500")),
                noBid to BiddingClient.AuctionResult(null, null)
            )
        )
        `when`(repo.closeIfOpen(anyUuid(), anyStatus(), anyInstant())).thenReturn(1)

        job.closeEndedAuctions()

        verify(client).getAuctionResults(listOf(sold, noBid))
        verify(repo).closeIfOpen(eqK(sold), eqK(ProductStatus.SOLD), anyInstant())
        verify(repo).closeIfOpen(eqK(noBid), eqK(ProductStatus.ENDED_NO_BID), anyInstant())
    }

    @Test
    fun `does nothing when bidding-service is unavailable`() {
        val id = UUID.randomUUID()
        `when`(repo.findExpired(anyInstant(), anyPageable())).thenReturn(listOf(product(id)))
        `when`(client.getAuctionResults(listOf(id))).thenReturn(null)

        job.closeEndedAuctions()

        verify(repo, never()).closeIfOpen(anyUuid(), anyStatus(), anyInstant())
    }

    @Test
    fun `opens started auctions with a single update`() {
        `when`(repo.openStarted(anyInstant())).thenReturn(3)

        job.openStartedAuctions()

        verify(repo).openStarted(anyInstant())
        verify(client, never()).getAuctionResults(ArgumentMatchers.anyList<UUID>() ?: emptyList())
    }

    @Test
    fun `no ended products means no remote call`() {
        `when`(repo.findExpired(anyInstant(), anyPageable())).thenReturn(emptyList())

        job.closeEndedAuctions()

        verify(client, never()).getAuctionResults(ArgumentMatchers.anyList<UUID>() ?: emptyList())
    }
}
