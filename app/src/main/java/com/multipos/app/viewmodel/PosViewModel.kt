package com.multipos.app.viewmodel  
  
import androidx.lifecycle.LiveData  
import androidx.lifecycle.MutableLiveData  
import androidx.lifecycle.ViewModel  
import com.multipos.app.data.entities.Producto  
  
data class CartItem(val product: Producto, var quantity: Int)  
  
class PosViewModel : ViewModel() {  
    private val _products = MutableLiveData<List<Producto>>()  
    val products: LiveData<List<Producto>> = _products  
    private val _cartItems = MutableLiveData<List<CartItem>>()  
    val cartItems: LiveData<List<CartItem>> = _cartItems  
    init { _cartItems.value = emptyList() }  
    fun loadProducts() { _products.value = emptyList() }  
    fun getTotalAmount(): Long = 0
} 
