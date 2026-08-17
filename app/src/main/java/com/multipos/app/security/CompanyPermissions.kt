package com.multipos.app.security

import android.content.Context
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Usuario

enum class CompanyPermission {
    SELL,
    VIEW_DASHBOARD,
    MANAGE_INVENTORY,
    VIEW_HISTORY,
    MANAGE_CLIENT_CREDIT,
    MANAGE_EMPLOYEES,
    CREATE_COMPANY,
    MANAGE_CASH,
    MANAGE_RETURNS,
    VIEW_REPORTS
}

object CompanyPermissions {
    fun allows(role: String?, permission: CompanyPermission): Boolean = when (role) {
        Usuario.ROL_PROPIETARIO -> true
        Usuario.ROL_ADMINISTRADOR -> permission != CompanyPermission.CREATE_COMPANY
        Usuario.ROL_CAJERO -> permission == CompanyPermission.SELL || permission == CompanyPermission.MANAGE_CASH
        Usuario.ROL_VENDEDOR -> permission == CompanyPermission.SELL
        else -> false
    }
}

object ActiveCompanyAccess {
    suspend fun role(context: Context, database: AppDatabase): String? {
        val userId = UserSessionStore.userId(context)
        if (userId <= 0) return null
        return database.usuarioEmpresaDao()
            .getActiveMembership(userId, ActiveCompanyStore.get(context))
            ?.rol
    }

    suspend fun allows(
        context: Context,
        database: AppDatabase,
        permission: CompanyPermission
    ): Boolean = CompanyPermissions.allows(role(context, database), permission)
}
