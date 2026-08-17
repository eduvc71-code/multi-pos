package com.multipos.app.data

import androidx.room.withTransaction
import com.multipos.app.data.entities.Auditoria
import com.multipos.app.data.entities.DetalleDevolucion
import com.multipos.app.data.entities.Devolucion
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.MovimientoInventario
import com.multipos.app.data.entities.Venta
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions
import java.util.Calendar

sealed class ReturnException(message: String) : IllegalStateException(message) {
    object NotAuthorized : ReturnException("No tienes permiso para realizar esta operación")
    object SaleNotFound : ReturnException("Venta no encontrada")
    object SaleNotCompleted : ReturnException("Solo se pueden anular o devolver ventas completadas")
    object SaleNotToday : ReturnException("Solo se puede anular una venta del mismo día")
    object PriorRefundExists : ReturnException("La venta ya tiene devoluciones registradas")
    object InvalidReason : ReturnException("El motivo debe tener entre 5 y 300 caracteres")
    object NoActiveCashSession : ReturnException("No hay caja abierta para registrar la reversión")
    object ExternalRefundNotConfirmed : ReturnException("Debe confirmar el reembolso externo")
    object InvalidQuantity : ReturnException("La cantidad a devolver debe ser mayor a cero")
    object RefundExceedsSoldQuantity : ReturnException("La devolución supera la cantidad vendida")
    object InsufficientStock : ReturnException("No se pudo reponer el stock")
    object CreditDebtNotEnough : ReturnException("La deuda del cliente no cubre el importe a reversar")
    object InconsistentTotals : ReturnException("Los totales de la venta no son coherentes")
}

data class AnnulSaleRequest(
    val companyId: String,
    val saleId: Int,
    val userId: Int,
    val motivo: String,
    val externalRefundConfirmed: Boolean
)

data class AnnulSaleResult(
    val saleId: Int,
    val restoredProductCount: Int,
    val cashMovementId: Long?
)

data class RefundLineRequest(
    val detailId: Int,
    val quantity: Int
)

data class RefundSaleRequest(
    val companyId: String,
    val saleId: Int,
    val userId: Int,
    val motivo: String,
    val externalRefundConfirmed: Boolean,
    val lines: List<RefundLineRequest>
)

data class RefundSaleResult(
    val saleId: Int,
    val refundId: Long,
    val refundMonto: Long,
    val restoredProductCount: Int
)

class ReturnRepository(
    private val database: AppDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun annulSale(request: AnnulSaleRequest): AnnulSaleResult = database.withTransaction {
        val role = activeRole(request.companyId, request.userId)
        if (!CompanyPermissions.allows(role, CompanyPermission.MANAGE_RETURNS)) {
            throw ReturnException.NotAuthorized
        }
        val sale = database.ventaDao().getById(request.saleId, request.companyId)
            ?: throw ReturnException.SaleNotFound
        if (sale.estado != Venta.ESTADO_COMPLETADA) {
            throw ReturnException.SaleNotCompleted
        }
        val motivo = request.motivo.trim()
        if (motivo.length !in 5..300) {
            throw ReturnException.InvalidReason
        }
        if (!isSameDay(sale.fecha)) {
            throw ReturnException.SaleNotToday
        }
        if (database.devolucionDao().getBySale(request.companyId, request.saleId).isNotEmpty()) {
            throw ReturnException.PriorRefundExists
        }
        val details = database.ventaDao().getDetails(request.saleId, request.companyId)
        if (details.isEmpty()) {
            throw ReturnException.InconsistentTotals
        }

        val cashSession = database.cajaSesionDao().getActiveSessionForCompany(request.companyId)
            ?: throw ReturnException.NoActiveCashSession
        if (sale.tipoPago == PAYMENT_CARD || sale.tipoPago == PAYMENT_TRANSFER) {
            if (!request.externalRefundConfirmed) {
                throw ReturnException.ExternalRefundNotConfirmed
            }
        }
        if (sale.tipoPago == PAYMENT_CREDIT && sale.idCliente == null) {
            throw ReturnException.SaleNotFound
        }

        var restoredProductCount = 0
        val now = System.currentTimeMillis()
        for (detail in details) {
            val product = database.productoDao()
                .getByIdIncludingInactive(detail.idProducto, request.companyId)
                ?: throw ReturnException.InsufficientStock
            val stockAnterior = product.stock
            if (
                database.productoDao().increaseStock(
                    detail.idProducto,
                    detail.cantidad,
                    request.companyId
                ) == 0
            ) {
                throw ReturnException.InsufficientStock
            }
            database.movimientoInventarioDao().insert(
                MovimientoInventario(
                    empresaId = request.companyId,
                    productoId = detail.idProducto,
                    usuarioId = request.userId,
                    tipo = MovimientoInventario.TIPO_ANULACION,
                    cantidadFirmada = detail.cantidad,
                    stockAnterior = stockAnterior,
                    stockPosterior = stockAnterior + detail.cantidad,
                    ventaId = request.saleId,
                    motivo = "Anulación de venta #${request.saleId}",
                    fecha = now
                )
            )
            restoredProductCount++
        }

        val rows = database.ventaDao().anularVenta(
            ventaId = request.saleId,
            empresaId = request.companyId,
            usuarioId = request.userId,
            fecha = now,
            motivo = motivo
        )
        if (rows == 0) {
            throw ReturnException.SaleNotCompleted
        }

        var cashMovementId: Long? = null
        when (sale.tipoPago) {
            PAYMENT_EFFECTIVE -> {
                cashMovementId = database.movimientoCajaDao().insert(
                    MovimientoCaja(
                        cajaSesionId = cashSession.id,
                        empresaId = request.companyId,
                        usuarioId = request.userId,
                        tipo = MovimientoCaja.TIPO_REVERSO_ANULACION,
                        monto = sale.total,
                        ventaId = request.saleId,
                        concepto = "Anulación de venta #${request.saleId}",
                        fecha = now
                    )
                )
            }
            PAYMENT_CREDIT -> {
                val client = database.clienteDao()
                    .getByIdIncludingInactive(sale.idCliente!!, request.companyId)
                    ?: throw ReturnException.SaleNotFound
                if (client.creditoActual < sale.total) {
                    throw ReturnException.CreditDebtNotEnough
                }
                if (
                    database.clienteDao().decreaseCredit(
                        sale.idCliente!!,
                        sale.total,
                        request.companyId
                    ) == 0
                ) {
                    throw ReturnException.CreditDebtNotEnough
                }
                CreditRepository(database).postCreditAnnulment(
                    companyId = request.companyId,
                    clientId = sale.idCliente!!,
                    userId = request.userId,
                    monto = sale.total,
                    ventaId = request.saleId,
                    fecha = now
                )
            }
        }

        database.auditoriaDao().insert(
            Auditoria(
                empresaId = request.companyId,
                usuarioId = request.userId,
                accion = Auditoria.ACCION_ANULACION,
                entidad = "venta",
                entidadId = request.saleId.toString(),
                detalle = "anulacion venta=${request.saleId} tipoPago=${sale.tipoPago} total=${sale.total}",
                fecha = now
            )
        )

        AnnulSaleResult(
            saleId = request.saleId,
            restoredProductCount = restoredProductCount,
            cashMovementId = cashMovementId
        )
    }

    suspend fun refundSale(request: RefundSaleRequest): RefundSaleResult = database.withTransaction {
        val role = activeRole(request.companyId, request.userId)
        if (!CompanyPermissions.allows(role, CompanyPermission.MANAGE_RETURNS)) {
            throw ReturnException.NotAuthorized
        }
        val sale = database.ventaDao().getById(request.saleId, request.companyId)
            ?: throw ReturnException.SaleNotFound
        if (sale.estado != Venta.ESTADO_COMPLETADA) {
            throw ReturnException.SaleNotCompleted
        }
        val motivo = request.motivo.trim()
        if (motivo.length !in 5..300) {
            throw ReturnException.InvalidReason
        }
        if (request.lines.isEmpty() || request.lines.any { it.quantity <= 0 }) {
            throw ReturnException.InvalidQuantity
        }
        val details = database.ventaDao().getDetails(request.saleId, request.companyId)
        if (details.isEmpty()) {
            throw ReturnException.InconsistentTotals
        }

        val quantitiesByDetail = request.lines
            .groupBy { it.detailId }
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
        val refundDetailsBySaleDetail = LinkedHashMap<Int, Pair<com.multipos.app.data.entities.DetalleVenta, Int>>()
        var refundSubtotal = 0L
        for ((detailId, quantity) in quantitiesByDetail) {
            val detail = details.firstOrNull { it.id == detailId }
                ?: throw ReturnException.InvalidQuantity
            val alreadyReturned = database.devolucionDao()
                .returnedQuantity(request.companyId, request.saleId, detailId)
            if (Math.addExact(alreadyReturned, quantity) > detail.cantidad) {
                throw ReturnException.RefundExceedsSoldQuantity
            }
            refundDetailsBySaleDetail[detailId] = detail to quantity
            refundSubtotal = Math.addExact(
                refundSubtotal,
                Math.multiplyExact(quantity.toLong(), detail.precioUnitario)
            )
        }

        var previousRefundSubtotal = 0L
        var previousRefundMonto = 0L
        for (previous in database.devolucionDao().getBySale(request.companyId, request.saleId)) {
            previousRefundMonto = Math.addExact(previousRefundMonto, previous.monto)
            for (previousDetail in database.devolucionDao().getDetails(previous.id)) {
                previousRefundSubtotal = Math.addExact(previousRefundSubtotal, previousDetail.subtotal)
            }
        }

        val computation = RefundCalculator.compute(
            subtotal = sale.subtotal,
            discount = sale.descuento,
            tax = sale.impuesto,
            refundSubtotal = refundSubtotal,
            previousRefundSubtotal = previousRefundSubtotal
        )
        val refundMonto = computation.refundMonto
        if (refundMonto > Math.subtractExact(sale.total, previousRefundMonto)) {
            throw ReturnException.InconsistentTotals
        }

        val cashSession = if (sale.tipoPago == PAYMENT_EFFECTIVE) {
            database.cajaSesionDao().getActiveSessionForCompany(request.companyId)
                ?: throw ReturnException.NoActiveCashSession
        } else {
            null
        }
        val medioReembolso: String
        val estadoReembolso: String
        when (sale.tipoPago) {
            PAYMENT_EFFECTIVE -> {
                medioReembolso = PAYMENT_EFFECTIVE
                estadoReembolso = Devolucion.ESTADO_COMPLETADO
            }
            PAYMENT_CARD, PAYMENT_TRANSFER -> {
                if (!request.externalRefundConfirmed) {
                    throw ReturnException.ExternalRefundNotConfirmed
                }
                medioReembolso = sale.tipoPago
                estadoReembolso = Devolucion.ESTADO_CONFIRMADO_EXTERNAMENTE
            }
            PAYMENT_CREDIT -> {
                if (sale.idCliente == null) {
                    throw ReturnException.SaleNotFound
                }
                val client = database.clienteDao()
                    .getByIdIncludingInactive(sale.idCliente!!, request.companyId)
                    ?: throw ReturnException.SaleNotFound
                if (client.creditoActual < refundMonto) {
                    throw ReturnException.CreditDebtNotEnough
                }
                medioReembolso = PAYMENT_CREDIT
                estadoReembolso = Devolucion.ESTADO_COMPLETADO
            }
            else -> throw ReturnException.InconsistentTotals
        }

        val now = System.currentTimeMillis()
        val refundId = database.devolucionDao().insert(
            Devolucion(
                empresaId = request.companyId,
                ventaId = request.saleId,
                usuarioId = request.userId,
                cajaSesionId = cashSession?.id,
                monto = refundMonto,
                medioReembolso = medioReembolso,
                estadoReembolso = estadoReembolso,
                motivo = motivo,
                fecha = now
            )
        )

        var restoredProductCount = 0
        val refundDetails = mutableListOf<DetalleDevolucion>()
        for ((detail, quantity) in refundDetailsBySaleDetail.values) {
            val product = database.productoDao()
                .getByIdIncludingInactive(detail.idProducto, request.companyId)
                ?: throw ReturnException.InsufficientStock
            val stockAnterior = product.stock
            if (
                database.productoDao().increaseStock(
                    detail.idProducto,
                    quantity,
                    request.companyId
                ) == 0
            ) {
                throw ReturnException.InsufficientStock
            }
            database.movimientoInventarioDao().insert(
                MovimientoInventario(
                    empresaId = request.companyId,
                    productoId = detail.idProducto,
                    usuarioId = request.userId,
                    tipo = MovimientoInventario.TIPO_DEVOLUCION,
                    cantidadFirmada = quantity,
                    stockAnterior = stockAnterior,
                    stockPosterior = stockAnterior + quantity,
                    ventaId = request.saleId,
                    devolucionId = refundId,
                    motivo = "Devolución de venta #${request.saleId}",
                    fecha = now
                )
            )
            refundDetails.add(
                DetalleDevolucion(
                    devolucionId = refundId,
                    detalleVentaId = detail.id,
                    productoId = detail.idProducto,
                    cantidad = quantity,
                    precioUnitario = detail.precioUnitario,
                    subtotal = Math.multiplyExact(quantity.toLong(), detail.precioUnitario)
                )
            )
            restoredProductCount++
        }
        database.devolucionDao().insertDetails(refundDetails)

        when (sale.tipoPago) {
            PAYMENT_EFFECTIVE -> {
                database.movimientoCajaDao().insert(
                    MovimientoCaja(
                        cajaSesionId = cashSession!!.id,
                        empresaId = request.companyId,
                        usuarioId = request.userId,
                        tipo = MovimientoCaja.TIPO_EGRESO_DEVOLUCION,
                        monto = refundMonto,
                        ventaId = request.saleId,
                        devolucionId = refundId,
                        concepto = "Devolución de venta #${request.saleId}",
                        fecha = now
                    )
                )
            }
            PAYMENT_CREDIT -> {
                if (
                    database.clienteDao().decreaseCredit(
                        sale.idCliente!!,
                        refundMonto,
                        request.companyId
                    ) == 0
                ) {
                    throw ReturnException.CreditDebtNotEnough
                }
                CreditRepository(database).postCreditRefund(
                    companyId = request.companyId,
                    clientId = sale.idCliente!!,
                    userId = request.userId,
                    monto = refundMonto,
                    ventaId = request.saleId,
                    devolucionId = refundId,
                    fecha = now
                )
            }
        }

        database.auditoriaDao().insert(
            Auditoria(
                empresaId = request.companyId,
                usuarioId = request.userId,
                accion = Auditoria.ACCION_DEVOLUCION,
                entidad = "devolucion",
                entidadId = refundId.toString(),
                detalle = "devolucion venta=${request.saleId} monto=$refundMonto medio=$medioReembolso estado=$estadoReembolso",
                fecha = now
            )
        )

        RefundSaleResult(
            saleId = request.saleId,
            refundId = refundId,
            refundMonto = refundMonto,
            restoredProductCount = restoredProductCount
        )
    }

    private suspend fun activeRole(companyId: String, userId: Int): String? =
        database.usuarioEmpresaDao().getActiveMembership(userId, companyId)?.rol

    private fun isSameDay(saleFecha: Long): Boolean {
        val (inicio, fin) = dayBoundaries(clock())
        return saleFecha >= inicio && saleFecha < fin
    }

    private fun dayBoundaries(now: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val inicio = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return inicio to cal.timeInMillis
    }

    private companion object {
        const val PAYMENT_EFFECTIVE = "EFECTIVO"
        const val PAYMENT_CARD = "TARJETA"
        const val PAYMENT_TRANSFER = "TRANSFERENCIA"
        const val PAYMENT_CREDIT = "CREDITO"
    }
}
