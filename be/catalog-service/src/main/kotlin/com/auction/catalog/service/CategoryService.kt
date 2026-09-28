package com.auction.catalog.service

import com.auction.catalog.domain.Category
import com.auction.catalog.dto.CategoryResponse
import com.auction.catalog.repository.CategoryRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {
    fun listAll(): List<CategoryResponse> =
        categoryRepository.findAllByOrderBySortOrderAscNameAsc().map(CategoryResponse::from)



    fun existsByName(name: String): Boolean = categoryRepository.existsByName(name)
}
