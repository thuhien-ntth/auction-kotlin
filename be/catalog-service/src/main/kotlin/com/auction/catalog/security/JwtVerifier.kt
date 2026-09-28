package com.auction.catalog.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

/**
 * catalog-service KHÔNG phát hành token, chỉ verify lại chữ ký bằng chung JWT_SECRET
 * với auth-service — không tin header X-User-* do Gateway forward (defense in depth,
 * xem ARCHITECTURE_DESIGN.md mục 4/5.1 - DC2 Addressing security concerns).
 */
@Component
class JwtVerifier(@Value("\${jwt.secret}") secret: String) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun parse(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}
