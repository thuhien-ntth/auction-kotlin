package com.auction.catalog.controller

import com.auction.catalog.domain.ProductStatus
import com.auction.catalog.dto.CreateProductRequest
import com.auction.catalog.dto.ProductResponse
import com.auction.catalog.dto.ProductSummaryResponse
import com.auction.catalog.security.currentUserOrNull
import com.auction.catalog.security.requireMember
import com.auction.catalog.service.ProductService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

// Các route dưới đây được Gateway map vào /api/...
@RestController
class ProductController(private val productService: ProductService) {

    // Product search — GET /api/products?keyword=&category=
    @GetMapping("/products")
    // Public; nếu có JWT admin hợp lệ thì trả thêm sản phẩm đã duyệt / bị từ chối (xem ProductService.search)
    fun search(
        request: HttpServletRequest,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        pageable: Pageable
    ): Page<ProductSummaryResponse> =
        productService.search(keyword, category, request.currentUserOrNull()?.isAdmin == true, pageable)

    // Product detail — GET /api/products/{id}
    @GetMapping("/products/{id}")
    fun detail(@PathVariable id: UUID): ProductResponse = productService.getDetail(id)

    // Exhibition product register — POST /api/products
    @PostMapping("/products")
    fun register(
        request: HttpServletRequest,
        @Valid @RequestBody body: CreateProductRequest
    ): ResponseEntity<ProductResponse> {
        val user = request.requireMember()
        return ResponseEntity.status(201).body(productService.register(user.id, body))
    }

    // Update product — PUT /api/products/{id}
    @PutMapping("/products/{id}")
    fun update(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @Valid @RequestBody body: CreateProductRequest
    ): ResponseEntity<ProductResponse> {
        val user = request.requireMember()
        return ResponseEntity.ok(productService.update(user.id, id, body))
    }

    // Exhibition management — GET /api/my/products?status=
    @GetMapping("/my/products")
    fun myProducts(
        request: HttpServletRequest,
        @RequestParam(required = false) status: ProductStatus?,
        pageable: Pageable
    ): Page<ProductSummaryResponse> {
        val user = request.requireMember()
        return productService.myProducts(user.id, status, pageable)
    }

    // Upload/thay ảnh sản phẩm — POST /api/products/{id}/image (multipart/form-data, field "file").
    // Chỉ seller sở hữu sản phẩm mới được gọi (kiểm tra trong service).
    @PostMapping("/products/{id}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @RequestParam("file") file: MultipartFile
    ): ProductResponse {
        val user = request.requireMember()
        return productService.uploadImage(user.id, id, file)
    }

    // Trả ảnh sản phẩm — GET /api/products/{id}/image — public, giống GET /api/products/{id}.
    @GetMapping("/products/{id}/image")
    fun getImage(@PathVariable id: UUID): ResponseEntity<Resource> {
        val (resource, mediaType) = productService.getImageResource(id)
        return ResponseEntity.ok().contentType(mediaType).body(resource)
    }
}
