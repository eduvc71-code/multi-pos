package com.multipos.app.data

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Cálculo determinista del monto de una devolución.
 *
 * El descuento y el impuesto de la venta se prorratean sobre el subtotal devuelto.
 * Se usa redondeo hacia abajo sobre el acumulado devuelto (no sobre cada línea),
 * de modo que la suma de todos los reembolsos parciales nunca supere el total
 * original y el remanente quede siempre asignado a la última devolución.
 */
object RefundCalculator {

    data class RefundComputation(
        val refundSubtotal: Long,
        val proratedDiscount: Long,
        val proratedTax: Long,
        val refundMonto: Long
    )

    fun compute(
        subtotal: Long,
        discount: Long,
        tax: Long,
        refundSubtotal: Long,
        previousRefundSubtotal: Long = 0L
    ): RefundComputation {
        require(subtotal > 0) { "El subtotal de la venta debe ser positivo" }
        require(refundSubtotal >= 0) { "El subtotal devuelto no puede ser negativo" }
        require(previousRefundSubtotal >= 0) { "El subtotal previamente devuelto no puede ser negativo" }
        val cumulativeRefundSubtotal = Math.addExact(previousRefundSubtotal, refundSubtotal)
        require(cumulativeRefundSubtotal <= subtotal) {
            "El acumulado devuelto no puede superar el subtotal de la venta"
        }

        val cumulativeDiscount = proportionalFloor(discount, cumulativeRefundSubtotal, subtotal)
        val cumulativeTax = proportionalFloor(tax, cumulativeRefundSubtotal, subtotal)
        val previousDiscount = proportionalFloor(discount, previousRefundSubtotal, subtotal)
        val previousTax = proportionalFloor(tax, previousRefundSubtotal, subtotal)

        val proratedDiscount = Math.subtractExact(cumulativeDiscount, previousDiscount)
        val proratedTax = Math.subtractExact(cumulativeTax, previousTax)
        val refundMonto = Math.addExact(
            Math.subtractExact(refundSubtotal, proratedDiscount),
            proratedTax
        )

        return RefundComputation(
            refundSubtotal = refundSubtotal,
            proratedDiscount = proratedDiscount,
            proratedTax = proratedTax,
            refundMonto = refundMonto
        )
    }

    private fun proportionalFloor(a: Long, b: Long, c: Long): Long {
        if (a == 0L || b == 0L) return 0L
        return BigDecimal.valueOf(a)
            .multiply(BigDecimal.valueOf(b))
            .divide(BigDecimal.valueOf(c), 0, RoundingMode.FLOOR)
            .longValueExact()
    }
}
