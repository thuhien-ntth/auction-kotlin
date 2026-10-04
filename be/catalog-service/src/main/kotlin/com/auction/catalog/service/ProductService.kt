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

    
    
    
    
    fun search(keyword: String?, category: String?, isAdmin: Boolean, pageable: Pageable): Page<ProductSummaryResponse> {
        val statuses = if (isAdmin) ALL_STATUSES else PUBLIC_STATUSES
        val page = productRepository.search(keyword?.trim()?.ifBlank { null }, category, statuses, pageable)
        
        val counts = biddingClient.getBidderCounts(page.content.mapNotNull { it.id })
        return page.map { ProductSummaryResponse.from(it).copy(bidderCount = counts[it.id] ?: 0L) }
    }

    
    fun getDetail(productId: UUID): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm") }
        val currentPrice = biddingClient.getCurrentPrice(productId).price ?: product.startPrice
        return ProductResponse.from(product, currentPrice)
    }

    
    fun myProducts(sellerId: UUID, status: ProductStatus?, pageable: Pageable): Page<ProductSummaryResponse> {
        val page = if (status != null) {
            productRepository.findBySellerIdAndStatus(sellerId, status, pageable)
        } else {
            productRepository.findBySellerId(sellerId, pageable)
        }
        return page.map(ProductSummaryResponse::from)
    }

    
    
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

    
    
    fun getImageResource(productId: UUID): Pair<Resource, MediaType> {
        val product = productRepository.findById(productId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm") }
        val path = product.imagePath
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm chưa có ảnh")
        return imageStorageService.load(path) to imageStorageService.contentTypeFor(path)
    }

    

    fun pendingApproval(pageable: Pageable): Page<ProductSummaryResponse> =
        productRepository.findByStatus(ProductStatus.PENDING_APPROVAL, pageable).map(ProductSummaryResponse::from)

    
    
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
        
        val SUPPORTED_CURRENCIES = setOf("VND", "USD", "EUR")
    }
}
