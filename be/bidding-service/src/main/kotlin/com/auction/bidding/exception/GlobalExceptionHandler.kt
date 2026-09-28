package com.auction.bidding.exception

import com.auction.bidding.dto.ApiError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiError> =
        ResponseEntity.status(ex.statusCode)
            .body(ApiError(ex.statusCode.value(), ex.reason ?: defaultMessageFor(ex.statusCode.value())))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = ex.bindingResult.fieldErrors
            .mapNotNull { it.defaultMessage }
            .distinct()
            .joinToString("; ")
            .ifBlank { "Dữ liệu không hợp lệ" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError(400, message))
    }

    // JSON sai định dạng (vd: Instant/số không parse được, body rỗng) là lỗi của client -> 400, không phải 500.
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError(400, "Dữ liệu gửi lên sai định dạng"))

    // Tham số path/query sai kiểu (vd. id không phải UUID) -> 400 thay vì 500
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: org.springframework.web.method.annotation.MethodArgumentTypeMismatchException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError(400, "Tham số '${ex.name}' không hợp lệ"))

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiError> {
        log.error("Unhandled exception", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(500, "Lỗi hệ thống, vui lòng thử lại sau"))
    }

    private companion object {
        val log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    // Thông báo tiếng Việt mặc định khi ResponseStatusException không kèm reason
    private fun defaultMessageFor(status: Int): String = when (status) {
        400 -> "Yêu cầu không hợp lệ"
        401 -> "Vui lòng đăng nhập"
        403 -> "Bạn không có quyền thực hiện thao tác này"
        404 -> "Không tìm thấy dữ liệu"
        409 -> "Dữ liệu bị xung đột, vui lòng thử lại"
        503 -> "Dịch vụ tạm thời không khả dụng"
        else -> "Đã có lỗi xảy ra"
    }
}
