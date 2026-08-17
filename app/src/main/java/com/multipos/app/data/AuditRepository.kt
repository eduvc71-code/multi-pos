package com.multipos.app.data

import android.content.Context
import com.multipos.app.data.entities.Auditoria
import com.multipos.app.data.entities.Usuario
import com.multipos.app.security.ActiveCompanyAccess
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a la auditoría restringido al rol PROPIETARIO, validado también en la
 * capa de negocio (no solo en la navegación/UI).
 */
class AuditRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    /** Consulta de auditoría de la empresa activa. Solo PROPIETARIO. */
    suspend fun getForActiveCompany(): Flow<List<Auditoria>> {
        val currentCompany = ActiveCompanyStore.get(context)
        val role = ActiveCompanyAccess.role(context, database)
        require(role == Usuario.ROL_PROPIETARIO) { "Solo el propietario puede consultar la auditoría" }
        return database.auditoriaDao().getByCompany(currentCompany)
    }
}