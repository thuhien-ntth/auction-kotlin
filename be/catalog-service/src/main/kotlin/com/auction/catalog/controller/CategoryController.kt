package com.auction.catalog.controller

import com.auction.catalog.dto.CategoryResponse
import com.auction.catalog.service.CategoryService
import org.springframework.web.bind.annotation.*

// GET /categories — public (không cần JWT), cho phép app lấy danh sách danh mục để điền dropdown
// POST, PUT, DELETE /admin/categories — chỉ Admin
@RestController
class CategoryController(
    private val categoryService: CategoryService
) {
    // Public: lấy danh sách tất cả danh mục, dùng cho dropdown ở client
    @GetMapping("/categories")
    fun listAll(): List<CategoryResponse> = categoryService.listAll()


}
