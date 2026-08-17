package com.multipos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.multipos.app.data.entities.Producto
import com.multipos.app.data.dao.ProductoDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CartItem(
    val product: Producto,
    val quantity: Int
)

data class PosUiState(
    val products: List<Producto> = emptyList(),
    val filteredProducts: List<Producto> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val total: Long = 0,
    val selectedClient: String? = null,
    val paymentMethod: String = "EFECTIVO",
    val error: String? = null
)

class PosViewModel(private val productoDao: ProductoDao, private val companyId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

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

    fun addToCart(product: Producto) {
        val currentCart = _uiState.value.cart.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
        
        if (existingIndex >= 0) {
            currentCart[existingIndex] = currentCart[existingIndex].copy(
                quantity = currentCart[existingIndex].quantity + 1
            )
        } else {
            currentCart.add(CartItem(product, 1))
        }
        updateCart(currentCart)
    }

    fun updateQuantity(productId: Int, delta: Int) {
        val currentCart = _uiState.value.cart.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val newQuantity = currentCart[index].quantity + delta
            if (newQuantity <= 0) {
                currentCart.removeAt(index)
            } else {
                currentCart[index] = currentCart[index].copy(quantity = newQuantity)
            }
            updateCart(currentCart)
        }
    }

    fun removeFromCart(productId: Int) {
        val currentCart = _uiState.value.cart.filter { it.product.id != productId }
        updateCart(currentCart)
    }

    fun setPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = method)
    }

    fun setClient(client: String?) {
        _uiState.value = _uiState.value.copy(selectedClient = client)
    }

    private fun updateCart(newCart: List<CartItem>) {
        val newTotal = newCart.sumOf { it.product.precioVenta * it.quantity }
        _uiState.value = _uiState.value.copy(cart = newCart, total = newTotal)
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(cart = emptyList(), total = 0)
    }
}

class PosViewModelFactory(
    private val productoDao: ProductoDao,
    private val companyId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PosViewModel(productoDao, companyId) as T
    }
}
