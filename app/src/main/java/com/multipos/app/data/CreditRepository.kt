package com.multipos.app.data

import androidx.room.withTransaction
import com.multipos.app.data.entities.Abono
import com.multipos.app.data.entities.Auditoria
import com.multipos.app.data.entities.CajaSesion
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.MovimientoCredito
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions

sealed class CreditException(message: String) : IllegalStateException(message) {
    object NotAuthorized : CreditException("No tienes permiso para administrar créditos")
    object InvalidAmount : CreditException("El monto debe ser mayor a cero")
    object InvalidMedioPago : CreditException("Medio de pago inválido")
    object PaymentNotFound : CreditException("El cliente no existe")
    object ClientNotAllowed : CreditException("El cliente no está activo o no puede recibir abonos")
    object InvalidNote : CreditException("La nota no puede superar 300 caracteres")
    object CreditDebtNotEnough : CreditException("El monto supera el saldo pendiente")
    object NoActiveCashSession : CreditException("No hay caja abierta para el ingreso de abono")
    object InvalidCashSession : CreditException("La caja indicada no está abierta o no pertenece a la empresa")
    object ExternalPaymentNotConfirmed : CreditException("Debe confirmar el cobro externo")
    object InconsistentLedger : CreditException("El libro de crédito no cuadra con el saldo del cliente")
}

data class RegisterAbonoRequest(
    val companyId: String,
    val clientId: Int,
    val userId: Int,
    val monto: Long,
    val medioPago: String,
    val cajaSesionId: Long? = null,
    val nota: String = "",
    val externalPaymentConfirmed: Boolean = false
)

data class AbonoResult(
    val abonoId: Long,
    val saldoAnterior: Long,
    val saldoNuevo: Long
)

class CreditRepository(private val database: AppDatabase) {

    suspend fun registerAbono(request: RegisterAbonoRequest): AbonoResult = database.withTransaction {
        val role = activeRole(request.companyId, request.userId)
        if (!CompanyPermissions.allows(role, CompanyPermission.MANAGE_CLIENT_CREDIT)) {
            throw CreditException.NotAuthorized
        }
        if (request.medioPago !in ALLOWED_MEDIO_PAGO) {
            throw CreditException.InvalidMedioPago
        }
        if (request.nota.length > MAX_NOTA_LENGTH) {
            throw CreditException.InvalidNote
        }
        if (request.monto <= 0) {
            throw CreditException.InvalidAmount
        }
        val client = database.clienteDao()
            .getByIdIncludingInactive(request.clientId, request.companyId)
            ?: throw CreditException.PaymentNotFound
        if (!client.activo || (client.estadoCredito != Cliente.ESTADO_ACTIVO && client.estadoCredito != Cliente.ESTADO_SUSPENDIDO)) {
            throw CreditException.ClientNotAllowed
        }
        val saldoAnterior = client.creditoActual
        if (request.medioPago != Abono.MEDIO_EFECTIVO) {
            if (!request.externalPaymentConfirmed) {
                throw CreditException.ExternalPaymentNotConfirmed
            }
        }
        val cashSessionId = if (request.medioPago == Abono.MEDIO_EFECTIVO) {
            val session = request.cajaSesionId?.let { provided ->
                database.cajaSesionDao().getById(provided, request.companyId)
                    ?: throw CreditException.InvalidCashSession
            } ?: database.cajaSesionDao()
                .getActiveSessionForCompany(request.companyId)
                ?: throw CreditException.NoActiveCashSession
            if (session.estado != CajaSesion.ESTADO_ABIERTA) {
                throw CreditException.InvalidCashSession
            }
            session.id
        } else {
            null
        }

        if (
            database.clienteDao().decreaseCredit(
                request.clientId,
                request.monto,
                request.companyId
            ) == 0
        ) {
            throw CreditException.CreditDebtNotEnough
        }

        val now = System.currentTimeMillis()
        val abonoId = database.abonoDao().insert(
            Abono(
                empresaId = request.companyId,
                idCliente = request.clientId,
                usuarioId = request.userId,
                cajaSesionId = cashSessionId,
                monto = request.monto,
                medioPago = request.medioPago,
                nota = request.nota,
                fecha = now
            )
        )
        postLedger(
            companyId = request.companyId,
            clientId = request.clientId,
            userId = request.userId,
            tipo = MovimientoCredito.TIPO_ABONO,
            importe = -request.monto,
            abonoId = abonoId,
            fecha = now,
            nota = request.nota
        )

        if (request.medioPago == Abono.MEDIO_EFECTIVO) {
            database.movimientoCajaDao().insert(
                MovimientoCaja(
                    cajaSesionId = cashSessionId!!,
                    empresaId = request.companyId,
                    usuarioId = request.userId,
                    tipo = MovimientoCaja.TIPO_INGRESO_ABONO,
                    monto = request.monto,
                    abonoId = abonoId,
                    concepto = "Abono de cliente ${client.nombre}",
                    fecha = now
                )
            )
        }

        database.auditoriaDao().insert(
            Auditoria(
                empresaId = request.companyId,
                usuarioId = request.userId,
                accion = Auditoria.ACCION_ABONO,
                entidad = "abono",
                entidadId = abonoId.toString(),
                detalle = "abono cliente=${request.clientId} monto=${request.monto} medio=${request.medioPago}",
                fecha = now
            )
        )

        reconcile(request.companyId, request.clientId)
        AbonoResult(
            abonoId = abonoId,
            saldoAnterior = saldoAnterior,
            saldoNuevo = saldoAnterior - request.monto
        )
    }

    suspend fun postCreditSale(
        companyId: String,
        clientId: Int,
        userId: Int,
        monto: Long,
        ventaId: Int,
        fecha: Long = System.currentTimeMillis()
    ) {
        postLedger(
            companyId = companyId,
            clientId = clientId,
            userId = userId,
            tipo = MovimientoCredito.TIPO_VENTA_CREDITO,
            importe = monto,
            ventaId = ventaId,
            fecha = fecha,
            nota = "Venta a crédito #$ventaId"
        )
        reconcile(companyId, clientId)
    }

    suspend fun postCreditAnnulment(
        companyId: String,
        clientId: Int,
        userId: Int,
        monto: Long,
        ventaId: Int,
        fecha: Long = System.currentTimeMillis()
    ) {
        postLedger(
            companyId = companyId,
            clientId = clientId,
            userId = userId,
            tipo = MovimientoCredito.TIPO_ANULACION,
            importe = -monto,
            ventaId = ventaId,
            fecha = fecha,
            nota = "Anulación de venta a crédito #$ventaId"
        )
        reconcile(companyId, clientId)
    }

    suspend fun postCreditRefund(
        companyId: String,
        clientId: Int,
        userId: Int,
        monto: Long,
        ventaId: Int,
        devolucionId: Long,
        fecha: Long = System.currentTimeMillis()
    ) {
        postLedger(
            companyId = companyId,
            clientId = clientId,
            userId = userId,
            tipo = MovimientoCredito.TIPO_DEVOLUCION,
            importe = -monto,
            ventaId = ventaId,
            devolucionId = devolucionId,
            fecha = fecha,
            nota = "Devolución de venta a crédito #$ventaId"
        )
        reconcile(companyId, clientId)
    }

    suspend fun estadoDeCuenta(
        companyId: String,
        clientId: Int,
        userId: Int,
        desde: Long? = null,
        hastaExclusive: Long? = null
    ): List<MovimientoCredito> {
        // Consultar el estado de cuenta solo exige pertenecer a la empresa; el alta
        // de abonos sigue restringida por MANAGE_CLIENT_CREDIT en registerAbono.
        if (activeRole(companyId, userId) == null) {
            throw CreditException.NotAuthorized
        }
        return if (desde != null && hastaExclusive != null) {
            database.movimientoCreditoDao().getByClientBetween(companyId, clientId, desde, hastaExclusive)
        } else {
            database.movimientoCreditoDao().getByClient(companyId, clientId)
        }
    }

    private suspend fun postLedger(
        companyId: String,
        clientId: Int,
        userId: Int,
        tipo: String,
        importe: Long,
        ventaId: Int? = null,
        abonoId: Long? = null,
        devolucionId: Long? = null,
        fecha: Long,
        nota: String
    ) {
        val saldoPosterior = database.clienteDao()
            .getByIdIncludingInactive(clientId, companyId)
            ?.creditoActual
            ?: throw CreditException.PaymentNotFound
        database.movimientoCreditoDao().insert(
            MovimientoCredito(
                empresaId = companyId,
                clienteId = clientId,
                usuarioId = userId,
                tipo = tipo,
                importeFirmado = importe,
                saldoPosterior = saldoPosterior,
                ventaId = ventaId,
                abonoId = abonoId,
                devolucionId = devolucionId,
                fecha = fecha,
                nota = nota
            )
        )
    }

    private suspend fun reconcile(companyId: String, clientId: Int) {
        val ledged = database.movimientoCreditoDao().sumImporteFirmado(companyId, clientId)
        val actual = database.clienteDao()
            .getByIdIncludingInactive(clientId, companyId)
            ?.creditoActual
            ?: throw CreditException.PaymentNotFound
        if (ledged != actual) {
            throw CreditException.InconsistentLedger
        }
    }

    private suspend fun activeRole(companyId: String, userId: Int): String? =
        database.usuarioEmpresaDao().getActiveMembership(userId, companyId)?.rol

    private companion object {
        val ALLOWED_MEDIO_PAGO = setOf(Abono.MEDIO_EFECTIVO, Abono.MEDIO_TARJETA, Abono.MEDIO_TRANSFERENCIA)
        const val MAX_NOTA_LENGTH = 300
    }
}
