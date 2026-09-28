package com.auction.auth.dto

import com.auction.auth.domain.User
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val email: String,
    val fullName: String,
    val isAdmin: Boolean
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                email = user.email,
                fullName = user.fullName,
                isAdmin = user.isAdmin
            )
        }
    }
}
