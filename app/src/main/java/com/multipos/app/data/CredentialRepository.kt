package com.multipos.app.data

import androidx.room.withTransaction
import com.multipos.app.data.entities.Auditoria
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.CredencialCliente
import com.multipos.app.security.PinHasher
import java.util.concurrent.TimeUnit

sealed class CredentialException(message: String) : IllegalStateException(message) {
    object InvalidFormat : CredentialException("Formato QR inválido")
    object WrongCompany : CredentialException("Credencial de otra empresa")
    object RevokedOrReplaced : CredentialException("Credencial revocada o reemplazada")
    object Expired : CredentialException("Credencial vencida")
    object Blocked : CredentialException("Credencial bloqueada")
    object IncorrectPin : CredentialException("PIN incorrecto")
    object ClientInactive : CredentialException("Cliente inactivo")
    object CreditInactive : CredentialException("Crédito inactivo")
    object LimitExceeded : CredentialException("Límite de crédito insuficiente")
}

data class CredentialValidationResult(
    val credential: CredencialCliente,
    val client: Cliente
)

class CredentialRepository(private val database: AppDatabase) {

    suspend fun validateForCreditSale(
        companyId: String,
        credentialId: String,
        pin: String
    ): CredentialValidationResult = database.withTransaction {
        val now = System.currentTimeMillis()
        val credential = database.credencialClienteDao().getForCredentialId(companyId, credentialId)
            ?: throw CredentialException.WrongCompany
        val client = database.clienteDao().getByIdIncludingInactive(credential.clienteId, companyId)
            ?: throw CredentialException.WrongCompany
        when (credential.estado) {
            CredencialCliente.ESTADO_REVOCADA -> throw CredentialException.RevokedOrReplaced
            CredencialCliente.ESTADO_REEMPLAZADA -> throw CredentialException.RevokedOrReplaced
        }
        if (credential.fechaVencimiento != null && now > credential.fechaVencimiento) {
            throw CredentialException.Expired
        }
        if (credential.bloqueadaHasta != null && now < credential.bloqueadaHasta) {
            val remaining = credential.bloqueadaHasta - now
            throw CredentialBlockedException(remaining)
        }
        if (!client.activo) throw CredentialException.ClientInactive
        if (!client.creditoHabilitado || client.estadoCredito != Cliente.ESTADO_ACTIVO) {
            throw CredentialException.CreditInactive
        }
        val pinHash = credential.pinHash
        val pinSalt = credential.pinSalt
        if (pinHash == null || pinSalt == null) {
            throw CredentialException.RevokedOrReplaced
        }
        if (pin.length != CredencialCliente.PIN_LENGTH || !pin.all { it.isDigit() }) {
            database.credencialClienteDao().incrementPinAttempts(credential.id, companyId)
            throw CredentialException.IncorrectPin
        }
        if (!PinHasher.verify(pin.toCharArray(), pinHash, pinSalt)) {
            database.credencialClienteDao().incrementPinAttempts(credential.id, companyId)
            val updatedCredential = database.credencialClienteDao().getById(credential.id, companyId)
            if (updatedCredential != null && updatedCredential.intentosFallidos >= CredencialCliente.MAX_PIN_ATTEMPTS) {
                database.credencialClienteDao().lockCredential(
                    credential.id,
                    companyId,
                    now + CredencialCliente.DURATION_LOCKOUT_MS
                )
            }
            throw CredentialException.IncorrectPin
        }
        database.credencialClienteDao().resetPinAttempts(credential.id, companyId)
        database.credencialClienteDao().updateUltimoUso(credential.id, companyId, now)
        auditCredentialUse(companyId, credential.id, client.id)
        CredentialValidationResult(credential, client)
    }

    suspend fun validateLimit(
        client: Cliente,
        amount: Long
    ): CredentialValidationResult {
        if (client.creditoActual + amount > client.limiteCredito) {
            throw CredentialException.LimitExceeded
        }
        return CredentialValidationResult(
            credential = CredencialCliente(
                clienteId = client.id,
                empresaId = client.empresaId,
                credentialId = "",
                estado = CredencialCliente.ESTADO_ACTIVA,
                emitidaPorUsuarioId = 0,
                fechaEmision = 0,
                fechaVencimiento = 0
            ),
            client = client
        )
    }

    suspend fun issueCredential(
        companyId: String,
        clientId: Int,
        pin: String,
        issuedBy: Int,
        now: Long = System.currentTimeMillis()
    ): CredencialCliente = database.withTransaction {
        val existingActive = database.credencialClienteDao().getActiveForClient(clientId, companyId)
        if (existingActive != null) {
            database.credencialClienteDao().revokeActive(
                clientId,
                companyId,
                CredencialCliente.ESTADO_REEMPLAZADA,
                now
            )
            auditCredentialReplace(companyId, existingActive.id, issuedBy)
        }
        val digest = PinHasher.hash(pin.toCharArray())
        val credential = CredencialCliente(
            clienteId = clientId,
            empresaId = companyId,
            credentialId = com.multipos.app.security.QrCredentialService.newCredentialId(),
            estado = CredencialCliente.ESTADO_ACTIVA,
            fechaEmision = now,
            emitidaPorUsuarioId = issuedBy,
            pinHash = digest.hash,
            pinSalt = digest.salt,
            fechaVencimiento = now + CredencialCliente.DURATION_CREDENTIAL_VALID_MS
        )
        val id = database.credencialClienteDao().insert(credential).toInt()
        auditCredentialIssue(companyId, id, issuedBy)
        credential.copy(id = id)
    }

    suspend fun revokeCredential(
        companyId: String,
        clientId: Int,
        revokedBy: Int
    ) {
        database.credencialClienteDao().revokeActive(
            clientId,
            companyId,
            CredencialCliente.ESTADO_REVOCADA,
            System.currentTimeMillis()
        )
        auditCredentialRevoke(companyId, clientId, revokedBy)
    }

    private suspend fun auditCredentialUse(companyId: String, credentialId: Int, clientId: Int) {
        database.auditoriaDao().insert(
            Auditoria(
                empresaId = companyId,
                usuarioId = null,
                accion = Auditoria.ACCION_VENTA,
                entidad = "credencial",
                entidadId = credentialId.toString(),
                detalle = "uso_credencial cliente_id=$clientId",
                fecha = System.currentTimeMillis()
            )
        )
    }

    private suspend fun auditCredentialIssue(companyId: String, credentialId: Int, userId: Int) {
        database.auditoriaDao().insert(
            Auditoria(
                empresaId = companyId,
                usuarioId = userId,
                accion = Auditoria.ACCION_CREDENCIAL_EMISION,
                entidad = "credencial",
                entidadId = credentialId.toString(),
                detalle = "credencial emitida",
                fecha = System.currentTimeMillis()
            )
        )
    }

    private suspend fun auditCredentialReplace(companyId: String, credentialId: Int, userId: Int) {
        database.auditoriaDao().insert(
            Auditoria(
                empresaId = companyId,
                usuarioId = userId,
                accion = Auditoria.ACCION_CREDENCIAL_REEMPLAZO,
                entidad = "credencial",
                entidadId = credentialId.toString(),
                detalle = "credencial reemplazada",
                fecha = System.currentTimeMillis()
            )
        )
    }

    private suspend fun auditCredentialRevoke(companyId: String, clientId: Int, userId: Int) {
        database.auditoriaDao().insert(
            Auditoria(
                empresaId = companyId,
                usuarioId = userId,
                accion = Auditoria.ACCION_CREDENCIAL_REVOCACION,
                entidad = "credencial",
                entidadId = clientId.toString(),
                detalle = "credencial revocada",
                fecha = System.currentTimeMillis()
            )
        )
    }
}

class CredentialBlockedException(val remainingMs: Long) : CredentialException(
    "Credencial bloqueada. Intenta en ${
        TimeUnit.MILLISECONDS.toMinutes(remainingMs)
    } minutos"
)
