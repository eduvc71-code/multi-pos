package com.multipos.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.multipos.app.R
import com.multipos.app.data.ReportRow
import com.multipos.app.data.ReportsRepository
import com.multipos.app.data.Unidad
import com.multipos.app.util.Money

class ReportAdapter(private var rows: List<ReportRow> = emptyList()) :
    RecyclerView.Adapter<ReportAdapter.Holder>() {

    fun update(newRows: List<ReportRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false))

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(rows[position])

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val fecha = view.findViewById<TextView>(R.id.txtReportFecha)
        private val importe = view.findViewById<TextView>(R.id.txtReportImporte)
        private val concepto = view.findViewById<TextView>(R.id.txtReportConcepto)

        fun bind(row: ReportRow) {
            fecha.text = row.fecha
            importe.text = if (ReportsRepository.unidadDe(row.categoria) == Unidad.MONEDA) {
                Money.format(row.importe)
            } else {
                row.importe.toString()
            }
            concepto.text = row.concepto
        }
    }
}