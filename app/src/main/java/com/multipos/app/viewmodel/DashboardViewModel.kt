package com.multipos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.multipos.app.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val totalSalesToday: Long = 0,
    val totalProducts: Int = 0,
    val lowStockCount: Int = 0,
    val isLoading: Boolean = false
)

class DashboardViewModel(private val db: AppDatabase, private val companyId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val sales = db.ventaDao().totalSince(start, companyId)
            val products = db.productoDao().count(companyId)
            val lowStock = db.productoDao().lowStockCount(companyId)
            
            _uiState.value = DashboardUiState(
                totalSalesToday = sales,
                totalProducts = products,
                lowStockCount = lowStock,
                isLoading = false
            )
        }
    }
}

class DashboardViewModelFactory(
    private val db: AppDatabase,
    private val companyId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(db, companyId) as T
    }
}
