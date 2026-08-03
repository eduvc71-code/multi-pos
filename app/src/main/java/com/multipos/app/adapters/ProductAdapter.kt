package com.multipos.app.adapters  
  
import android.view.LayoutInflater  
import android.view.View  
import android.view.ViewGroup  
import android.widget.TextView  
import androidx.recyclerview.widget.DiffUtil  
import androidx.recyclerview.widget.ListAdapter  
import androidx.recyclerview.widget.RecyclerView  
import com.multipos.app.R  
import com.multipos.app.data.entities.Producto  
import java.text.NumberFormat  
import java.util.Locale  
  
class ProductAdapter(private val onItemClick: (Producto) -> Unit) : ListAdapter<Producto, ProductAdapter.ProductViewHolder(DiffCallback()) {  
  
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {  
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)  
        return ProductViewHolder(view)  
    }  
  
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {  
        holder.bind(getItem(position), onItemClick)  
    }  
  
