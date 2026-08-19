package com.multipos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.multipos.app.data.dao.UsuarioDao
import com.multipos.app.data.dao.UsuarioEmpresaDao
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import com.multipos.app.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EmployeesUiState(
    val employees: List<Usuario> = emptyList(),
    val filteredEmployees: List<Usuario> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class EmployeesViewModel(
    private val usuarioDao: UsuarioDao,
    private val usuarioEmpresaDao: UsuarioEmpresaDao,
    private val companyId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmployeesUiState())
    val uiState: StateFlow<EmployeesUiState> = _uiState.asStateFlow()

    init {
        loadEmployees()
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            usuarioEmpresaDao.getUsersForCompany(companyId).collectLatest { list ->
                _uiState.value = _uiState.value.copy(
                    employees = list,
                    filteredEmployees = filterList(list, _uiState.value.searchQuery),
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredEmployees = filterList(_uiState.value.employees, query)
        )
    }

    private fun filterList(list: List<Usuario>, query: String): List<Usuario> {
        if (query.isBlank()) return list
        val lowerQuery = query.lowercase()
        return list.filter {
            it.nombre.lowercase().contains(lowerQuery) ||
                    it.usuario.lowercase().contains(lowerQuery)
        }
    }

    fun registerEmployee(nombre: String, username: String, pass: String, rol: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val digest = withContext(Dispatchers.Default) { PasswordHasher.hash(pass.toCharArray()) }
                val newUser = Usuario(
                    nombre = nombre,
                    usuario = username,
                    passwordHash = digest.hash,
                    passwordSalt = digest.salt,
                    rol = rol,
                    empresaId = companyId,
                    requiereCambioClave = true // Obligamos a que cambie la clave temporal que le dimos
                )
                
                withContext(Dispatchers.IO) {
                    val userId = usuarioDao.insert(newUser).toInt()
                    usuarioEmpresaDao.insert(UsuarioEmpresa(userId, companyId, rol))
                }
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun updateEmployee(userId: Int, nombre: String, rol: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    usuarioDao.updateName(userId, nombre)
                    usuarioEmpresaDao.updateRol(userId, companyId, rol)
                }
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}

class EmployeesViewModelFactory(
    private val usuarioDao: UsuarioDao,
    private val usuarioEmpresaDao: UsuarioEmpresaDao,
    private val companyId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EmployeesViewModel(usuarioDao, usuarioEmpresaDao, companyId) as T
    }
}
