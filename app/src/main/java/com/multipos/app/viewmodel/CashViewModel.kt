package com.multipos.app.viewmodel

import androidx.lifecycle.*
import com.multipos.app.data.CashRepository
import com.multipos.app.data.entities.CajaSesion
import com.multipos.app.data.entities.MovimientoCaja
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CashUiState(
    val session: CajaSesion? = null,
    val ingresos: Long = 0,
    val egresos: Long = 0,
    val expected: Long = 0,
    val difference: Long = 0,
    val movements: List<MovimientoCaja> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CashViewModel(private val repository: CashRepository, private val companyId: String, private val userId: Int) : ViewModel() {

    private val _uiState = MutableStateFlow(CashUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.value = uiState.value.copy(isLoading = true, error = null)
            try {
                val session = repository.getActiveSession(companyId)
                if (session != null) {
                    val balance = repository.getSessionWithBalance(companyId, session.id)
                    val movements = repository.getMovementsForSession(companyId, session.id)
                    _uiState.value = CashUiState(
                        session = session,
                        ingresos = balance?.ingresos ?: 0,
                        egresos = balance?.egresos ?: 0,
                        expected = balance?.expected ?: session.montoApertura,
                        difference = balance?.difference ?: 0,
                        movements = movements,
                        isLoading = false
                    )
                } else {
                    _uiState.value = CashUiState(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = CashUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun openSession(montoApertura: Long) {
        viewModelScope.launch {
            _uiState.value = uiState.value.copy(isLoading = true, error = null)
            try {
                repository.openSession(companyId, userId, montoApertura)
                loadSession()
            } catch (e: Exception) {
                _uiState.value = CashUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun closeSession(montoContado: Long, nota: String) {
        val sessionId = uiState.value.session?.id ?: return
        viewModelScope.launch {
            _uiState.value = uiState.value.copy(isLoading = true, error = null)
            try {
                repository.closeSession(companyId, sessionId, userId, montoContado, nota)
                _uiState.value = CashUiState(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = CashUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun addManualMovement(tipo: String, monto: Long, concepto: String) {
        val sessionId = uiState.value.session?.id ?: return
        viewModelScope.launch {
            _uiState.value = uiState.value.copy(isLoading = true, error = null)
            try {
                repository.registerManualMovement(companyId, sessionId, userId, tipo, monto, concepto)
                loadSession()
            } catch (e: Exception) {
                _uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun refresh() {
        loadSession()
    }

    fun clearError() {
        _uiState.value = uiState.value.copy(error = null)
    }
}

class CashViewModelFactory(
    private val repository: CashRepository,
    private val companyId: String,
    private val userId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CashViewModel(repository, companyId, userId) as T
    }
}
