package com.multipos.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multipos.app.data.*
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions
import com.multipos.app.util.InventorySeeder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.get(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    data class HomeUiState(
        val activeCompanyName: String = "Cargando...",
        val userName: String = "...",
        val userRole: String = "Sin rol",
        val companyColor: Color = Color(0xFF1E40AF),
        val selectedMenu: String = "DASHBOARD",
        val companyId: String = "",
        val userId: Int = 0,
        val activeRole: String? = null,
        val logoutSuccess: Boolean = false,
        val companies: List<com.multipos.app.data.entities.Empresa> = emptyList()
    )

    init {
        Log.d("HomeViewModel", "Iniciando HomeViewModel")
        loadSessionData()
    }

    fun loadSessionData() {
        val userId = UserSessionStore.userId(getApplication())
        if (userId == 0) {
            Log.w("HomeViewModel", "UserId es 0, cerrando sesión")
            logout()
            return
        }

        viewModelScope.launch {
            Log.d("HomeViewModel", "Cargando datos para usuario: $userId")
            val user = db.usuarioDao().getById(userId)
            if (user == null) {
                Log.e("HomeViewModel", "Usuario no encontrado o inactivo, cerrando sesión")
                logout()
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(userName = user.nombre, userId = userId)
            
            db.usuarioEmpresaDao().getCompaniesForUser(userId).collect { companies ->
                Log.d("HomeViewModel", "Empresas encontradas: ${companies.size}")
                _uiState.value = _uiState.value.copy(companies = companies)
                
                if (companies.isEmpty()) {
                    Log.e("HomeViewModel", "El usuario no tiene empresas asignadas, cerrando sesión")
                    logout()
                    return@collect
                }

                val activeId = ActiveCompanyStore.get(getApplication())
                val active = companies.firstOrNull { it.id == activeId } ?: companies.first()
                
                if (active.id != activeId) {
                    Log.d("HomeViewModel", "Cambiando empresa activa a: ${active.nombre}")
                    ActiveCompanyStore.set(getApplication(), active.id)
                    ActiveCompanyStore.setColor(getApplication(), active.colorPrimarioHex)
                    ActiveCompanyStore.setName(getApplication(), active.nombre)
                }
                
                val hex = active.colorPrimarioHex
                val color = try { Color(hex.toColorInt()) } catch (_: Exception) { Color(0xFF1E40AF) }
                
                val productCount = db.productoDao().count(active.id)
                if (productCount == 0) {
                    Log.d("HomeViewModel", "Sembrando inventario para: ${active.id}")
                    InventorySeeder.seedAbarrotes(db, active.id)
                }

                val membership = db.usuarioEmpresaDao().getActiveMembership(userId, active.id)
                _uiState.value = _uiState.value.copy(
                    activeCompanyName = active.nombre,
                    companyColor = color,
                    companyId = active.id,
                    activeRole = membership?.rol,
                    userRole = membership?.rol ?: "Sin rol"
                )
            }
        }
    }

    fun setSelectedMenu(menu: String) {
        if (canNavigateTo(menu)) {
            _uiState.value = _uiState.value.copy(selectedMenu = menu)
        }
    }

    fun canNavigateTo(menu: String): Boolean {
        val permission = when (menu) {
            "DASHBOARD" -> CompanyPermission.VIEW_DASHBOARD
            "POS" -> CompanyPermission.SELL
            "INVENTORY" -> CompanyPermission.MANAGE_INVENTORY
            "HISTORY" -> CompanyPermission.VIEW_HISTORY
            "CLIENTS" -> CompanyPermission.MANAGE_CLIENT_CREDIT
            "EMPLOYEES" -> CompanyPermission.MANAGE_EMPLOYEES
            "CASH" -> CompanyPermission.MANAGE_CASH
            "REPORTS" -> CompanyPermission.VIEW_REPORTS
            else -> return false
        }
        return CompanyPermissions.allows(_uiState.value.activeRole, permission)
    }

    fun logout() {
        Log.d("HomeViewModel", "Ejecutando logout completo", Throwable())
        UserSessionStore.clear(getApplication())
        _uiState.value = _uiState.value.copy(logoutSuccess = true)
    }
}
