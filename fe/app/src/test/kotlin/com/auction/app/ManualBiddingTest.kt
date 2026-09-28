package com.auction.app

import com.auction.app.util.StepValueCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ManualBiddingTest {

    @Test
    fun testStandardStepValue_WhenCurrentPriceIsMultipleOfMinStep() {
        // Current Price = 40 (thuộc khoảng $0-$100, S1 = 5)
        // 40 là bội số của 5 -> Hiển thị Standard Step Value: 5, 10, 15, 20
        val steps40 = StepValueCalculator.getAvailableSteps(BigDecimal("40"))
        assertEquals(listOf(BigDecimal("5"), BigDecimal("10"), BigDecimal("15"), BigDecimal("20")), steps40)

        // Current Price = 100
        val steps100 = StepValueCalculator.getAvailableSteps(BigDecimal("100"))
        assertEquals(listOf(BigDecimal("5"), BigDecimal("10"), BigDecimal("15"), BigDecimal("20")), steps100)

        // Current Price = 200 (thuộc khoảng $101-$500, S1 = 50)
        // 200 là bội số của 50 -> Hiển thị Standard Step Value: 50, 100, 150, 200
        val steps200 = StepValueCalculator.getAvailableSteps(BigDecimal("200"))
        assertEquals(listOf(BigDecimal("50"), BigDecimal("100"), BigDecimal("150"), BigDecimal("200")), steps200)

        // Current Price = 1500 (thuộc khoảng $501-$3000, S1 = 500)
        // 1500 là bội số của 500 -> Hiển thị Standard Step Value: 500, 1000, 1500, 2500
        val steps1500 = StepValueCalculator.getAvailableSteps(BigDecimal("1500"))
        assertEquals(listOf(BigDecimal("500"), BigDecimal("1000"), BigDecimal("1500"), BigDecimal("2500")), steps1500)
    }

    @Test
    fun testTemporaryStepValue_WhenCurrentPriceHasRemainder() {
        // Case spec: Current price = 105 (khoảng $101-$500, standard steps: 50, 100, 150, 200)
        // S1 = 50
        // Phần lẻ = 105 % 50 = 5
        // Temporary steps = Step - 5: 45, 95, 145, 195
        val steps105 = StepValueCalculator.getAvailableSteps(BigDecimal("105"))
        assertEquals(listOf(BigDecimal("45"), BigDecimal("95"), BigDecimal("145"), BigDecimal("195")), steps105)

        // Case spec: Current price = 550 (khoảng $501-$3000, standard steps: 500, 1000, 1500, 2500)
        // S1 = 500
        // Phần lẻ = 550 % 500 = 50
        // Temporary steps = Step - 50: 450, 950, 1450, 2450
        val steps550 = StepValueCalculator.getAvailableSteps(BigDecimal("550"))
        assertEquals(listOf(BigDecimal("450"), BigDecimal("950"), BigDecimal("1450"), BigDecimal("2450")), steps550)
    }

    @Test
    fun testTargetBidPriceCalculation() {
        // Kiểm tra phép tính giá dự thầu khi click step:
        // Giá dự thầu = Giá hiện tại + Step Value
        val currentPrice = BigDecimal("105")
        val availableSteps = StepValueCalculator.getAvailableSteps(currentPrice)
        val minStep = availableSteps.first() // 45
        val targetBidPrice = currentPrice.add(minStep) // 105 + 45 = 150 (làm tròn đẹp số tiền)

        assertEquals(BigDecimal("150"), targetBidPrice)
    }
}
