package com.auction.auth.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Chỉ dùng để nhận diện user cho endpoint /api/auth/logout (cần biết jti để revoke).
 * Không chặn request thiếu token vì phần lớn endpoint của auth-service là public (login).
 */
@Component
class JwtAuthFilter(
    private val jwtUtil: JwtUtil,
    private val blacklist: TokenBlacklistService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Propagate traceId do Gateway sinh ra -> log của mọi service cùng 1 request có thể grep chung
        // (giảm nhẹ monitoring challenge MC1 "collection of logs from containers" — ARCHITECTURE_DESIGN.md mục 5.2)
        val traceId = request.getHeader("X-Trace-Id") ?: UUID.randomUUID().toString()
        MDC.put("traceId", traceId)
        try {
            val header = request.getHeader("Authorization")
            if (header != null && header.startsWith("Bearer ")) {
                val token = header.removePrefix("Bearer ").trim()
                runCatching {
                    val claims = jwtUtil.parse(token)
                    if (!blacklist.isBlacklisted(claims.id)) {
                        request.setAttribute("userId", claims.subject)
                        request.setAttribute("jti", claims.id)
                        request.setAttribute("tokenExpiresAt", claims.expiration.toInstant())
                    }
                }
            }
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove("traceId")
        }
    }
}
