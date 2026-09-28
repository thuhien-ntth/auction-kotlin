package com.auction.catalog.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "categories")
class Category(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    var name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    // Thứ tự hiển thị trong dropdown (nhỏ lên trước). Xem migration V8.
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 1000
)
