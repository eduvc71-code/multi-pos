package com.multipos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.ReporteTipo
import com.multipos.app.data.ReportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class ReportsUiState(
    val reportData: com.multipos.app.data.ReportData? = null,
    val reportType: ReporteTipo = ReporteTipo.VENTAS,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ReportsViewModel(
    private val db: AppDatabase, 
    private val companyId: String,
    private val userId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()
    private val repository = ReportsRepository(db)

    init {
        generateReport()
    }

    fun generateReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Rango por defecto: Mes actual
                val cal = Calendar.getInstance()
                val hasta = cal.timeInMillis + 86400000L // Mañana
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                val desde = cal.timeInMillis

                val data = repository.compute(
                    companyId = companyId,
                    userId = userId,
                    desde = desde,
                    hastaExclusive = hasta,
                    tipo = _uiState.value.reportType
                )
                _uiState.value = _uiState.value.copy(reportData = data, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun setReportType(tipo: ReporteTipo) {
        _uiState.value = _uiState.value.copy(reportType = tipo)
        generateReport()
    }

    fun exportCsv() {
        // La exportación real requiere FileProvider y permisos, 
        // por ahora simulamos que el motor existe en ReportExport
    }
}

class ReportsViewModelFactory(
    private val db: AppDatabase,
    private val companyId: String,
    private val userId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReportsViewModel(db, companyId, userId) as T
    }
}
