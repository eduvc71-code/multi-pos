package com.multipos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.RegisterSaleRequest
import com.multipos.app.data.SaleLineSnapshot
import com.multipos.app.data.SaleRepository
import com.multipos.app.data.entities.Producto
import com.multipos.app.data.models.CartLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PosUiState(
    val products: List<Producto> = emptyList(),
    val filteredProducts: List<Producto> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val total: Long = 0,
    val selectedClient: String? = null,
    val clientId: Int? = null,
    val paymentMethod: String = "EFECTIVO",
    val lastSaleId: Int? = null,
    val lastSaleTotal: Long = 0,
    val warning: String? = null, // Cambio de error a advertencia
    val error: String? = null
)

class PosViewModel(private val db: AppDatabase, private val companyId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()
    private val saleRepository = SaleRepository(db)
    private val productoDao = db.productoDao()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            productoDao.getAll(companyId).collectLatest { list ->
                _uiState.value = _uiState.value.copy(
                    products = list,
                    filteredProducts = filterList(list, _uiState.value.searchQuery),
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredProducts = filterList(_uiState.value.products, query)
        )
    }

    private fun filterList(list: List<Producto>, query: String): List<Producto> {
        if (query.isBlank()) return list
        val lowerQuery = query.lowercase()
        return list.filter {
            it.nombre.lowercase().contains(lowerQuery) ||
                    it.codigo.lowercase().contains(lowerQuery)
        }
    }

    fun addToCart(product: Producto, quantity: Int) {
        val currentCart = _uiState.value.cart.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
        
        if (quantity > product.stock) {
            _uiState.value = _uiState.value.copy(warning = "Atención: Stock insuficiente (${product.stock} disp.)")
        }

        if (existingIndex >= 0) {
            currentCart[existingIndex] = currentCart[existingIndex].copy(
                quantity = currentCart[existingIndex].quantity + quantity
            )
        } else {
            currentCart.add(CartLine(product, quantity))
        }
        updateCart(currentCart)
    }

    fun updateQuantity(productId: Int, delta: Int) {
        val currentCart = _uiState.value.cart.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val product = currentCart[index].product
            val newQuantity = currentCart[index].quantity + delta
            
            if (newQuantity > product.stock) {
                _uiState.value = _uiState.value.copy(warning = "Atención: Sobrepasando stock de ${product.stock}")
            }
            
            if (newQuantity <= 0) {
                currentCart.removeAt(index)
            } else {
                currentCart[index] = currentCart[index].copy(quantity = newQuantity)
            }
            updateCart(currentCart)
        }
    }

    fun clearWarning() {
        _uiState.value = _uiState.value.copy(warning = null)
    }

    fun removeFromCart(productId: Int) {
        val currentCart = _uiState.value.cart.filter { it.product.id != productId }
        updateCart(currentCart)
    }

    fun setPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = method)
    }

    fun setClient(client: String?, clientId: Int? = null) {
        _uiState.value = _uiState.value.copy(selectedClient = client, clientId = clientId)
    }

    fun processSale(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val request = RegisterSaleRequest(
                    paymentType = _uiState.value.paymentMethod,
                    total = _uiState.value.total,
                    subtotal = _uiState.value.total,
                    discount = 0,
                    tax = 0,
                    clientId = _uiState.value.clientId,
                    credentialId = null, // TODO: Implement for credit
                    pin = null,
                    userId = userId,
                    companyId = companyId,
                    lines = _uiState.value.cart.map { 
                        SaleLineSnapshot(it.product.id, it.quantity, it.product.precioVenta) 
                    }
                )
                val saleId = saleRepository.register(request)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastSaleId = saleId,
                    lastSaleTotal = _uiState.value.total,
                    cart = emptyList(),
                    total = 0,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun clearLastSale() {
        _uiState.value = _uiState.value.copy(lastSaleId = null, error = null)
    }

    private fun updateCart(newCart: List<CartLine>) {
        val newTotal = newCart.sumOf { it.product.precioVenta * it.quantity }
        _uiState.value = _uiState.value.copy(cart = newCart, total = newTotal)
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(cart = emptyList(), total = 0)
    }
}

class PosViewModelFactory(
    private val db: AppDatabase,
    private val companyId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PosViewModel(db, companyId) as T
    }
}
