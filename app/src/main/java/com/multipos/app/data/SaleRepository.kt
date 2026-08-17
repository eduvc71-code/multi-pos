package com.multipos.app.data

import androidx.room.withTransaction
import com.multipos.app.data.entities.DetalleVenta
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.MovimientoInventario
import com.multipos.app.data.entities.Venta

data class SaleLineSnapshot(
    val productId: Int,
    val quantity: Int,
    val unitPrice: Long
)

data class RegisterSaleRequest(
    val paymentType: String,
    val total: Long,
    val subtotal: Long,
    val discount: Long,
    val tax: Long,
    val clientId: Int?,
    val credentialId: String?,
    val pin: String?,
    val userId: Int,
    val companyId: String,
    val lines: List<SaleLineSnapshot>
)

sealed class SaleRegistrationException(message: String) : IllegalStateException(message) {
    class InvalidCredential : SaleRegistrationException("Credencial inválida")
    class CreditLimitExceeded : SaleRegistrationException("Límite de crédito excedido")
    class InsufficientStock : SaleRegistrationException("Stock insuficiente")
    class CredentialBlocked : SaleRegistrationException("Credencial bloqueada")
    class CredentialExpired : SaleRegistrationException("Credencial vencida")
    class IncorrectPin : SaleRegistrationException("PIN incorrecto")
    class CreditInactive : SaleRegistrationException("Crédito inactivo")
    class NoActiveCashSession : SaleRegistrationException("No hay caja abierta")
    class InvalidLines : SaleRegistrationException("La venta debe tener al menos una línea de producto")
    class InvalidQuantity : SaleRegistrationException("Las cantidades deben ser mayores a cero")
    class InvalidProduct : SaleRegistrationException("Identificador de producto inválido")
    class InvalidUnitPrice : SaleRegistrationException("El precio unitario no puede ser negativo")
    class InvalidTotals : SaleRegistrationException("Los importes de la venta no son coherentes")
    class InvalidDiscount : SaleRegistrationException("El descuento no puede ser negativo ni superar el subtotal")
    class Overflow : SaleRegistrationException("Desbordamiento al calcular los importes de la venta")
    class NegativeTotal : SaleRegistrationException("El total de la venta no puede ser negativo")
}

class SaleRepository(private val database: AppDatabase) {
    suspend fun register(request: RegisterSaleRequest): Int = database.withTransaction {
        validateSaleRequest(request)

        val cashSession = database.cajaSesionDao().getActiveSessionForCompany(request.companyId)
            ?: throw SaleRegistrationException.NoActiveCashSession()

        var creditClientId: Int? = null

        if (request.paymentType == PAYMENT_CREDIT) {
            val clientId = request.clientId ?: throw SaleRegistrationException.InvalidCredential()
            val credentialId = request.credentialId ?: throw SaleRegistrationException.InvalidCredential()
            val pin = request.pin ?: throw SaleRegistrationException.InvalidCredential()
            val credentialRepo = CredentialRepository(database)
            credentialRepo.validateForCreditSale(
                companyId = request.companyId,
                credentialId = credentialId,
                pin = pin
            )
            if (database.clienteDao().increaseCredit(clientId, request.total, request.companyId) == 0) {
                throw SaleRegistrationException.CreditLimitExceeded()
            }
            creditClientId = clientId
        }

        val saleId = database.ventaDao().insert(
            Venta(
                tipoPago = request.paymentType,
                total = request.total,
                subtotal = request.subtotal,
                descuento = request.discount,
                impuesto = request.tax,
                idCliente = request.clientId,
                idUsuario = request.userId,
                empresaId = request.companyId,
                cajaSesionId = cashSession.id
            )
        ).toInt()

        val details = request.lines.map { line ->
            val product = database.productoDao()
                .getByIdIncludingInactive(line.productId, request.companyId)
                ?: throw SaleRegistrationException.InsufficientStock()
            if (
                database.productoDao().decreaseStock(
                    line.productId,
                    line.quantity,
                    request.companyId
                ) == 0
            ) {
                throw SaleRegistrationException.InsufficientStock()
            }
            database.movimientoInventarioDao().insert(
                MovimientoInventario(
                    empresaId = request.companyId,
                    productoId = line.productId,
                    usuarioId = request.userId,
                    tipo = MovimientoInventario.TIPO_VENTA,
                    cantidadFirmada = -line.quantity,
                    stockAnterior = product.stock,
                    stockPosterior = product.stock - line.quantity,
                    ventaId = saleId,
                    motivo = "Venta #$saleId",
                    fecha = System.currentTimeMillis()
                )
            )
            DetalleVenta(
                idVenta = saleId,
                idProducto = line.productId,
                cantidad = line.quantity,
                precioUnitario = line.unitPrice,
                subtotal = Math.multiplyExact(line.quantity.toLong(), line.unitPrice),
                costoUnitario = product.costoUnitario,
                nombreProductoSnapshot = product.nombre,
                empresaId = request.companyId
            )
        }
        database.ventaDao().insertDetalles(details)
        val now = System.currentTimeMillis()
        database.auditoriaDao().insert(
            com.multipos.app.data.entities.Auditoria(
                empresaId = request.companyId,
                usuarioId = request.userId,
                accion = com.multipos.app.data.entities.Auditoria.ACCION_VENTA,
                entidad = "venta",
                entidadId = saleId.toString(),
                detalle = "venta_registrada tipoPago=${request.paymentType} total=${request.total}",
                fecha = now
            )
        )

        if (request.paymentType == PAYMENT_EFFECTIVE) {
            database.movimientoCajaDao().insert(
                MovimientoCaja(
                    cajaSesionId = cashSession.id,
                    empresaId = request.companyId,
                    usuarioId = request.userId,
                    tipo = MovimientoCaja.TIPO_INGRESO_VENTA,
                    monto = request.total,
                    ventaId = saleId,
                    concepto = "Venta #${saleId}",
                    fecha = now
                )
            )
        }

        if (request.paymentType == PAYMENT_CREDIT && creditClientId != null) {
            CreditRepository(database).postCreditSale(
                companyId = request.companyId,
                clientId = creditClientId,
                userId = request.userId,
                monto = request.total,
                ventaId = saleId,
                fecha = now
            )
        }

        saleId
    }

    private fun validateSaleRequest(request: RegisterSaleRequest) {
        if (request.lines.isEmpty()) throw SaleRegistrationException.InvalidLines()
        if (request.lines.any { it.quantity <= 0 }) {
            throw SaleRegistrationException.InvalidQuantity()
        }
        if (request.lines.any { it.productId <= 0 }) {
            throw SaleRegistrationException.InvalidProduct()
        }
        if (request.lines.any { it.unitPrice < 0 }) {
            throw SaleRegistrationException.InvalidUnitPrice()
        }
        if (request.subtotal <= 0) throw SaleRegistrationException.InvalidTotals()
        if (request.discount < 0 || request.discount > request.subtotal) {
            throw SaleRegistrationException.InvalidDiscount()
        }
        if (request.tax < 0) throw SaleRegistrationException.InvalidTotals()
        if (request.total < 0) throw SaleRegistrationException.NegativeTotal()
        val expectedTotal: Long = try {
            Math.addExact(Math.subtractExact(request.subtotal, request.discount), request.tax)
        } catch (_: ArithmeticException) {
            throw SaleRegistrationException.Overflow()
        }
        if (request.total != expectedTotal) {
            throw SaleRegistrationException.InvalidTotals()
        }
        request.lines.forEach { line ->
            try {
                Math.multiplyExact(line.quantity.toLong(), line.unitPrice)
            } catch (_: ArithmeticException) {
                throw SaleRegistrationException.Overflow()
            }
        }
    }

    private companion object {
        const val PAYMENT_CREDIT = "CREDITO"
        const val PAYMENT_EFFECTIVE = "EFECTIVO"
    }
}
