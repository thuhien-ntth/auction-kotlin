package com.auction.app.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Đơn vị tiền tệ cho phép chọn khi đăng sản phẩm (hardcode, khớp backend ProductService.SUPPORTED_CURRENCIES). */
enum class CurrencyOption(val code: String, val label: String) {
    VND("VND", "VND - Việt Nam Đồng"),
    USD("USD", "USD - Đô la Mỹ"),
    EUR("EUR", "EUR - Euro");

    companion object {
        fun of(code: String?): CurrencyOption = values().firstOrNull { it.code == code } ?: VND
    }
}

object FormatUtils {
    /**
     * Formats a BigDecimal price to a String with comma as thousands separator (tối đa 2 số lẻ).
     * Example: 1500000 -> 1,500,000 ; 99.5 -> 99.5
     */
    fun formatPrice(amount: BigDecimal): String {
        val nf = NumberFormat.getNumberInstance(Locale.US)
        nf.maximumFractionDigits = 2
        return nf.format(amount)
    }

    /**
     * Formats a price kèm đơn vị tiền tệ của sản phẩm.
     * VND: 1,500,000 VND | USD: $1,500 | EUR: €1,500
     */
    /** ISO-8601 (UTC, từ backend) -> "yyyy/MM/dd HH:mm" theo múi giờ của máy. Sai định dạng thì trả nguyên chuỗi. */
    fun formatDateTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.parse(iso))
        } catch (e: Exception) {
            iso
        }
    }

    fun formatCurrency(amount: BigDecimal, currency: String? = "VND"): String = when (CurrencyOption.of(currency)) {
        CurrencyOption.USD -> "\$${formatPrice(amount)}"
        CurrencyOption.EUR -> "€${formatPrice(amount)}"
        CurrencyOption.VND -> "${formatPrice(amount)} VND"
    }
}
