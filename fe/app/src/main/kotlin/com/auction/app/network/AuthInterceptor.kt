package com.auction.app.network

import com.auction.app.data.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

// Các endpoint không cần đăng nhập — KHÔNG được gắn token cũ vào đây. Nếu gắn, Gateway
// (JwtGatewayFilter) sẽ tự verify token đó và chặn 401 "Invalid or expired token" ngay cả
// khi request là đăng nhập/đăng ký, trước khi tới được auth-service — lỗi thật gặp phải khi
// máy còn lưu access token cũ (hết hạn/đã logout/DB đã bị reset) từ lần đăng nhập trước.
private val NO_AUTH_PATH_SUFFIXES = listOf(
    "auth/login", "auth/register", "auth/verify", "auth/resend-verification"
)

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        val needsAuth = NO_AUTH_PATH_SUFFIXES.none { path.endsWith(it) }
        val token = if (needsAuth) tokenStore.accessTokenBlocking() else null
        val request = if (token != null) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
