package com.multipos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.dao.ClienteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ClientsUiState(
    val clients: List<Cliente> = emptyList(),
    val filteredClients: List<Cliente> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ClientsViewModel(private val clienteDao: ClienteDao, private val companyId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    init {
        loadClients()
    }

    private fun loadClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            clienteDao.getAll(companyId).collectLatest { list ->
                _uiState.value = _uiState.value.copy(
                    clients = list,
                    filteredClients = filterList(list, _uiState.value.searchQuery),
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredClients = filterList(_uiState.value.clients, query)
        )
    }

    private fun filterList(list: List<Cliente>, query: String): List<Cliente> {
        if (query.isBlank()) return list
        val lowerQuery = query.lowercase()
        return list.filter {
            it.nombre.lowercase().contains(lowerQuery) ||
                    it.documento?.lowercase()?.contains(lowerQuery) == true
        }
    }

    fun saveClient(client: Cliente, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (client.id == 0) clienteDao.insert(client)
                else clienteDao.update(client)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}

class ClientsViewModelFactory(
    private val clienteDao: ClienteDao,
    private val companyId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ClientsViewModel(clienteDao, companyId) as T
    }
}
