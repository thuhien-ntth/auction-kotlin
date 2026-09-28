package com.auction.app.network

import com.auction.app.data.TokenStore
import com.auction.app.network.model.*
import retrofit2.Response
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Mock API phá»¥c vá»¥ cháº¡y app Android Ä‘á»™c láº­p khÃ´ng cáº§n Docker hay backend.
 * ToÃ n bá»™ dá»¯ liá»‡u Ä‘Æ°á»£c quáº£n lÃ½ in-memory, cho phÃ©p thao tÃ¡c Ä‘áº·t giÃ¡, Ä‘Äƒng sáº£n pháº©m
 * vÃ  cáº­p nháº­t giao diá»‡n mÆ°á»£t mÃ .
 */
class MockAuctionApi(private val tokenStore: TokenStore) : AuctionApi {

    private val bidMutex = Mutex()
    private val products = mutableListOf(
        ProductItem(
            id = "prod-1",
            sellerId = "user-seller-01",
            title = "MÃ¡y áº£nh Leica M11 Rangefinder Body Black",
            description = "HÃ ng chÃ­nh hÃ£ng fullbox 99%, cáº£m biáº¿n BSI CMOS 60MP, kÃ¨m 2 pin dá»± phÃ²ng vÃ  bao da Artisan & Artist thá»§ cÃ´ng.",
            category = "ELECTRONICS",
            startPrice = BigDecimal("185000000"),
            currentPrice = BigDecimal("192000000"),
            status = "ACTIVE",
            rejectionReason = null,
            createdAt = "2026-09-01T08:00:00Z"
        ),
        ProductItem(
            id = "prod-2",
            sellerId = "user-seller-02",
            title = "Äá»“ng há»“ Thá»¥y SÄ© Rolex Submariner Date 126610LN",
            description = "Má»›i 100% nguyÃªn seal tháº» báº£o hÃ nh toÃ n cáº§u 2026. ThÃ©p Oystersteel 904L, vÃ nh gá»‘m Cerachrom Ä‘en, trá»¯ cÃ³t 70 giá».",
            category = "WATCHES",
            startPrice = BigDecimal("280000000"),
            currentPrice = BigDecimal("305000000"),
            status = "ACTIVE",
            rejectionReason = null,
            createdAt = "2026-09-02T10:15:00Z"
        ),
        ProductItem(
            id = "prod-3",
            sellerId = "user-bidder-01", // Cho bidder hiá»‡n táº¡i sá»Ÿ há»¯u Ä‘á»ƒ hiá»ƒn thá»‹ á»Ÿ "Sáº£n pháº©m cá»§a tÃ´i"
            title = "Tranh sÆ¡n dáº§u 'Phá»‘ Cá»• HÃ  Ná»™i' - Báº£n váº½ 1985",
            description = "Tranh sÆ¡n dáº§u trÃªn toan cÃ³ chá»©ng nháº­n tháº©m Ä‘á»‹nh nghá»‡ thuáº­t. KÃ­ch thÆ°á»›c 70x90cm kÃ¨m khung gá»— gá»¥ cá»•.",
            category = "ART",
            startPrice = BigDecimal("85000000"),
            currentPrice = BigDecimal("92000000"),
            status = "ACTIVE",
            rejectionReason = null,
            createdAt = "2026-09-03T14:30:00Z"
        ),
        ProductItem(
            id = "prod-4",
            sellerId = "user-seller-01",
            title = "Xe mÃ¡y Vespa Sprint 150cc 1968 Cá»• Äiá»ƒn",
            description = "MÃ¡y mÃ³c zin nguyÃªn báº£n ná»• thÃ¬ tháº§m, mÃ u sÆ¡n lam cá»• Ä‘iá»ƒn phá»¥c cháº¿ chuáº©n xÃ¡c tá»«ng con á»‘c, biá»ƒn sá»‘ Ä‘áº¹p 4 sá»‘.",
            category = "VEHICLES",
            startPrice = BigDecimal("68000000"),
            currentPrice = BigDecimal("75000000"),
            status = "ACTIVE",
            rejectionReason = null,
            createdAt = "2026-09-04T09:00:00Z"
        ),
        ProductItem(
            id = "prod-5",
            sellerId = "user-bidder-01",
            title = "Bá»™ bÃ n gháº¿ Tráº¯c Cáº©m 6 mÃ³n thá»i Nguyá»…n",
            description = "Gá»— tráº¯c tá»± nhiÃªn tuyá»ƒn chá»n, Ä‘á»¥c tay tinh xáº£o, vÃ¢n gá»— cuá»“n cuá»™n. Äang chá» duyá»‡t kiá»ƒm Ä‘á»‹nh.",
            category = "ANTIQUES",
            startPrice = BigDecimal("120000000"),
            currentPrice = BigDecimal("120000000"),
            status = "PENDING_APPROVAL",
            rejectionReason = null,
            createdAt = "2026-09-08T11:00:00Z"
        ),
        ProductItem(
            id = "prod-6",
            sellerId = "user-bidder-01",
            title = "Laptop Apple MacBook Pro 16 inch M3 Max",
            description = "Báº£n 64GB RAM 1TB SSD mÃ u Space Black, sáº¡c 5 láº§n, bÃ n phÃ­m US.",
            category = "ELECTRONICS",
            startPrice = BigDecimal("72000000"),
            currentPrice = BigDecimal("72000000"),
            status = "REJECTED",
            rejectionReason = "HÃ¬nh áº£nh sáº£n pháº©m bá»‹ má», vui lÃ²ng chá»¥p láº¡i rÃµ sá»‘ Serial Number trÃªn thÃ¢n mÃ¡y.",
            createdAt = "2026-09-05T16:20:00Z"
        )
    )

    // Lá»‹ch sá»­ Ä‘áº·t giÃ¡ tá»«ng sáº£n pháº©m
    private val bidHistoryMap = mutableMapOf<String, MutableList<BidResponse>>(
        "prod-1" to mutableListOf(
            BidResponse("b1-3", "prod-1", "user-bidder-01", BigDecimal("192000000"), true, "2026-09-12T15:30:00Z"),
            BidResponse("b1-2", "prod-1", "user-bidder-02", BigDecimal("188000000"), true, "2026-09-12T14:10:00Z"),
            BidResponse("b1-1", "prod-1", "user-bidder-03", BigDecimal("185000000"), true, "2026-09-12T11:00:00Z")
        ),
        "prod-2" to mutableListOf(
            BidResponse("b2-2", "prod-2", "user-bidder-02", BigDecimal("305000000"), true, "2026-09-12T16:00:00Z"),
            BidResponse("b2-1", "prod-2", "user-bidder-01", BigDecimal("290000000"), true, "2026-09-12T13:20:00Z")
        ),
        "prod-3" to mutableListOf(
            BidResponse("b3-1", "prod-3", "user-bidder-03", BigDecimal("92000000"), true, "2026-09-11T10:00:00Z")
        ),
        "prod-4" to mutableListOf(
            BidResponse("b4-1", "prod-4", "user-bidder-02", BigDecimal("75000000"), true, "2026-09-10T09:30:00Z")
        )
    )

    private var currentUserId = "user-bidder-01"

    override suspend fun login(request: LoginRequest): LoginResponse {
        val email = request.email.lowercase().trim()
        val (userId, fullName, isAdmin) = when {
            email.contains("admin") -> Triple("user-admin-01", "Quáº£n trá»‹ viÃªn (Admin)", true)
            email.contains("seller") -> Triple("user-seller-01", "NgÆ°á»i bÃ¡n (Seller 1)", false)
            else -> Triple("user-bidder-01", "NgÆ°á»i Ä‘áº¥u giÃ¡ (Bidder 1)", false)
        }
        currentUserId = userId
        return LoginResponse(
            accessToken = "mock-jwt-token-${UUID.randomUUID()}",
            userId = userId,
            fullName = fullName,
            isAdmin = isAdmin
        )
    }

    override suspend fun register(request: RegisterRequest): Response<Unit> {
        println("MOCK: User registered with email ${request.email}. Verification code sent to email.")
        return Response.success(Unit)
    }

    override suspend fun verifyEmail(request: VerifyRequest): Response<Unit> {
        println("MOCK: User verified with email ${request.email} and token ${request.token}.")
        return Response.success(Unit)
    }

    override suspend fun logout(): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun searchProducts(
        keyword: String?,
        category: String?,
        page: Int?,
        size: Int?
    ): PageResponse<ProductSummary> {
        val filtered = products.filter { p ->
            val matchStatus = p.status == "ACTIVE"
            val matchKeyword = keyword.isNullOrBlank() || p.title.contains(keyword, ignoreCase = true) || p.description.contains(keyword, ignoreCase = true)
            val matchCat = category.isNullOrBlank() || p.category.equals(category, ignoreCase = true)
            matchStatus && matchKeyword && matchCat
        }.map { it.toSummary() }

        // Mock không cần phân trang thật — trả nguyên danh sách đã lọc ở "trang" được yêu cầu.
        val pageSize = size ?: filtered.size.coerceAtLeast(1)
        val pageNumber = page ?: 0
        val fromIndex = (pageNumber * pageSize).coerceIn(0, filtered.size)
        val toIndex = (fromIndex + pageSize).coerceIn(fromIndex, filtered.size)
        val pageContent = filtered.subList(fromIndex, toIndex)
        val totalPages = if (pageSize == 0) 1 else ((filtered.size + pageSize - 1) / pageSize).coerceAtLeast(1)

        return PageResponse(
            content = pageContent,
            totalElements = filtered.size.toLong(),
            totalPages = totalPages,
            number = pageNumber
        )
    }

    override suspend fun getProduct(id: String): ProductDetail {
        val item = products.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y sáº£n pháº©m vá»›i mÃ£: $id")
        return item.toDetail()
    }

        override suspend fun updateProduct(id: String, request: CreateProductRequest): ProductDetail {
        val detail = getProduct(id)
        return detail.copy(
            title = request.title,
            description = request.description,
            category = request.category,
            startPrice = request.startPrice
        )
    }
    override suspend fun registerProduct(request: CreateProductRequest): ProductDetail {
        val newItem = ProductItem(
            id = "prod-${System.currentTimeMillis().toString().takeLast(6)}",
            sellerId = currentUserId,
            title = request.title,
            description = request.description,
            category = request.category,
            startPrice = request.startPrice,
            currentPrice = request.startPrice,
            status = "PENDING_APPROVAL",
            rejectionReason = null,
            createdAt = "2026-09-13T08:00:00Z"
        )
        products.add(0, newItem)
        return newItem.toDetail()
    }

    override suspend fun uploadProductImage(
        productId: String,
        file: okhttp3.MultipartBody.Part
    ): ProductDetail {
        val item = products.firstOrNull { it.id == productId }
            ?: throw IllegalArgumentException("Không tìm thấy sản phẩm với mã: $productId")
        // Mock không lưu file thật — chỉ đánh dấu là đã có ảnh để UI cập nhật.
        item.imageUrl = "products/$productId/image"
        return item.toDetail()
    }

    override suspend fun myProducts(status: String?): PageResponse<ProductSummary> {
        val userProducts = products.filter { p ->
            (p.sellerId == currentUserId || p.sellerId == "user-bidder-01") &&
                    (status.isNullOrBlank() || p.status.equals(status, ignoreCase = true))
        }.map { it.toSummary() }

        return PageResponse(
            content = userProducts,
            totalElements = userProducts.size.toLong(),
            totalPages = 1,
            number = 0
        )
    }

    override suspend fun placeBid(productId: String, request: PlaceBidRequest): BidResponse {
        // Ensure sequential processing to respect firstâ€‘comeâ€‘firstâ€‘serve order
        return bidMutex.withLock {
            val item = products.firstOrNull { it.id == productId }
                ?: throw IllegalArgumentException("Sáº£n pháº©m khÃ´ng tá»“n táº¡i: $productId")

            // Kiá»ƒm tra thá»i gian Ä‘áº¥u giÃ¡ náº¿u cÃ³ (start/end)
            val now = java.time.Instant.now()
            item.auctionStartAt?.let { start ->
                if (now.isBefore(java.time.Instant.parse(start))) {
                    throw IllegalStateException("Äáº¥u giÃ¡ chÆ°a báº¯t Ä‘áº§u")
                }
            }
            item.auctionEndAt?.let { end ->
                if (now.isAfter(java.time.Instant.parse(end))) {
                    throw IllegalStateException("Äáº¥u giÃ¡ Ä‘Ã£ káº¿t thÃºc")
                }
            }

            val historyList = bidHistoryMap[productId] ?: emptyList()
            val isFirstBid = historyList.none { it.accepted }
            val lastAcceptedBidder = historyList.firstOrNull { it.accepted }?.bidderId

            // Rule 3.3.1: Không được bid 2 lần liên tiếp nếu đang giữ giá cao nhất
            if (lastAcceptedBidder == currentUserId) {
                throw IllegalStateException("Bạn đang là người giữ giá cao nhất, không thể đặt giá liên tiếp.")
            }

            // Rule 3.3.3: Kiểm tra mức giá hợp lệ
            // Lần ra giá đầu: >= startPrice. Lần tiếp theo: > currentPrice
            val isAccepted = if (isFirstBid) {
                request.amount >= item.startPrice
            } else {
                request.amount > item.currentPrice
            }
            val bid = BidResponse(
                id = "bid-${UUID.randomUUID().toString().take(8)}",
                productId = productId,
                bidderId = currentUserId,
                amount = request.amount,
                accepted = isAccepted,
                createdAt = now.toString()
            )

            // LÆ°u lá»‹ch sá»­
            val history = bidHistoryMap.getOrPut(productId) { mutableListOf() }
            history.add(0, bid)

            if (isAccepted) {
                item.currentPrice = request.amount
            }
            bid
        }
    }

    override suspend fun bidHistory(productId: String): PageResponse<BidResponse> {
        val history = bidHistoryMap[productId] ?: emptyList()
        return PageResponse(
            content = history,
            totalElements = history.size.toLong(),
            totalPages = 1,
            number = 0
        )
    }

    override suspend fun participating(): PageResponse<AuctionStateResponse> {
        val states = listOf(
            AuctionStateResponse(
                productId = "prod-1",
                currentPrice = products.first { it.id == "prod-1" }.currentPrice,
                currentBidderId = "user-bidder-01",
                auctionEndAt = "2026-09-25 21:00"
            ),
            AuctionStateResponse(
                productId = "prod-2",
                currentPrice = products.first { it.id == "prod-2" }.currentPrice,
                currentBidderId = "user-bidder-02",
                auctionEndAt = "2026-09-26 20:00"
            ),
            AuctionStateResponse(
                productId = "prod-4",
                currentPrice = products.first { it.id == "prod-4" }.currentPrice,
                currentBidderId = "user-bidder-02",
                auctionEndAt = "2026-09-28 18:00"
            )
        )
        return PageResponse(
            content = states,
            totalElements = states.size.toLong(),
            totalPages = 1,
            number = 0
        )
    }

    override suspend fun won(): PageResponse<AuctionStateResponse> {
        kotlinx.coroutines.delay(500)
        val states = listOf(
            AuctionStateResponse(
                productId = "prod-3",
                currentPrice = BigDecimal("92000000"),
                currentBidderId = "user-bidder-01",
                auctionEndAt = "2026-09-05 20:00"
            )
        )
        return PageResponse(
            content = states,
            totalElements = states.size.toLong(),
            totalPages = 1,
            number = 0
        )
    }

    override suspend fun getPendingProducts() = PageResponse<ProductSummary>(emptyList())
    override suspend fun getReviewedProducts(result: String, size: Int, sort: String) = PageResponse<ProductSummary>(emptyList())
    override suspend fun approveProduct(id: String): retrofit2.Response<Unit> = retrofit2.Response.success(Unit)
    override suspend fun rejectProduct(id: String, body: RejectProductRequest): retrofit2.Response<Unit> = retrofit2.Response.success(Unit)
    override suspend fun getProfile() = UserResponse("1", "admin@auction.local", "Admin", true)

    override suspend fun getCategories(): List<CategoryDto> {
        kotlinx.coroutines.delay(300)
        return listOf(
            CategoryDto(id = "1", name = "Electronics"),
            CategoryDto(id = "2", name = "Vehicles"),
            CategoryDto(id = "3", name = "Fashion")
        )
    }

    private data class ProductItem(
        val id: String,
        val sellerId: String,
        val title: String,
        val description: String,
        val category: String,
        val startPrice: BigDecimal,
        var currentPrice: BigDecimal,
        var status: String,
        val rejectionReason: String?,
        val createdAt: String,
        val auctionStartAt: String? = null,
        val auctionEndAt: String? = null,
        var imageUrl: String? = null
    ) {
        fun toSummary() = ProductSummary(
            id = id,
            title = title,
            category = category,
            startPrice = startPrice,
            status = status,
            auctionStartAt = auctionStartAt,
            auctionEndAt = auctionEndAt,
            imageUrl = imageUrl
        )

        fun toDetail() = ProductDetail(
            id = id,
            sellerId = sellerId,
            title = title,
            description = description,
            category = category,
            startPrice = startPrice,
            currentPrice = currentPrice,
            status = status,
            rejectionReason = rejectionReason,
            createdAt = createdAt,
            auctionStartAt = auctionStartAt,
            auctionEndAt = auctionEndAt,
            imageUrl = imageUrl
        )
    }
}

