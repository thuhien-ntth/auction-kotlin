package com.auction.auth.security

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class TokenBlacklistService(private val redis: StringRedisTemplate) {

    fun blacklist(jti: String, expiresAt: Instant) {
        val ttl = Duration.between(Instant.now(), expiresAt)
        if (ttl.isNegative || ttl.isZero) return
        redis.opsForValue().set(key(jti), "revoked", ttl)
    }

    fun isBlacklisted(jti: String): Boolean = redis.hasKey(key(jti))

    private fun key(jti: String) = "auth:blacklist:$jti"
}
