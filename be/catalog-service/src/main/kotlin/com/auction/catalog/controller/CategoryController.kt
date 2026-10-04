package com.auction.catalog.controller

import com.auction.catalog.dto.CategoryResponse
import com.auction.catalog.service.CategoryService
import org.springframework.web.bind.annotation.*



@RestController
class CategoryController(
    private val categoryService: CategoryService
) {
    
    @GetMapping("/categories")
    fun listAll(): List<CategoryResponse> = categoryService.listAll()


}
