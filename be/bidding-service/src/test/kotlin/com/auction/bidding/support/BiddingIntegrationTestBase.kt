package com.auction.bidding.support

import com.auction.bidding.client.CatalogClient
import com.auction.bidding.repository.AuctionStateRepository
import com.auction.bidding.repository.BidRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpHeaders
import java.util.UUID


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BiddingIntegrationTestBase {

    @Autowired
    protected lateinit var restTemplate: TestRestTemplate

    @Autowired
    protected lateinit var auctionStateRepository: AuctionStateRepository

    @Autowired
    protected lateinit var bidRepository: BidRepository

    @MockBean
    protected lateinit var catalogClient: CatalogClient

    
    
    
    @BeforeEach
    fun cleanDatabase() {
        bidRepository.deleteAll()
        auctionStateRepository.deleteAll()
    }

    protected fun authHeaders(userId: UUID, isAdmin: Boolean = false): HttpHeaders =
        HttpHeaders().apply {
            set(HttpHeaders.AUTHORIZATION, "Bearer ${JwtTestSupport.tokenFor(userId, isAdmin)}")
        }
}
