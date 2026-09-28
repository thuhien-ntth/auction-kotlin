package com.auction.auth.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Bounded context: Identity & Access.
 * Chỉ 1 field isAdmin (boolean) thay vì bảng "role" riêng — đủ cho yêu cầu tối thiểu
 * "vai trò Admin tách biệt", xem ARCHITECTURE_DESIGN.md mục 1.1.
 */
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "full_name", nullable = false)
    val fullName: String,

    @Column(name = "is_admin", nullable = false)
    val isAdmin: Boolean = false,

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean = false,

    @Column(name = "verification_token")
    var verificationToken: String? = null,

    @Column(name = "verification_token_expires_at")
    var verificationTokenExpiresAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
