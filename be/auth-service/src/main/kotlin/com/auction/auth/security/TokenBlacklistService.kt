package com.auction.auth.security

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Logout với JWT stateless: revoke bằng cách đưa jti vào Redis với TTL = thời gian
 * còn lại của token (tự hết hạn, không cần dọn dẹp thủ công).
 * Chỉ api-gateway + auth-service kiểm tra blacklist này (xem ARCHITECTURE_DESIGN.md mục 4,
 * hàng "Bảo mật giữa các service") — catalog-service/bidding-service chỉ verify chữ ký+hạn
 * dùng, không gọi Redis cho việc này để giữ độc lập.
 */
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
