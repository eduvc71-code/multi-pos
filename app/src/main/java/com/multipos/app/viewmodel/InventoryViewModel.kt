package com.multipos.app.viewmodel  
  
import androidx.lifecycle.LiveData  
import androidx.lifecycle.MutableLiveData  
import androidx.lifecycle.ViewModel  
import com.multipos.app.data.entities.Producto  
  
class InventoryViewModel : ViewModel() {  
    private val _products = MutableLiveData<List<Producto>>()  
    val products: LiveData<List<Producto>> = _products  
  
    fun loadProducts() { _products.value = emptyList() }  
} 
