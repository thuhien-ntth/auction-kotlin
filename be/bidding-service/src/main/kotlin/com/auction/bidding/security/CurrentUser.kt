package com.auction.bidding.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class CurrentUser(val id: UUID, val isAdmin: Boolean)

fun HttpServletRequest.requireUser(): CurrentUser {
    val userId = getAttribute("userId") as String?
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập")
    val isAdmin = getAttribute("isAdmin") as? Boolean ?: false
    return CurrentUser(UUID.fromString(userId), isAdmin)
}

// Admin chỉ quản trị: không được đăng/sở hữu sản phẩm, không được đặt giá.
fun HttpServletRequest.requireMember(): CurrentUser {
    val user = requireUser()
    if (user.isAdmin) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản quản trị không được dùng chức năng này")
    return user
}
