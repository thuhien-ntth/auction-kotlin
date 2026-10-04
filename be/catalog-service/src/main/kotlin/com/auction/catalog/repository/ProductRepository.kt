package com.auction.catalog.repository

import com.auction.catalog.domain.Product
import com.auction.catalog.domain.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

interface ProductRepository : JpaRepository<Product, UUID> {

    fun findBySellerId(sellerId: UUID, pageable: Pageable): Page<Product>

    fun findBySellerIdAndStatus(sellerId: UUID, status: ProductStatus, pageable: Pageable): Page<Product>

    
    
    
    
    @Query(
        """
        SELECT p FROM Product p
        WHERE p.status IN :statuses
        AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        AND (:category IS NULL OR p.category = :category)
        """
    )
    fun search(
        @Param("keyword") keyword: String?,
        @Param("category") category: String?,
        @Param("statuses") statuses: Collection<ProductStatus>,
        pageable: Pageable
    ): Page<Product>

    fun findByStatus(status: ProductStatus, pageable: Pageable): Page<Product>

    
    fun findByStatusIn(statuses: Collection<ProductStatus>, pageable: Pageable): Page<Product>

    
    
    
    @Query(
        """
        SELECT p FROM Product p
        WHERE p.status IN ('APPROVED', 'ACTIVE')
        AND p.auctionEndAt <= :now
        ORDER BY p.auctionEndAt ASC
        """
    )
    fun findExpired(@Param("now") now: Instant, pageable: Pageable): List<Product>

    
    
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE Product p SET p.status = 'ACTIVE', p.updatedAt = :now
        WHERE p.status = 'APPROVED' AND p.auctionStartAt <= :now AND p.auctionEndAt > :now
        """
    )
    fun openStarted(@Param("now") now: Instant): Int

    
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        "UPDATE Product p SET p.status = :target, p.updatedAt = :now " +
            "WHERE p.id = :id AND p.status IN ('APPROVED', 'ACTIVE')"
    )
    fun closeIfOpen(
        @Param("id") id: UUID,
        @Param("target") target: ProductStatus,
        @Param("now") now: Instant
    ): Int
}
