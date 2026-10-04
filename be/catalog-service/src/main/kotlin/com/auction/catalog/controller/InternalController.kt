package com.auction.catalog.controller

import com.auction.catalog.dto.InternalProductBrief
import com.auction.catalog.dto.InternalProductInfo
import com.auction.catalog.dto.ProductBriefRequest
import com.auction.catalog.repository.ProductRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import com.auction.catalog.service.ProductService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/internal")
class InternalController(
    private val productService: ProductService,
    private val productRepository: ProductRepository
) {

    @GetMapping("/products/{id}")
    fun get(@PathVariable id: UUID): InternalProductInfo = productService.internalGet(id)

    
    @PostMapping("/products/briefs")
    fun briefs(@RequestBody body: ProductBriefRequest): List<InternalProductBrief> =
        productRepository.findAllById(body.productIds.distinct().take(500)).map(InternalProductBrief::from)
}
