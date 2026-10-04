package com.auction.catalog.controller

import com.auction.catalog.dto.ProductResponse
import com.auction.catalog.dto.ProductSummaryResponse
import com.auction.catalog.dto.RejectProductRequest
import com.auction.catalog.security.requireAdmin
import com.auction.catalog.service.ProductService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*
import java.util.UUID


@RestController
@RequestMapping("/admin")
class AdminController(
    private val productService: ProductService
) {
    @GetMapping("/products/pending")
    fun pending(request: HttpServletRequest, pageable: Pageable): Page<ProductSummaryResponse> {
        request.requireAdmin()
        return productService.pendingApproval(pageable)
    }

    
    @GetMapping("/products/reviewed")
    fun reviewed(
        request: HttpServletRequest,
        @RequestParam(required = false) result: String?,
        pageable: Pageable
    ): Page<ProductSummaryResponse> {
        request.requireAdmin()
        return productService.reviewedProducts(result, pageable)
    }

    @PostMapping("/products/{id}/approve")
    fun approve(request: HttpServletRequest, @PathVariable id: UUID): ProductResponse {
        request.requireAdmin()
        return productService.approve(id)
    }

    @PostMapping("/products/{id}/reject")
    fun reject(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @Valid @RequestBody body: RejectProductRequest
    ): ProductResponse {
        request.requireAdmin()
        return productService.reject(id, body.reason)
    }
}
