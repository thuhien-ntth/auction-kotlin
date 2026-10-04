package com.auction.auth.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-minutes:120}") private val expirationMinutes: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(userId: UUID, isAdmin: Boolean): TokenResult {
        val now = Instant.now()
        val expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES)
        val jti = UUID.randomUUID().toString()
        val token = Jwts.builder()
            .id(jti)
            .subject(userId.toString())
            .claim("isAdmin", isAdmin)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact()
        return TokenResult(token, jti, expiry)
    }

    fun parse(token: String): io.jsonwebtoken.Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

    data class TokenResult(val token: String, val jti: String, val expiresAt: Instant)
}
