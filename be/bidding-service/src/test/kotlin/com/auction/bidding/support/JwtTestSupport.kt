package com.auction.bidding.support

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date
import java.util.UUID

/**
 * Tự ký JWT test bằng đúng thuật toán JwtVerifier dùng để parse (HMAC, jjwt 0.12.x fluent API)
 * — PHẢI dùng cùng secret khai báo ở src/test/resources/application.yml (jwt.secret), nếu không
 * JwtAuthFilter sẽ âm thầm bỏ qua token (coi như request chưa đăng nhập, xem JwtAuthFilter.kt).
 */
object JwtTestSupport {
    private const val TEST_SECRET = "test-only-secret-key-minimum-32-bytes-long!!"
    private val key = Keys.hmacShaKeyFor(TEST_SECRET.toByteArray())

    fun tokenFor(userId: UUID, isAdmin: Boolean = false): String =
        Jwts.builder()
            .subject(userId.toString())
            .claim("isAdmin", isAdmin)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key)
            .compact()

    // Dùng cho test case JWT hỏng/sai chữ ký (E4) — build 1 token hợp lệ rồi phá phần chữ ký
    fun tamperedToken(userId: UUID): String {
        val valid = tokenFor(userId)
        val parts = valid.split(".")
        val tamperedSignature = parts[2].reversed()
        return "${parts[0]}.${parts[1]}.$tamperedSignature"
    }
}
