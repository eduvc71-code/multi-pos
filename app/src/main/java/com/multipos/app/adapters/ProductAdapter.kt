package com.multipos.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.multipos.app.R
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.entities.Producto
import com.multipos.app.util.Money

class ProductAdapter(private val onItemClick: (Producto) -> Unit) :
    ListAdapter<Producto, ProductAdapter.ProductViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ProductViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
    )
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.txtName)
        private val price: TextView = view.findViewById(R.id.txtPrice)
        private val stock: TextView = view.findViewById(R.id.txtStock)
        fun bind(product: Producto) {
            name.text = product.nombre
            price.text = Money.format(product.precioVenta)
            price.setTextColor(runCatching { Color.parseColor(ActiveCompanyStore.color(itemView.context)) }.getOrDefault(Color.rgb(37, 99, 235)))
            stock.text = itemView.context.getString(R.string.stock_format, product.stock)
            stock.setTextColor(when { product.stock <= 0 -> Color.rgb(183, 28, 28); product.stock <= product.stockMinimo -> Color.rgb(245, 127, 23); else -> Color.rgb(27, 94, 32) })
            itemView.setOnClickListener { onItemClick(product) }
        }
    }
    private class DiffCallback : DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(a: Producto, b: Producto) = a.id == b.id
        override fun areContentsTheSame(a: Producto, b: Producto) = a == b
    }
}
