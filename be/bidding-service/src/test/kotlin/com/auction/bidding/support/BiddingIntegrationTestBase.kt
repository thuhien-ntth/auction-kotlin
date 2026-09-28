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

/**
 * Base class dùng chung cho mọi test case Manual Bidding (nhóm A..F trong kịch bản test đã
 * gửi user duyệt). Bật full Spring context + embedded server thật (RANDOM_PORT) để test đi
 * qua đúng chuỗi thật: JwtAuthFilter -> BiddingController -> BiddingService ->
 * BidTransactionExecutor (transaction thật) -> JPA/H2 — không mock tầng service/repository,
 * CHỈ mock CatalogClient (phụ thuộc cross-service ra ngoài, không phải thứ đang test).
 *
 * DB: H2 in-memory (xem src/test/resources/application.yml), schema tự sinh từ entity
 * (ddl-auto=create-drop) — không cần Postgres/Docker thật để chạy bộ test này.
 */
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

    // Mỗi test chạy trên DB sạch — tránh 1 test bị ảnh hưởng bởi dữ liệu test chạy trước
    // (các test dùng UUID.randomUUID() cho productId nên về lý thuyết đã tách biệt, nhưng
    // dọn DB tường minh vẫn an toàn hơn và giúp assertion đơn giản, dễ đọc hơn).
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
