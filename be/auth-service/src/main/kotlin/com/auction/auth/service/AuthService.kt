package com.auction.auth.service

import com.auction.auth.domain.User
import com.auction.auth.dto.LoginRequest
import com.auction.auth.dto.LoginResponse
import com.auction.auth.dto.RegisterRequest
import com.auction.auth.dto.VerifyRequest
import com.auction.auth.repository.UserRepository
import com.auction.auth.security.JwtUtil
import com.auction.auth.security.TokenBlacklistService
import org.mindrot.jbcrypt.BCrypt
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
    private val blacklist: TokenBlacklistService,
    private val emailService: EmailService
) {
    companion object {
        private const val VERIFICATION_TOKEN_TTL_MINUTES = 15L
    }

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng") }

        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng")
        }

        if (!user.isVerified) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa xác thực email. Vui lòng kiểm tra hộp thư của bạn.")
        }

        val token = jwtUtil.generateToken(user.id!!, user.isAdmin)
        return LoginResponse(
            accessToken = token.token,
            userId = user.id.toString(),
            fullName = user.fullName,
            isAdmin = user.isAdmin
        )
    }

    fun register(request: RegisterRequest): com.auction.auth.dto.RegisterResponse {
        if (request.password.length < 8 || !request.password.contains(Regex("^(?=.*[A-Za-z])(?=.*\\d).+$"))) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số")
        }

        if (userRepository.existsByEmail(request.email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng")
        }

        val token = UUID.randomUUID().toString().substring(0, 6).uppercase()
        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())

        val user = User(
            email = request.email,
            passwordHash = hashedPassword,
            fullName = request.fullName,
            isVerified = false,
            verificationToken = token,
            verificationTokenExpiresAt = Instant.now().plus(VERIFICATION_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES)
        )
        userRepository.save(user)

        emailService.sendVerificationEmail(user.email, token)

        return com.auction.auth.dto.RegisterResponse(
            message = "Đăng ký thành công. Vui lòng kiểm tra email để lấy mã xác thực."
        )
    }

    fun verifyEmail(request: VerifyRequest) {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản với email này") }

        if (user.isVerified) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản đã được xác thực trước đó")
        }

        if (user.verificationToken != request.token) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực không đúng")
        }

        val expiresAt = user.verificationTokenExpiresAt
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực đã hết hạn, vui lòng yêu cầu gửi mã mới")
        }

        user.isVerified = true
        user.verificationToken = null
        user.verificationTokenExpiresAt = null
        userRepository.save(user)
    }

    fun resendVerification(request: com.auction.auth.dto.ResendVerificationRequest): com.auction.auth.dto.RegisterResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản với email này") }

        if (user.isVerified) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản đã được xác thực trước đó")
        }

        val token = UUID.randomUUID().toString().substring(0, 6).uppercase()
        user.verificationToken = token
        user.verificationTokenExpiresAt = Instant.now().plus(VERIFICATION_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES)
        userRepository.save(user)

        emailService.sendVerificationEmail(user.email, token)

        return com.auction.auth.dto.RegisterResponse(
            message = "Đã gửi lại mã xác thực. Vui lòng kiểm tra email."
        )
    }

    fun logout(jti: String?, expiresAt: Instant?) {
        if (jti == null || expiresAt == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ")
        }
        blacklist.blacklist(jti, expiresAt)
    }

    fun getProfile(userId: UUID): com.auction.auth.dto.UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng") }
        return com.auction.auth.dto.UserResponse.from(user)
    }
}
