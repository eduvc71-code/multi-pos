package com.multipos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.multipos.app.data.entities.Venta
import com.multipos.app.data.dao.VentaDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sales: List<Venta> = emptyList(),
    val filteredSales: List<Venta> = emptyList(),
    val searchQuery: String = "",
    val totalToday: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class HistoryViewModel(private val ventaDao: VentaDao, private val companyId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadSales()
    }

    private fun loadSales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            ventaDao.getAll(companyId).collectLatest { list ->
                val startOfDay = getStartOfDay()
                val total = list.filter { it.fecha >= startOfDay && it.estado == Venta.ESTADO_COMPLETADA }.sumOf { it.total }
                _uiState.value = _uiState.value.copy(
                    sales = list,
                    filteredSales = filterList(list, _uiState.value.searchQuery),
                    totalToday = total,
                    isLoading = false
                )
            }
        }
    }

    private fun getStartOfDay(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredSales = filterList(_uiState.value.sales, query)
        )
    }

    private fun filterList(list: List<Venta>, query: String): List<Venta> {
        if (query.isBlank()) return list
        val lowerQuery = query.lowercase()
        return list.filter {
            it.folio.lowercase().contains(lowerQuery) ||
                    it.clienteNombre.lowercase().contains(lowerQuery)
        }
    }
}

class HistoryViewModelFactory(
    private val ventaDao: VentaDao,
    private val companyId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(ventaDao, companyId) as T
    }
}
