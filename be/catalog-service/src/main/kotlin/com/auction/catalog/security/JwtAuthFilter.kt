package com.auction.catalog.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest
import java.util.UUID

@Component
class JwtAuthFilter(
    private val verifier: JwtVerifier,
    @Value("\${internal.token:dev-internal-token}") private val internalToken: String
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val traceId = request.getHeader("X-Trace-Id") ?: UUID.randomUUID().toString()
        MDC.put("traceId", traceId)
        try {
            // Endpoint nội bộ do bidding-service gọi: không dùng JWT của người dùng, nhưng bắt buộc có
            // X-Internal-Token — phòng vệ nhiều lớp phòng khi cổng service bị mở ra ngoài nhầm.
            if (request.requestURI.startsWith("/internal/")) {
                val provided = request.getHeader("X-Internal-Token") ?: ""
                if (!MessageDigest.isEqual(provided.toByteArray(), internalToken.toByteArray())) {
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.contentType = "application/json"
                    response.writer.write("""{"statusCode":403,"message":"Endpoint nội bộ, không được truy cập"}""")
                    return
                }
                filterChain.doFilter(request, response)
                return
            }
            val header = request.getHeader("Authorization")
            if (header != null && header.startsWith("Bearer ")) {
                runCatching {
                    val claims = verifier.parse(header.removePrefix("Bearer ").trim())
                    request.setAttribute("userId", claims.subject)
                    request.setAttribute("isAdmin", claims["isAdmin"] as? Boolean ?: false)
                }
            }
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove("traceId")
        }
    }
}
