package com.auction.app.util

import java.math.BigDecimal

data class StepRange(
    val start: BigDecimal,
    val end: BigDecimal,
    val steps: List<BigDecimal>
)

object StepValueCalculator {

    // Bảng cấu hình chuẩn (theo USD)
    private val standardRanges = listOf(
        StepRange(
            BigDecimal("0"),
            BigDecimal("100"),
            listOf(BigDecimal("5"), BigDecimal("10"), BigDecimal("15"), BigDecimal("20"))
        ),
        StepRange(
            BigDecimal("101"),
            BigDecimal("500"),
            listOf(BigDecimal("50"), BigDecimal("100"), BigDecimal("150"), BigDecimal("200"))
        ),
        StepRange(
            BigDecimal("501"),
            BigDecimal("3000"),
            listOf(BigDecimal("500"), BigDecimal("1000"), BigDecimal("1500"), BigDecimal("2500"))
        ),
        StepRange(
            BigDecimal("3001"),
            BigDecimal("4000"),
            listOf(BigDecimal("2000"), BigDecimal("4000"))
        )
    )

    // Bảng cấu hình VND (khi currentPrice lớn)
    private val vndRanges = listOf(
        StepRange(
            BigDecimal("0"),
            BigDecimal("10000000"),
            listOf(BigDecimal("500000"), BigDecimal("1000000"), BigDecimal("2000000"), BigDecimal("5000000"))
        ),
        StepRange(
            BigDecimal("10000001"),
            BigDecimal("100000000"),
            listOf(BigDecimal("2000000"), BigDecimal("5000000"), BigDecimal("10000000"), BigDecimal("20000000"))
        ),
        StepRange(
            BigDecimal("100000001"),
            BigDecimal("1000000000"),
            listOf(BigDecimal("5000000"), BigDecimal("10000000"), BigDecimal("20000000"), BigDecimal("50000000"))
        )
    )

    // VND dùng bảng bước giá VND; USD/EUR dùng bảng chuẩn.
    fun getAvailableSteps(currentPrice: BigDecimal, currency: String = "VND"): List<BigDecimal> {
        val ranges = if (currency == "VND" && currentPrice > BigDecimal("100000")) vndRanges else standardRanges
        val range = ranges.firstOrNull { currentPrice >= it.start && currentPrice <= it.end }
            ?: if (currentPrice < ranges.first().start) ranges.first() else ranges.last()

        val s1 = range.steps.first()
        val remainder = currentPrice.remainder(s1)

        return if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            range.steps
        } else {
            range.steps.map { step ->
                step.subtract(remainder).coerceAtLeast(BigDecimal.ONE)
            }
        }
    }
}
