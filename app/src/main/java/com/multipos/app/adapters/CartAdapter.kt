package com.multipos.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.multipos.app.R
import com.multipos.app.data.entities.Producto

data class CartLine(val product: Producto, val quantity: Int)

class CartAdapter(private val onMinus: (Producto) -> Unit, private val onPlus: (Producto) -> Unit) : RecyclerView.Adapter<CartAdapter.Holder>() {
    private var lines: List<CartLine> = emptyList()
    fun submitList(value: List<CartLine>) { lines = value; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false))
    override fun getItemCount() = lines.size
    override fun onBindViewHolder(holder: Holder, position: Int) { val line = lines[position]; holder.bind(line, onMinus, onPlus) }
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val name = view.findViewById<TextView>(R.id.txtCartName)
        private val quantity = view.findViewById<TextView>(R.id.txtCartQuantity)
        fun bind(line: CartLine, minus: (Producto) -> Unit, plus: (Producto) -> Unit) { name.text = line.product.nombre; quantity.text = line.quantity.toString(); itemView.findViewById<View>(R.id.btnMinus).setOnClickListener { minus(line.product) }; itemView.findViewById<View>(R.id.btnPlus).setOnClickListener { plus(line.product) } }
    }
}
