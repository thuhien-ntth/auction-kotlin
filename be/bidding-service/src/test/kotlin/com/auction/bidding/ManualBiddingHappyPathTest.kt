package com.auction.bidding

import com.auction.bidding.client.CatalogClient
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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID


class ManualBiddingHappyPathTest : BiddingIntegrationTestBase() {

    private fun jsonHeaders(userId: UUID) =
        authHeaders(userId).apply { contentType = MediaType.APPLICATION_JSON }

    private fun mockActiveProduct(productId: UUID, sellerId: UUID, startPrice: BigDecimal) {
        given(catalogClient.getProduct(productId)).willReturn(
            CatalogClient.CatalogProductInfo(
                id = productId,
                sellerId = sellerId,
                startPrice = startPrice,
                status = "ACTIVE",
                auctionEndAt = Instant.now().plusSeconds(3600)
            )
        )
    }

    
    @Test
    fun `A1 bid dau tien hop le duoc chap nhan va tu tao AuctionState`() {
        val productId = UUID.randomUUID()
        val sellerId = UUID.randomUUID()
        val bidderId = UUID.randomUUID()
        val startPrice = BigDecimal("1000000")
        val bidAmount = BigDecimal("1200000")
        mockActiveProduct(productId, sellerId, startPrice)

        val response = restTemplate.exchange(
            "/products/{id}/bids",
            HttpMethod.POST,
            HttpEntity(PlaceBidRequest(bidAmount), jsonHeaders(bidderId)),
            BidResponse::class.java,
            productId
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body
        assertThat(body).isNotNull
        assertThat(body!!.accepted).isTrue()
        assertThat(body.amount).isEqualByComparingTo(bidAmount)
        assertThat(body.bidderId).isEqualTo(bidderId)
        assertThat(body.productId).isEqualTo(productId)

        val state = auctionStateRepository.findById(productId).orElseThrow()
        assertThat(state.currentPrice).isEqualByComparingTo(bidAmount)
        assertThat(state.currentBidderId).isEqualTo(bidderId)
        assertThat(state.sellerId).isEqualTo(sellerId)
    }

    
    @Test
    fun `A2 bid thu 2 gia cao hon tu bidder khac cap nhat dung nguoi moi`() {
        val productId = UUID.randomUUID()
        val sellerId = UUID.randomUUID()
        val bidder1 = UUID.randomUUID()
        val bidder2 = UUID.randomUUID()
        val startPrice = BigDecimal("1000000")
        mockActiveProduct(productId, sellerId, startPrice)

        val firstAmount = BigDecimal("1200000")
        val r1 = restTemplate.exchange(
            "/products/{id}/bids", HttpMethod.POST,
            HttpEntity(PlaceBidRequest(firstAmount), jsonHeaders(bidder1)),
            BidResponse::class.java, productId
        )
        assertThat(r1.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(r1.body?.accepted).isTrue()

        val secondAmount = BigDecimal("1500000")
        val r2 = restTemplate.exchange(
            "/products/{id}/bids", HttpMethod.POST,
            HttpEntity(PlaceBidRequest(secondAmount), jsonHeaders(bidder2)),
            BidResponse::class.java, productId
        )
        assertThat(r2.statusCode).isEqualTo(HttpStatus.OK)
        val body2 = r2.body
        assertThat(body2).isNotNull
        assertThat(body2!!.accepted).isTrue()
        assertThat(body2.bidderId).isEqualTo(bidder2)

        val state = auctionStateRepository.findById(productId).orElseThrow()
        assertThat(state.currentPrice).isEqualByComparingTo(secondAmount)
        assertThat(state.currentBidderId).isEqualTo(bidder2)
    }
}
