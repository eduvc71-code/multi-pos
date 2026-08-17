package com.multipos.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.multipos.app.R
import com.multipos.app.data.entities.Venta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.multipos.app.util.Money

class SaleAdapter : ListAdapter<Venta, SaleAdapter.Holder>(Diff) {
    var onItemClick: ((Venta) -> Unit)? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_sale, parent, false))
        holder.itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onItemClick?.invoke(getItem(position))
        }
        return holder
    }
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.txtSaleTitle)
        private val detail = view.findViewById<TextView>(R.id.txtSaleDetail)
        fun bind(sale: Venta) {
            title.text = itemView.context.getString(R.string.sale_title_format, sale.id, Money.format(sale.total))
            detail.text = itemView.context.getString(
                R.string.sale_detail_format,
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(sale.fecha)),
                sale.tipoPago,
                sale.estado
            )
        }
    }
    private object Diff : DiffUtil.ItemCallback<Venta>() {
        override fun areItemsTheSame(a: Venta, b: Venta) = a.id == b.id
        override fun areContentsTheSame(a: Venta, b: Venta) = a == b
    }
}
