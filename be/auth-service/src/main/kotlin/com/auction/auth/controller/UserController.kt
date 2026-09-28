package com.auction.auth.controller

import com.auction.auth.dto.UserResponse
import com.auction.auth.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(private val authService: AuthService) {

    @GetMapping("/me")
    fun getProfile(request: HttpServletRequest): UserResponse {
        val userIdStr = request.getAttribute("userId") as String?
            ?: throw org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Không tìm thấy thông tin đăng nhập")
        return authService.getProfile(java.util.UUID.fromString(userIdStr))
    }
}
