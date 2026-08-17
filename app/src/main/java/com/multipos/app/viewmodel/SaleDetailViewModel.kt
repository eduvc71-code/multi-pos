package com.multipos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.entities.DetalleVenta
import com.multipos.app.data.entities.Devolucion
import com.multipos.app.data.entities.Venta
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SaleDetailUiState(
    val sale: Venta? = null,
    val details: List<DetalleVenta> = emptyList(),
    val refunds: List<Devolucion> = emptyList(),
    val vendedorName: String = "—",
    val clienteName: String = "—",
    val isLoading: Boolean = true,
    val canManageReturns: Boolean = false,
    val error: String? = null
)

class SaleDetailViewModel(
    private val db: AppDatabase,
    private val saleId: Int,
    private val companyId: String,
    private val role: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(SaleDetailUiState())
    val uiState: StateFlow<SaleDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val sale = db.ventaDao().getById(saleId, companyId)
            if (sale != null) {
                val details = db.ventaDao().getDetails(saleId, companyId)
                val refunds = db.devolucionDao().getBySale(companyId, saleId)
                val vendedor = db.usuarioDao().getById(sale.idUsuario)?.nombre ?: "—"
                val cliente = sale.idCliente?.let { 
                    db.clienteDao().getByIdIncludingInactive(it, companyId)?.nombre 
                } ?: "Público General"
                
                val canManage = CompanyPermissions.allows(role, CompanyPermission.MANAGE_RETURNS)

                _uiState.value = _uiState.value.copy(
                    sale = sale,
                    details = details,
                    refunds = refunds,
                    vendedorName = vendedor,
                    clienteName = cliente,
                    canManageReturns = canManage,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Venta no encontrada"
                )
            }
        }
    }
}

class SaleDetailViewModelFactory(
    private val db: AppDatabase,
    private val saleId: Int,
    private val companyId: String,
    private val role: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SaleDetailViewModel(db, saleId, companyId, role) as T
    }
}
