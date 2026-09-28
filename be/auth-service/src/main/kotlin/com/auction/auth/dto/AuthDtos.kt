package com.auction.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:Email(message = "Email không đúng định dạng") @field:NotBlank(message = "Vui lòng nhập email") val email: String,
    @field:NotBlank(message = "Vui lòng nhập mật khẩu") val password: String
)

data class LoginResponse(
    val accessToken: String,
    val userId: String,
    val fullName: String,
    val isAdmin: Boolean
)

data class RegisterRequest(
    @field:Email(message = "Email không đúng định dạng") @field:NotBlank(message = "Vui lòng nhập email") val email: String,
    @field:NotBlank(message = "Vui lòng nhập mật khẩu")
    @field:Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @field:Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu phải gồm cả chữ và số")
    val password: String,
    @field:NotBlank(message = "Vui lòng nhập họ tên") val fullName: String
)

data class RegisterResponse(
    val message: String
)

data class VerifyRequest(
    @field:Email(message = "Email không đúng định dạng") @field:NotBlank(message = "Vui lòng nhập email") val email: String,
    @field:NotBlank(message = "Vui lòng nhập mã xác thực") val token: String
)

data class ResendVerificationRequest(
    @field:Email(message = "Email không đúng định dạng") @field:NotBlank(message = "Vui lòng nhập email") val email: String
)
