package com.auction.gateway.filter

import com.auction.gateway.security.JwtVerifier
import io.jsonwebtoken.JwtException
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.util.UUID

@Component
class JwtGatewayFilter(
    private val jwtVerifier: JwtVerifier,
    private val redis: ReactiveStringRedisTemplate
) : GlobalFilter, Ordered {

    override fun getOrder(): Int = -1

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val traceId = request.headers.getFirst("X-Trace-Id") ?: UUID.randomUUID().toString()
        val authHeader = request.headers.getFirst("Authorization")

        val mutatedBuilder = exchange.request.mutate().header("X-Trace-Id", traceId)

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange.mutate().request(mutatedBuilder.build()).build())
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        val claims = try {
            jwtVerifier.parse(token)
        } catch (ex: JwtException) {
            return unauthorized(exchange, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn, vui lòng đăng nhập lại")
        }

        val jti = claims.id
        return redis.hasKey("auth:blacklist:$jti")
            .defaultIfEmpty(false)
            .flatMap { blacklisted ->
                if (blacklisted) {
                    unauthorized(exchange, "Phiên đăng nhập đã đăng xuất, vui lòng đăng nhập lại")
                } else {
                    val isAdmin = (claims["isAdmin"] as? Boolean) ?: false
                    val mutatedRequest = mutatedBuilder
                        .header("X-User-Id", claims.subject)
                        .header("X-User-Admin", isAdmin.toString())
                        .build()
                    chain.filter(exchange.mutate().request(mutatedRequest).build())
                }
            }
    }

    private fun unauthorized(exchange: ServerWebExchange, message: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.contentType = MediaType.APPLICATION_JSON
        response.headers.add("X-Auth-Error", "unauthorized")
        val escaped = message.replace("\"", "'")
        val body = """{"statusCode":401,"message":"$escaped"}"""
        val buffer: DataBuffer = response.bufferFactory().wrap(body.toByteArray(StandardCharsets.UTF_8))
        return response.writeWith(Mono.just(buffer))
    }
}
