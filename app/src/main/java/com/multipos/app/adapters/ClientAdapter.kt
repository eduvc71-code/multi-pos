package com.multipos.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.multipos.app.R
import com.multipos.app.data.entities.Cliente
import java.text.NumberFormat
import java.util.Locale

class ClientAdapter(private val onClick: (Cliente) -> Unit) : ListAdapter<Cliente, ClientAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_client, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position), onClick)
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val name = view.findViewById<TextView>(R.id.txtClientName)
        private val detail = view.findViewById<TextView>(R.id.txtClientDetail)
        fun bind(client: Cliente, click: (Cliente) -> Unit) { name.text = client.nombre; detail.text = "${client.documento} · Crédito: ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(client.creditoActual)}"; itemView.setOnClickListener { click(client) } }
    }
    private object Diff : DiffUtil.ItemCallback<Cliente>() { override fun areItemsTheSame(a: Cliente, b: Cliente) = a.id == b.id; override fun areContentsTheSame(a: Cliente, b: Cliente) = a == b }
}
