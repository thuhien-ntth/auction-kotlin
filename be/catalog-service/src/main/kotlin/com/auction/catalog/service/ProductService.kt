package com.auction.catalog.service

import com.auction.catalog.client.BiddingClient
import com.auction.catalog.domain.Product
import com.auction.catalog.domain.ProductStatus
import com.auction.catalog.dto.*
import com.auction.catalog.repository.ProductRepository
import com.auction.catalog.storage.ProductImageStorageService
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val biddingClient: BiddingClient,
    private val imageStorageService: ProductImageStorageService,
    private val categoryService: CategoryService
) {
    // Seller đăng sản phẩm (USRPS0010000/0030000) -> luôn PENDING_APPROVAL
    fun register(sellerId: UUID, request: CreateProductRequest): ProductResponse {
        val startAt = request.auctionStartAt!!
        val endAt = request.auctionEndAt!!
        if (!endAt.isAfter(startAt)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu")
        }
        if (!endAt.isAfter(Instant.now())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải ở tương lai")
        }
        val currency = request.currency.trim().uppercase()
        if (currency !in SUPPORTED_CURRENCIES) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn vị tiền tệ phải là một trong: ${SUPPORTED_CURRENCIES.joinToString(", ")}")
        }
        val categoryName = request.category.trim()
        if (!categoryService.existsByName(categoryName)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục '$categoryName' không tồn tại")
        }
        val saved = productRepository.save(
            Product(
                sellerId = sellerId,
                title = request.title,
                description = request.description,
                category = categoryName,
                startPrice = request.startPrice,
                auctionStartAt = startAt,
                auctionEndAt = endAt,
                status = ProductStatus.PENDING_APPROVAL,
                currency = currency
            )
        )
        return ProductResponse.from(saved, saved.startPrice)
    }

    // Cập nhật sản phẩm - chỉ cho phép khi sản phẩm đang chờ duyệt (PENDING_APPROVAL)
    fun update(sellerId: UUID, productId: UUID, request: CreateProductRequest): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm") }
        if (product.sellerId != sellerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Đây không phải sản phẩm của bạn")
        }
        if (product.status != ProductStatus.PENDING_APPROVAL) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể chỉnh sửa sản phẩm khi đang chờ duyệt")
        }
        val startAt = request.auctionStartAt!!
        val endAt = request.auctionEndAt!!
        // Chỉ kiểm tra điều kiện thời gian khi người bán THỰC SỰ đổi thời gian.
        // Mở form rồi bấm "Cập nhật" ngay (không sửa gì) thì vẫn lưu được, kể cả khi thời gian cũ đã qua.
        val timeChanged = startAt.compareTo(product.auctionStartAt) != 0 || endAt.compareTo(product.auctionEndAt) != 0
        if (timeChanged) {
            if (!endAt.isAfter(startAt)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu")
            }
            if (!endAt.isAfter(Instant.now())) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải ở tương lai")
            }
        }
        val currency = request.currency.trim().uppercase()
        if (currency !in SUPPORTED_CURRENCIES) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn vị tiền tệ phải là một trong: ${SUPPORTED_CURRENCIES.joinToString(", ")}")
        }
        val categoryName = request.category.trim()
        if (!categoryService.existsByName(categoryName)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục '$categoryName' không tồn tại")
        }

        product.title = request.title
        product.description = request.description
        product.category = categoryName
        product.startPrice = request.startPrice
        product.auctionStartAt = startAt
        product.auctionEndAt = endAt
        product.currency = currency
        product.updatedAt = Instant.now()

        val saved = productRepository.save(product)
        return ProductResponse.from(saved, saved.startPrice)
    }

    // Product search (USEDS001F003)
    // - Người dùng thường / chưa đăng nhập: chỉ sản phẩm ACTIVE (đang trong thời gian đấu giá)
    // - Admin: tất cả sản phẩm mọi trạng thái (chờ duyệt, đã duyệt, đang đấu giá, đã kết thúc,
    //   bị từ chối kèm rejectionReason). Việc duyệt thao tác ở màn "Duyệt sản phẩm" (/admin/products/pending).
    fun search(keyword: String?, category: String?, isAdmin: Boolean, pageable: Pageable): Page<ProductSummaryResponse> {
        val statuses = if (isAdmin) ALL_STATUSES else PUBLIC_STATUSES
        val page = productRepository.search(keyword?.trim()?.ifBlank { null }, category, statuses, pageable)
        // 1 lời gọi batch cho cả trang để lấy số người tham gia (tránh N+1 cross-service call)
        val counts = biddingClient.getBidderCounts(page.content.mapNotNull { it.id })
        return page.map { ProductSummaryResponse.from(it).copy(bidderCount = counts[it.id] ?: 0L) }
    }

    // Product detail (USEDS0030000) — ghép currentPrice từ bidding-service
    fun getDetail(productId: UUID): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm") }
        val currentPrice = biddingClient.getCurrentPrice(productId).price ?: product.startPrice
        return ProductResponse.from(product, currentPrice)
    }

    // Exhibition management (USMPS0130000, gộp từ USMPS0080000..USMPS0120000)
    fun myProducts(sellerId: UUID, status: ProductStatus?, pageable: Pageable): Page<ProductSummaryResponse> {
        val page = if (status != null) {
            productRepository.findBySellerIdAndStatus(sellerId, status, pageable)
        } else {
            productRepository.findBySellerId(sellerId, pageable)
        }
        return page.map(ProductSummaryResponse::from)
    }

    // Upload/thay ảnh sản phẩm (USRPS0010000 bổ sung) — chỉ chủ sở hữu (seller) mới được upload,
    // không giới hạn theo status: seller có thể đổi ảnh bất cứ lúc nào kể cả sau khi ACTIVE.
    fun uploadImage(sellerId: UUID, productId: UUID, file: MultipartFile): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm") }
        if (product.sellerId != sellerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Đây không phải sản phẩm của bạn")
        }
        product.imagePath = imageStorageService.save(productId, file)
        product.updatedAt = Instant.now()
        val saved = productRepository.save(product)
        val currentPrice = biddingClient.getCurrentPrice(productId).price ?: saved.startPrice
        return ProductResponse.from(saved, currentPrice)
    }

    // Phục vụ GET /products/{id}/image — public (không requireUser), khớp với việc
    // GET /products/{id} cũng đang public. Trả kèm Content-Type suy ra từ đuôi file đã lưu.
    fun getImageResource(productId: UUID): Pair<Resource, MediaType> {
        val product = productRepository.findById(productId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm") }
        val path = product.imagePath
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm chưa có ảnh")
        return imageStorageService.load(path) to imageStorageService.contentTypeFor(path)
    }

    // ---- Admin ----

    fun pendingApproval(pageable: Pageable): Page<ProductSummaryResponse> =
        productRepository.findByStatus(ProductStatus.PENDING_APPROVAL, pageable).map(ProductSummaryResponse::from)

    // Admin: sản phẩm đã xét duyệt. result = APPROVED (đã duyệt, gồm cả các trạng thái sau duyệt:
    // ACTIVE/SOLD/ENDED_NO_BID), REJECTED (bị từ chối, kèm lý do), null = cả hai. Mặc định mới xử lý trước.
    fun reviewedProducts(result: String?, pageable: Pageable): Page<ProductSummaryResponse> {
        val approved = listOf(ProductStatus.APPROVED, ProductStatus.ACTIVE, ProductStatus.SOLD, ProductStatus.ENDED_NO_BID)
        val statuses = when (result?.uppercase()) {
            null, "" -> approved + ProductStatus.REJECTED
            "APPROVED" -> approved
            "REJECTED" -> listOf(ProductStatus.REJECTED)
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Kết quả duyệt phải là APPROVED hoặc REJECTED")
        }
        val page = if (pageable.sort.isSorted) pageable
        else org.springframework.data.domain.PageRequest.of(
            pageable.pageNumber, pageable.pageSize,
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedAt")
        )
        return productRepository.findByStatusIn(statuses, page).map(ProductSummaryResponse::from)
    }

    fun approve(productId: UUID): ProductResponse {
        val product = getPendingOrThrow(productId)
        if (!product.auctionEndAt.isAfter(Instant.now())) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Đã quá thời gian kết thúc đấu giá, không thể duyệt")
        }
        product.status = ProductStatus.APPROVED
        product.updatedAt = Instant.now()
        return ProductResponse.from(productRepository.save(product), product.startPrice)
    }

    fun reject(productId: UUID, reason: String): ProductResponse {
        val product = getPendingOrThrow(productId)
        product.status = ProductStatus.REJECTED
        product.rejectionReason = reason
        product.updatedAt = Instant.now()
        return ProductResponse.from(productRepository.save(product), product.startPrice)
    }

    private fun getPendingOrThrow(productId: UUID): Product {
        val product = productRepository.findById(productId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm") }
        if (product.status != ProductStatus.PENDING_APPROVAL) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Sản phẩm không ở trạng thái chờ duyệt")
        }
        return product
    }

    // ---- Internal (gọi bởi bidding-service) ----

    fun internalGet(productId: UUID): InternalProductInfo {
        val product = productRepository.findById(productId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm") }
        return InternalProductInfo(
            id = product.id!!,
            sellerId = product.sellerId,
            startPrice = product.startPrice,
            status = product.status,
            auctionStartAt = product.auctionStartAt,
            auctionEndAt = product.auctionEndAt,
            currency = product.currency
        )
    }

    private companion object {
        val PUBLIC_STATUSES = listOf(ProductStatus.ACTIVE)
        val ALL_STATUSES = ProductStatus.entries.toList()
        // Hardcode theo yêu cầu: VND, Đô la Mỹ, Euro
        val SUPPORTED_CURRENCIES = setOf("VND", "USD", "EUR")
    }
}
