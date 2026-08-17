package com.multipos.app.security

import com.multipos.app.data.entities.Usuario
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanyPermissionsTest {
    @Test
    fun ownerHasEveryPermission() {
        CompanyPermission.entries.forEach { permission ->
            assertTrue(CompanyPermissions.allows(Usuario.ROL_PROPIETARIO, permission))
        }
    }

    @Test
    fun administratorCannotCreateCompaniesButCanManageCurrentCompany() {
        assertFalse(
            CompanyPermissions.allows(
                Usuario.ROL_ADMINISTRADOR,
                CompanyPermission.CREATE_COMPANY
            )
        )
        assertTrue(
            CompanyPermissions.allows(
                Usuario.ROL_ADMINISTRADOR,
                CompanyPermission.MANAGE_EMPLOYEES
            )
        )
        assertTrue(
            CompanyPermissions.allows(
                Usuario.ROL_ADMINISTRADOR,
                CompanyPermission.MANAGE_CLIENT_CREDIT
            )
        )
    }

    @Test
    fun cashierCanSellAndOperateCashWhileSellerCanOnlySell() {
        CompanyPermission.entries.forEach { permission ->
            assertTrue(
                "Permiso inesperado para ${Usuario.ROL_CAJERO}: $permission",
                CompanyPermissions.allows(Usuario.ROL_CAJERO, permission) ==
                    (permission == CompanyPermission.SELL || permission == CompanyPermission.MANAGE_CASH)
            )
            assertTrue(
                "Permiso inesperado para ${Usuario.ROL_VENDEDOR}: $permission",
                CompanyPermissions.allows(Usuario.ROL_VENDEDOR, permission) ==
                    (permission == CompanyPermission.SELL)
            )
        }
    }

    @Test
    fun missingOrUnknownRoleHasNoPermissions() {
        assertFalse(CompanyPermissions.allows(null, CompanyPermission.SELL))
        assertFalse(CompanyPermissions.allows("DESCONOCIDO", CompanyPermission.SELL))
    }
}
