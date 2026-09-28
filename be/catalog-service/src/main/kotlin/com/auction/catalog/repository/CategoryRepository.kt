package com.auction.catalog.repository

import com.auction.catalog.domain.Category
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CategoryRepository : JpaRepository<Category, UUID> {
    fun existsByName(name: String): Boolean
    fun findByName(name: String): Category?
    fun findAllByOrderBySortOrderAscNameAsc(): List<Category>
}
