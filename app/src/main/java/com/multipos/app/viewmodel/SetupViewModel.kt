package com.multipos.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.multipos.app.data.*
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import com.multipos.app.security.PasswordHasher
import com.multipos.app.util.InventorySeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.get(application)
    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    data class SetupUiState(
        val businessName: String = "",
        val businessNit: String = "",
        val ownerName: String = "",
        val username: String = "",
        val pass: String = "",
        val confirmPass: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val setupSuccess: Boolean = false
    )

    fun updateBusinessName(value: String) { _uiState.value = _uiState.value.copy(businessName = value) }
    fun updateBusinessNit(value: String) { _uiState.value = _uiState.value.copy(businessNit = value) }
    fun updateOwnerName(value: String) { _uiState.value = _uiState.value.copy(ownerName = value) }
    fun updateUsername(value: String) { _uiState.value = _uiState.value.copy(username = value) }
    fun updatePass(value: String) { _uiState.value = _uiState.value.copy(pass = value) }
    fun updateConfirmPass(value: String) { _uiState.value = _uiState.value.copy(confirmPass = value) }

    fun createWorkspace() {
        val state = _uiState.value
        val userTrim = state.username.trim().lowercase()
        
        if (state.businessName.length < 2) { 
            _uiState.value = _uiState.value.copy(error = "BUSINESS_NAME_SHORT")
            return 
        }
        if (state.ownerName.length < 3) { 
            _uiState.value = _uiState.value.copy(error = "OWNER_NAME_SHORT")
            return 
        }
        if (!userTrim.matches(Regex("[a-z0-9._-]{4,30}"))) { 
            _uiState.value = _uiState.value.copy(error = "USERNAME_INVALID")
            return 
        }
        if (state.pass.length < 8) { 
            _uiState.value = _uiState.value.copy(error = "PASSWORD_SHORT")
            return 
        }
        if (state.pass != state.confirmPass) { 
            _uiState.value = _uiState.value.copy(error = "PASSWORD_MISMATCH")
            return 
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val digest = withContext(Dispatchers.Default) { PasswordHasher.hash(state.pass.toCharArray()) }
                val company = Empresa(
                    id = UUID.randomUUID().toString(),
                    nombre = state.businessName,
                    nit = state.businessNit.trim()
                )
                
                var createdUser: Usuario? = null
                db.withTransaction {
                    if (db.empresaDao().count() > 0) throw IllegalStateException("COMPANY_EXISTS")
                    db.empresaDao().insert(company)
                    val id = db.usuarioDao().insert(
                        Usuario(
                            nombre = state.ownerName,
                            usuario = userTrim,
                            passwordHash = digest.hash,
                            passwordSalt = digest.salt,
                            rol = Usuario.ROL_PROPIETARIO,
                            empresaId = company.id
                        )
                    ).toInt()
                    db.usuarioEmpresaDao().insert(UsuarioEmpresa(id, company.id, Usuario.ROL_PROPIETARIO))
                    createdUser = db.usuarioDao().getById(id)
                }
                val owner = checkNotNull(createdUser)
                
                InventorySeeder.seedAbarrotes(db, company.id)
                
                ActiveCompanyStore.set(context, company.id)
                ActiveCompanyStore.setColor(context, company.colorPrimarioHex)
                UserSessionStore.set(context, owner)
                
                _uiState.value = _uiState.value.copy(isLoading = false, setupSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "UNKNOWN_ERROR")
            }
        }
    }
}
