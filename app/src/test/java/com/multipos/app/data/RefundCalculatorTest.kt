package com.multipos.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefundCalculatorTest {

    @Test
    fun fullReturnRefundsExactlyTheSaleTotal() {
        val result = RefundCalculator.compute(
            subtotal = 10_000,
            discount = 1_000,
            tax = 900,
            refundSubtotal = 10_000
        )
        assertEquals(10_000L, result.refundSubtotal)
        assertEquals(1_000L, result.proratedDiscount)
        assertEquals(900L, result.proratedTax)
        assertEquals(9_900L, result.refundMonto)
    }

    @Test
    fun partialReturnProratesDiscountAndTaxCumulatively() {
        val first = RefundCalculator.compute(
            subtotal = 100_000,
            discount = 20_000,
            tax = 8_000,
            refundSubtotal = 30_000
        )
        assertEquals(6_000L, first.proratedDiscount)
        assertEquals(2_400L, first.proratedTax)
        assertEquals(26_400L, first.refundMonto)

        val second = RefundCalculator.compute(
            subtotal = 100_000,
            discount = 20_000,
            tax = 8_000,
            refundSubtotal = 20_000,
            previousRefundSubtotal = 30_000
        )
        assertEquals(4_000L, second.proratedDiscount)
        assertEquals(1_600L, second.proratedTax)
        assertEquals(17_600L, second.refundMonto)
    }

    @Test
    fun accumulatedRefundsNeverExceedOriginalTotal() {
        val subtotal = 99_999L
        val discount = 17_777L
        val tax = 5_555L
        val total = subtotal - discount + tax

        var previousSubtotal = 0L
        var accumulatedRefund = 0L
        var remaining = subtotal
        while (remaining > 0) {
            val step = minOf(remaining, 7L)
            val result = RefundCalculator.compute(
                subtotal = subtotal,
                discount = discount,
                tax = tax,
                refundSubtotal = step,
                previousRefundSubtotal = previousSubtotal
            )
            accumulatedRefund += result.refundMonto
            assertTrue(accumulatedRefund <= total)
            previousSubtotal += step
            remaining -= step
        }
        assertEquals(total, accumulatedRefund)
    }

    @Test
    fun remainingDiscountGoesToLastReturnLine() {
        val subtotal = 100L
        val discount = 1L
        val tax = 0L

        val first = RefundCalculator.compute(subtotal, discount, tax, refundSubtotal = 1L)
        val second = RefundCalculator.compute(
            subtotal, discount, tax,
            refundSubtotal = 1L,
            previousRefundSubtotal = 1L
        )
        val last = RefundCalculator.compute(
            subtotal, discount, tax,
            refundSubtotal = 98L,
            previousRefundSubtotal = 2L
        )
        assertEquals(0L, first.proratedDiscount)
        assertEquals(0L, second.proratedDiscount)
        assertEquals(1L, last.proratedDiscount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun cumulativeSubtotalCannotExceedSaleSubtotal() {
        RefundCalculator.compute(100L, 0L, 0L, refundSubtotal = 50L, previousRefundSubtotal = 60L)
    }

    @Test
    fun zeroDiscountAndTaxKeepsRefundEqualToSubtotal() {
        val result = RefundCalculator.compute(50_000L, 0L, 0L, refundSubtotal = 12_345L)
        assertEquals(12_345L, result.refundMonto)
        assertEquals(0L, result.proratedDiscount)
        assertEquals(0L, result.proratedTax)
    }
}
