package com.auction.app.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
enum class CurrencyOption(val code: String, val label: String) {
    VND("VND", "VND - Việt Nam Đồng"),
    USD("USD", "USD - Đô la Mỹ"),
    EUR("EUR", "EUR - Euro");

    companion object {
        fun of(code: String?): CurrencyOption = values().firstOrNull { it.code == code } ?: VND
    }
}

object FormatUtils {
    fun formatPrice(amount: BigDecimal): String {
        val nf = NumberFormat.getNumberInstance(Locale.US)
        nf.maximumFractionDigits = 2
        return nf.format(amount)
    }

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
