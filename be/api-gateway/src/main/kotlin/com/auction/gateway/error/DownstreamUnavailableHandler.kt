package com.auction.gateway.error

import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.WebExceptionHandler
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

@Component
@Order(-2)
class DownstreamUnavailableHandler : WebExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        val status = classify(ex) ?: return Mono.error(ex)
        val response = exchange.response
        if (response.isCommitted) return Mono.error(ex)

        log.warn("Downstream unavailable for {} {} -> {} ({})",
            exchange.request.method, exchange.request.path, status.value(), rootMessage(ex))

        val message = if (status == HttpStatus.GATEWAY_TIMEOUT)
            "Hệ thống phản hồi quá lâu, vui lòng thử lại"
        else
            "Hệ thống tạm thời không khả dụng, vui lòng thử lại"

        response.statusCode = status
        response.headers.contentType = MediaType.APPLICATION_JSON
        val body = """{"statusCode":${status.value()},"message":"$message"}""".toByteArray()
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)))
    }

    private fun classify(ex: Throwable): HttpStatus? {
        var t: Throwable? = ex
        var depth = 0
        while (t != null && depth++ < 10) {
            when (t) {
                is ConnectException -> return HttpStatus.SERVICE_UNAVAILABLE
                is SocketTimeoutException, is TimeoutException -> return HttpStatus.GATEWAY_TIMEOUT
            }
            if (t.javaClass.name == "io.netty.handler.timeout.ReadTimeoutException" ||
                t.javaClass.name == "io.netty.channel.ConnectTimeoutException") return HttpStatus.GATEWAY_TIMEOUT
            t = t.cause
        }
        return null
    }

    private fun rootMessage(ex: Throwable): String {
        var t = ex
        while (t.cause != null && t.cause !== t) t = t.cause!!
        return t.message ?: t.javaClass.simpleName
    }
}
