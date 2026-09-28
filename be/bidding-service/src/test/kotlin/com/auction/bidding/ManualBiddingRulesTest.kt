package com.auction.bidding

import com.auction.bidding.client.CatalogClient
import com.auction.bidding.dto.ApiError
import com.auction.bidding.dto.BidResponse
import com.auction.bidding.dto.PlaceBidRequest
import com.auction.bidding.support.BiddingIntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * NHÓM B — Các quy tắc từ chối bid. Kiểm tra cả mã HTTP (409) lẫn việc bid bị từ chối VẪN được
 * ghi vào bảng bids (accepted=false) — trước đây bản ghi này bị rollback cùng transaction.
 */
class ManualBiddingRulesTest : BiddingIntegrationTestBase() {

    private val startPrice = BigDecimal("1000000")

    private fun mockProduct(
        productId: UUID,
        sellerId: UUID,
        status: String = "ACTIVE",
        endAt: Instant? = Instant.now().plusSeconds(3600)
    ) {
        given(catalogClient.getProduct(productId)).willReturn(
            CatalogClient.CatalogProductInfo(productId, sellerId, startPrice, status, endAt)
        )
    }

    private fun bid(productId: UUID, bidderId: UUID, amount: String): ResponseEntity<ApiError> {
        val headers = authHeaders(bidderId).apply { contentType = MediaType.APPLICATION_JSON }
        return restTemplate.exchange(
            "/products/{id}/bids", HttpMethod.POST,
            HttpEntity(PlaceBidRequest(BigDecimal(amount)), headers),
            ApiError::class.java, productId
        )
    }

    private fun bidOk(productId: UUID, bidderId: UUID, amount: String): ResponseEntity<BidResponse> {
        val headers = authHeaders(bidderId).apply { contentType = MediaType.APPLICATION_JSON }
        return restTemplate.exchange(
            "/products/{id}/bids", HttpMethod.POST,
            HttpEntity(PlaceBidRequest(BigDecimal(amount)), headers),
            BidResponse::class.java, productId
        )
    }

    @Test
    fun `B1 gia khong cao hon gia hien tai bi 409 va van duoc ghi lai voi accepted false`() {
        val productId = UUID.randomUUID(); val seller = UUID.randomUUID(); val bidder = UUID.randomUUID()
        mockProduct(productId, seller)

        val response = bid(productId, bidder, "1000000") // bằng giá khởi điểm -> không đủ cao

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body!!.message).contains("higher than the current price")
        val bids = bidRepository.findAll().filter { it.productId == productId }
        assertThat(bids).hasSize(1)
        assertThat(bids[0].accepted).isFalse()
        assertThat(auctionStateRepository.findById(productId).orElseThrow().currentPrice)
            .isEqualByComparingTo(startPrice)
    }

    @Test
    fun `B2 seller khong duoc tu dat gia san pham cua minh`() {
        val productId = UUID.randomUUID(); val seller = UUID.randomUUID()
        mockProduct(productId, seller)

        val response = bid(productId, seller, "1200000")

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body!!.message).contains("Người bán không được đặt giá")
    }

    @Test
    fun `B3 nguoi dang giu gia cao nhat khong duoc dat lien tiep`() {
        val productId = UUID.randomUUID(); val seller = UUID.randomUUID(); val bidder = UUID.randomUUID()
        mockProduct(productId, seller)

        assertThat(bidOk(productId, bidder, "1200000").statusCode).isEqualTo(HttpStatus.OK)
        val second = bid(productId, bidder, "1300000")

        assertThat(second.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(second.body!!.message).contains("already the highest bidder")
        assertThat(auctionStateRepository.findById(productId).orElseThrow().currentPrice)
            .isEqualByComparingTo(BigDecimal("1200000"))
    }

    @Test
    fun `B4 sau khi bi vuot gia nguoi cu duoc dat lai`() {
        val productId = UUID.randomUUID(); val seller = UUID.randomUUID()
        val a = UUID.randomUUID(); val b = UUID.randomUUID()
        mockProduct(productId, seller)

        assertThat(bidOk(productId, a, "1200000").statusCode).isEqualTo(HttpStatus.OK)
        assertThat(bidOk(productId, b, "1300000").statusCode).isEqualTo(HttpStatus.OK)
        assertThat(bidOk(productId, a, "1400000").statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `B5 phien da het gio bi 409`() {
        val productId = UUID.randomUUID(); val seller = UUID.randomUUID(); val bidder = UUID.randomUUID()
        mockProduct(productId, seller, endAt = Instant.now().minusSeconds(60))

        val response = bid(productId, bidder, "1200000")

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body!!.message).contains("has ended")
    }

    @Test
    fun `B6 san pham chua ACTIVE thi khong tao auction state`() {
        val productId = UUID.randomUUID(); val seller = UUID.randomUUID(); val bidder = UUID.randomUUID()
        mockProduct(productId, seller, status = "APPROVED", endAt = null)

        val response = bid(productId, bidder, "1200000")

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(auctionStateRepository.existsById(productId)).isFalse()
    }
}
