package com.auction.auth.controller

import com.auction.auth.dto.LoginRequest
import com.auction.auth.dto.LoginResponse
import com.auction.auth.dto.RegisterRequest
import com.auction.auth.dto.VerifyRequest
import com.auction.auth.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> =
        ResponseEntity.ok(authService.login(request))

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: com.auction.auth.dto.RegisterRequest): ResponseEntity<com.auction.auth.dto.RegisterResponse> {
        return ResponseEntity.ok(authService.register(request))
    }

    @PostMapping("/verify")
    fun verify(@Valid @RequestBody request: VerifyRequest): ResponseEntity<Void> {
        authService.verifyEmail(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/resend-verification")
    fun resendVerification(
        @Valid @RequestBody request: com.auction.auth.dto.ResendVerificationRequest
    ): ResponseEntity<com.auction.auth.dto.RegisterResponse> {
        return ResponseEntity.ok(authService.resendVerification(request))
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        val jti = request.getAttribute("jti") as String?
        val expiresAt = request.getAttribute("tokenExpiresAt") as Instant?
        authService.logout(jti, expiresAt)
        return ResponseEntity.noContent().build()
    }
}
