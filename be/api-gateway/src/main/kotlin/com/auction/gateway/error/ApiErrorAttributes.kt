package com.auction.gateway.error

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

// Chuẩn hoá format lỗi mặc định của Spring Cloud Gateway (vốn trả {timestamp, path, status, error,
// requestId, ...}) về đúng format chung {statusCode, message} — áp dụng cho lỗi phát sinh ngay tại
// Gateway (route không khớp, downstream service không phản hồi được, v.v.), khác với lỗi 401 tự
// viết tay trong JwtGatewayFilter (đã tự trả đúng format) và lỗi trả về nguyên văn từ các service
// phía sau (đã có GlobalExceptionHandler riêng ở mỗi service).
@Component
class ApiErrorAttributes : DefaultErrorAttributes() {

    override fun getErrorAttributes(request: ServerRequest, options: ErrorAttributeOptions): MutableMap<String, Any> {
        val defaults = super.getErrorAttributes(request, options)
        val statusCode = (defaults["status"] as? Int) ?: 500
        // Lỗi phát sinh tại Gateway (route không khớp, 405...) — Spring trả message tiếng Anh, dịch theo status
        val message = when (statusCode) {
            400 -> "Yêu cầu không hợp lệ"
            401 -> "Vui lòng đăng nhập"
            403 -> "Bạn không có quyền thực hiện thao tác này"
            404 -> "Không tìm thấy đường dẫn yêu cầu"
            405 -> "Phương thức không được hỗ trợ"
            429 -> "Bạn thao tác quá nhanh, vui lòng thử lại sau"
            502, 503 -> "Hệ thống tạm thời không khả dụng, vui lòng thử lại"
            504 -> "Hệ thống phản hồi quá lâu, vui lòng thử lại"
            else -> "Đã có lỗi xảy ra, vui lòng thử lại"
        }
        return mutableMapOf("statusCode" to statusCode, "message" to message)
    }
}
