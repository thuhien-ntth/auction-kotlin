package com.auction.catalog.dto

import com.auction.catalog.domain.Category
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val createdAt: Instant
) {
    companion object {
        fun from(c: Category) = CategoryResponse(
            id = c.id!!,
            name = c.name,
            createdAt = c.createdAt
        )
    }
}

