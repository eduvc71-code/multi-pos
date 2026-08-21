package com.multipos.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multipos.app.data.*
import com.multipos.app.data.entities.Usuario
import com.multipos.app.util.InventorySeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.get(application)
    private val authRepository = AuthRepository(db)
    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    data class LoginUiState(
        val username: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val loginSuccess: Boolean = false
    )

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun consumeLoginSuccess() {
        Log.d("LoginViewModel", "Consumiendo loginSuccess")
        _uiState.value = _uiState.value.copy(loginSuccess = false)
    }

    fun login() {
        val username = _uiState.value.username
        val password = _uiState.value.password

        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "CREDENTIALS_REQUIRED")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val userLower = username.trim().lowercase()
            Log.d("LoginViewModel", "Intentando login para: $userLower")
            
            try {
                val blocked = withContext(Dispatchers.Default) {
                    db.usuarioDao().getByUsername(userLower)?.bloqueadoHasta
                }
                
                val now = System.currentTimeMillis()
                if (blocked != null && blocked > now) {
                    val mins = ((blocked - now) / 60_000L).coerceAtLeast(1L)
                    Log.w("LoginViewModel", "Usuario bloqueado por $mins mins")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "BLOCKED|$mins")
                    return@launch
                }
                
                val user = withContext(Dispatchers.Default) {
                    authRepository.authenticate(userLower, password.toCharArray())
                }
                
                if (user == null) {
                    Log.w("LoginViewModel", "Credenciales inválidas")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "INVALID_CREDENTIALS")
                } else {
                    Log.d("LoginViewModel", "Login exitoso para: ${user.usuario}")
                    completeLogin(user)
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error durante el proceso de login", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun completeLogin(user: Usuario) {
        UserSessionStore.set(context, user)
        Log.d("LoginViewModel", "Sesión guardada en UserSessionStore")
        
        val count = withContext(Dispatchers.IO) { db.productoDao().count(user.empresaId) }
        if (count == 0) {
            Log.d("LoginViewModel", "Sembrando inventario inicial")
            InventorySeeder.seedAbarrotes(db, user.empresaId)
        }

        val currentCompany = ActiveCompanyStore.get(context)
        val membership = withContext(Dispatchers.IO) { 
            db.usuarioEmpresaDao().getActiveMembership(user.id, currentCompany) 
        }
        
        if (membership == null) {
            Log.d("LoginViewModel", "Estableciendo empresa activa por defecto: ${user.empresaId}")
            ActiveCompanyStore.set(context, user.empresaId)
            val company = withContext(Dispatchers.IO) { db.empresaDao().getById(user.empresaId) }
            if (company != null) {
                ActiveCompanyStore.setName(context, company.nombre)
                ActiveCompanyStore.setColor(context, company.colorPrimarioHex)
            }
        }
        
        _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
    }
}
