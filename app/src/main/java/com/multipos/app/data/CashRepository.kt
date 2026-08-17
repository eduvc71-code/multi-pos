package com.multipos.app.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.multipos.app.data.entities.Auditoria
import com.multipos.app.data.entities.CajaSesion
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.Usuario
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions

sealed class CashException(message: String) : IllegalStateException(message) {
    object NoActiveSession : CashException("No hay caja abierta")
    object SessionAlreadyOpen : CashException("Ya existe una caja abierta para esta empresa")
    object NotAuthorized : CashException("No tienes permiso para realizar esta operación de caja")
    object InvalidAmount : CashException("El monto debe ser mayor a cero")
    object InvalidNote : CashException("El concepto es obligatorio")
    object InsufficientCash : CashException("El retiro supera el efectivo disponible")
    object SessionAlreadyClosed : CashException("La caja ya está cerrada")
}

data class CashSessionWithBalance(
    val session: CajaSesion,
    val ingresos: Long,
    val egresos: Long,
    val expected: Long,
    val difference: Long
)

class CashRepository(private val database: AppDatabase) {

    suspend fun openSession(
        companyId: String,
        userId: Int,
        montoApertura: Long,
        now: Long = System.currentTimeMillis()
    ): CajaSesion = database.withTransaction {
        val role = activeRole(companyId, userId)
        if (!CompanyPermissions.allows(role, CompanyPermission.MANAGE_CASH)) {
            throw CashException.NotAuthorized
        }
        if (montoApertura < 0) {
            throw CashException.InvalidAmount
        }
        val existing = database.cajaSesionDao().getActiveSessionForCompany(companyId)
        if (existing != null) {
            throw CashException.SessionAlreadyOpen
        }
        val session = CajaSesion(
            empresaId = companyId,
            abiertaPorUsuarioId = userId,
            fechaApertura = now,
            montoApertura = montoApertura,
            estado = CajaSesion.ESTADO_ABIERTA
        )
        val sessionId = try {
            database.cajaSesionDao().insert(session)
        } catch (_: SQLiteConstraintException) {
            throw CashException.SessionAlreadyOpen
        }
        database.movimientoCajaDao().insert(
            MovimientoCaja(
                cajaSesionId = sessionId,
                empresaId = companyId,
                usuarioId = userId,
                tipo = MovimientoCaja.TIPO_APERTURA,
                monto = montoApertura,
                concepto = "Apertura de caja",
                fecha = now
            )
        )
        database.auditoriaDao().insert(
            Auditoria(
                empresaId = companyId,
                usuarioId = userId,
                accion = Auditoria.ACCION_CAJA_APERTURA,
                entidad = "caja",
                entidadId = sessionId.toString(),
                detalle = "apertura monto=${montoApertura}",
                fecha = now
            )
        )
        session.copy(id = sessionId)
    }

    suspend fun getActiveSession(companyId: String): CajaSesion? {
        return database.cajaSesionDao().getActiveSessionForCompany(companyId)
    }

    suspend fun closeSession(
        companyId: String,
        sessionId: Long,
        userId: Int,
        montoContado: Long,
        nota: String,
        now: Long = System.currentTimeMillis()
    ): CashSessionWithBalance = database.withTransaction {
        val session = database.cajaSesionDao().getById(sessionId, companyId)
            ?: throw CashException.SessionAlreadyClosed
        if (session.estado != CajaSesion.ESTADO_ABIERTA) {
            throw CashException.SessionAlreadyClosed
        }
        val role = activeRole(companyId, userId)
        if (!CompanyPermissions.allows(role, CompanyPermission.MANAGE_CASH)) {
            throw CashException.NotAuthorized
        }
        if (role == Usuario.ROL_CAJERO && session.abiertaPorUsuarioId != userId) {
            throw CashException.NotAuthorized
        }
        if (montoContado < 0) {
            throw CashException.InvalidAmount
        }
        val ingresos = database.movimientoCajaDao().totalIngresos(sessionId, companyId)
        val egresos = database.movimientoCajaDao().totalEgresos(sessionId, companyId)
        val expected = Math.subtractExact(Math.addExact(session.montoApertura, ingresos), egresos)
        val difference = Math.subtractExact(montoContado, expected)
        val trimmedNote = nota.trim()
        if (
            trimmedNote.length > 300 ||
            (difference != 0L && trimmedNote.length !in 5..300)
        ) {
            throw CashException.InvalidNote
        }
        val rows = database.cajaSesionDao().cerrarSesion(
            id = sessionId,
            empresaId = companyId,
            cerradaPor = userId,
            fechaCierre = now,
            montoEsperado = expected,
            montoContado = montoContado,
            diferencia = difference,
            nota = trimmedNote
        )
        if (rows == 0) {
            throw CashException.SessionAlreadyClosed
        }
        database.auditoriaDao().insert(
            Auditoria(
                empresaId = companyId,
                usuarioId = userId,
                accion = Auditoria.ACCION_CAJA_CIERRE,
                entidad = "caja",
                entidadId = sessionId.toString(),
                detalle = "cierre contado=${montoContado} esperado=${expected} diferencia=${difference}",
                fecha = now
            )
        )
        CashSessionWithBalance(
            session = session.copy(
                estado = CajaSesion.ESTADO_CERRADA,
                cerradaPorUsuarioId = userId,
                fechaCierre = now,
                montoEsperadoCierre = expected,
                montoContadoCierre = montoContado,
                diferenciaCierre = difference,
                notaCierre = trimmedNote
            ),
            ingresos = ingresos,
            egresos = egresos,
            expected = expected,
            difference = difference
        )
    }

    suspend fun registerManualMovement(
        companyId: String,
        sessionId: Long,
        userId: Int,
        tipo: String,
        monto: Long,
        concepto: String,
        now: Long = System.currentTimeMillis()
    ): MovimientoCaja = database.withTransaction {
        val role = activeRole(companyId, userId)
        if (role != Usuario.ROL_PROPIETARIO && role != Usuario.ROL_ADMINISTRADOR) {
            throw CashException.NotAuthorized
        }
        if (tipo != MovimientoCaja.TIPO_INGRESO_MANUAL && tipo != MovimientoCaja.TIPO_EGRESO_MANUAL) {
            throw CashException.NotAuthorized
        }
        if (monto <= 0) {
            throw CashException.InvalidAmount
        }
        val conceptoTrimmed = concepto.trim()
        if (conceptoTrimmed.length < 3 || conceptoTrimmed.length > 200) {
            throw CashException.InvalidNote
        }
        val session = database.cajaSesionDao().getById(sessionId, companyId)
            ?: throw CashException.NoActiveSession
        if (session.estado != CajaSesion.ESTADO_ABIERTA) {
            throw CashException.NoActiveSession
        }
        val movimiento = MovimientoCaja(
            cajaSesionId = sessionId,
            empresaId = companyId,
            usuarioId = userId,
            tipo = tipo,
            monto = monto,
            concepto = conceptoTrimmed,
            fecha = now
        )
        val movementId = database.movimientoCajaDao().insert(movimiento)
        val accion = if (tipo == MovimientoCaja.TIPO_INGRESO_MANUAL) {
            Auditoria.ACCION_INGRESO_MANUAL
        } else {
            Auditoria.ACCION_EGRESO_MANUAL
        }
        database.auditoriaDao().insert(
            Auditoria(
                empresaId = companyId,
                usuarioId = userId,
                accion = accion,
                entidad = "caja",
                entidadId = sessionId.toString(),
                detalle = "movimiento_manual tipo=${tipo} monto=${monto}",
                fecha = now
            )
        )
        movimiento.copy(id = movementId)
    }

    suspend fun getSessionWithBalance(companyId: String, sessionId: Long): CashSessionWithBalance? {
        val session = database.cajaSesionDao().getById(sessionId, companyId) ?: return null
        val ingresos = database.movimientoCajaDao().totalIngresos(sessionId, companyId)
        val egresos = database.movimientoCajaDao().totalEgresos(sessionId, companyId)
        val expected = Math.subtractExact(Math.addExact(session.montoApertura, ingresos), egresos)
        return CashSessionWithBalance(
            session = session,
            ingresos = ingresos,
            egresos = egresos,
            expected = expected,
            difference = (session.montoContadoCierre ?: 0L) - expected
        )
    }

    suspend fun getMovementsForSession(companyId: String, sessionId: Long): List<MovimientoCaja> {
        return database.movimientoCajaDao().getBySession(sessionId, companyId)
    }

    private suspend fun activeRole(companyId: String, userId: Int): String? =
        database.usuarioEmpresaDao().getActiveMembership(userId, companyId)?.rol
}
