package com.multipos.app.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.multipos.app.adapters.ReportAdapter
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.ReportAggregator
import com.multipos.app.data.ReportData
import com.multipos.app.data.ReportException
import com.multipos.app.data.ReportRow
import com.multipos.app.data.ReportsRepository
import com.multipos.app.data.ReporteTipo
import com.multipos.app.data.Unidad
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Abono
import com.multipos.app.data.entities.Usuario
import com.multipos.app.databinding.FragmentReportsBinding
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import com.multipos.app.util.EstadoCuentaFilters
import com.multipos.app.util.Money
import com.multipos.app.util.ReceiptPdfGenerator
import com.multipos.app.util.ReportExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportesFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var companyId: String
    private var userId: Int = 0
    private var users: List<Usuario> = emptyList()
    private var lastData: ReportData? = null
    private var lastRange: EstadoCuentaFilters.RangoResult.Range? = null
    private var lastTipo: ReporteTipo = ReporteTipo.VENTAS
    private val adapter = ReportAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        db = DatabaseProvider.get(requireContext())
        companyId = ActiveCompanyStore.get(requireContext())
        userId = UserSessionStore.userId(requireContext())
        binding.recyclerReport.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerReport.adapter = adapter
        binding.spnTipo.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(com.multipos.app.R.string.reportes_tipo_ventas),
                getString(com.multipos.app.R.string.reportes_tipo_rentabilidad),
                getString(com.multipos.app.R.string.reportes_tipo_caja),
                getString(com.multipos.app.R.string.reportes_tipo_inventario),
                getString(com.multipos.app.R.string.reportes_tipo_credito)
            )
        )
        binding.spnMedioPago.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(com.multipos.app.R.string.todos),
                Abono.MEDIO_EFECTIVO,
                Abono.MEDIO_TARJETA,
                Abono.MEDIO_TRANSFERENCIA,
                "CREDITO"
            )
        )
        binding.btnGenerar.setOnClickListener { generar() }
        binding.btnExportCSV.setOnClickListener { exportar(csv = true) }
        binding.btnExportPDF.setOnClickListener { exportar(csv = false) }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            val canView = ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.VIEW_REPORTS)
            if (!canView) {
                Toast.makeText(requireContext(), getString(com.multipos.app.R.string.reportes_sin_permiso), Toast.LENGTH_SHORT).show()
                return@launch
            }
            db.usuarioDao().getByCompany(companyId).collect { list ->
                users = list
                binding.spnVendedor.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf(getString(com.multipos.app.R.string.todos)) + list.map { it.nombre }
                )
            }
        }
    }

    private fun tipoSeleccionado(): ReporteTipo = when (binding.spnTipo.selectedItemPosition) {
        1 -> ReporteTipo.RENTABILIDAD
        2 -> ReporteTipo.CAJA
        3 -> ReporteTipo.INVENTARIO
        4 -> ReporteTipo.CREDITO
        else -> ReporteTipo.VENTAS
    }

    private fun medioSeleccionado(): String? = when (binding.spnMedioPago.selectedItemPosition) {
        0 -> null
        4 -> "CREDITO"
        else -> listOf(Abono.MEDIO_EFECTIVO, Abono.MEDIO_TARJETA, Abono.MEDIO_TRANSFERENCIA)[binding.spnMedioPago.selectedItemPosition - 1]
    }

    private fun vendedorSeleccionado(): Int? {
        val index = binding.spnVendedor.selectedItemPosition - 1
        return users.getOrNull(index)?.id
    }

    private fun generar() {
        val range = EstadoCuentaFilters.parseDateRange(binding.etDesde.text.toString(), binding.etHasta.text.toString())
        if (range is EstadoCuentaFilters.RangoResult.Invalid) {
            Toast.makeText(requireContext(), getString(com.multipos.app.R.string.reportes_fecha_invalida), Toast.LENGTH_SHORT).show()
            return
        }
        val desde: Long
        val hasta: Long
        if (range is EstadoCuentaFilters.RangoResult.Range) {
            desde = range.desde
            hasta = range.hastaExclusive
        } else {
            val now = System.currentTimeMillis()
            desde = EstadoCuentaFilters.startOfDay(now - ReportsRepository.MAX_RANGE_DAYS * ReportsRepository.DAY_MS)
            hasta = EstadoCuentaFilters.startOfDay(now) + ReportsRepository.DAY_MS
        }
        if (!ReportAggregator.withinLimit(desde, hasta)) {
            Toast.makeText(requireContext(), getString(com.multipos.app.R.string.reportes_rango_amplio), Toast.LENGTH_LONG).show()
            return
        }
        lastTipo = tipoSeleccionado()
        lastRange = EstadoCuentaFilters.RangoResult.Range(desde, hasta)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                lastData = ReportsRepository(db).compute(
                    companyId = companyId,
                    userId = userId,
                    desde = desde,
                    hastaExclusive = hasta,
                    tipo = lastTipo,
                    medioPago = medioSeleccionado(),
                    vendedorId = vendedorSeleccionado()
                )
                mostrar()
            } catch (e: ReportException) {
                val msg = when (e.message) {
                    ReportException.NOT_AUTHORIZED -> getString(com.multipos.app.R.string.reportes_sin_permiso)
                    ReportException.RANGE_TOO_WIDE -> getString(com.multipos.app.R.string.reportes_rango_amplio)
                    ReportException.INVALID_RANGE -> getString(com.multipos.app.R.string.reportes_fecha_invalida)
                    else -> getString(com.multipos.app.R.string.reportes_error)
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), getString(com.multipos.app.R.string.reportes_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrar() {
        val data = lastData ?: return
        binding.txtSummary.text = buildString {
            data.summary.totals.forEach { (cat, value) ->
                append(label(cat)).append(": ")
                append(formatImporte(cat, value)).append('\n')
            }
            if (ReportsRepository.FLAG_COSTO_APROXIMADO in data.flags) {
                append(getString(com.multipos.app.R.string.reportes_costo_aproximado)).append('\n')
            }
            if (data.summary.totals.isEmpty()) append(getString(com.multipos.app.R.string.reportes_vacio))
        }
        binding.txtEmpty.visibility = if (data.rows.isEmpty()) View.VISIBLE else View.GONE
        adapter.update(data.rows)
    }

    private fun exportar(csv: Boolean) {
        val data = lastData ?: run {
            Toast.makeText(requireContext(), getString(com.multipos.app.R.string.reportes_genera_primero), Toast.LENGTH_SHORT).show()
            return
        }
        val range = lastRange ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val company = db.empresaDao().getById(companyId)?.nombre ?: "MultiPOS"
            val desdeLabel = EstadoCuentaFilters.formatBoundary(range.desde) ?: getString(com.multipos.app.R.string.todos)
            val hastaLabel = EstadoCuentaFilters.formatBoundary(range.hastaExclusive - 1) ?: getString(com.multipos.app.R.string.todos)
            val reportName = tipoLabel(lastTipo)
            val reportKey = tipoKey(lastTipo)
            val nota = if (ReportsRepository.FLAG_COSTO_APROXIMADO in data.flags) {
                getString(com.multipos.app.R.string.reportes_costo_aproximado)
            } else null
            val file = withContext(Dispatchers.IO) {
                if (csv) {
                    ReportExport.exportCsv(
                        requireContext(), company, reportName, reportKey,
                        desdeLabel, hastaLabel, data.rows, data.summary
                    )
                } else {
                    ReportExport.exportPdf(
                        requireContext(), company, reportName, reportKey,
                        desdeLabel, hastaLabel, data.rows, data.summary, nota
                    )
                }
            }
            ReceiptPdfGenerator.share(requireContext(), file, getString(if (csv) com.multipos.app.R.string.export_csv else com.multipos.app.R.string.export_pdf))
        }
    }

    private fun formatImporte(categoria: String, value: Long): String =
        if (ReportsRepository.unidadDe(categoria) == Unidad.MONEDA) Money.format(value) else value.toString()

    private fun label(categoria: String): String = when (categoria) {
        ReportsRepository.CAT_CANTIDAD -> getString(com.multipos.app.R.string.reportes_cat_cantidad)
        ReportsRepository.CAT_VENTAS -> getString(com.multipos.app.R.string.reportes_cat_ventas)
        ReportsRepository.CAT_ANULADAS -> getString(com.multipos.app.R.string.reportes_cat_anuladas)
        ReportsRepository.CAT_ANULADAS_COUNT -> getString(com.multipos.app.R.string.reportes_cat_anuladas_count)
        ReportsRepository.CAT_BRUTO -> getString(com.multipos.app.R.string.reportes_cat_bruto)
        ReportsRepository.CAT_DESCUENTOS -> getString(com.multipos.app.R.string.reportes_cat_descuentos)
        ReportsRepository.CAT_IMPUESTOS -> getString(com.multipos.app.R.string.reportes_cat_impuestos)
        ReportsRepository.CAT_NETO -> getString(com.multipos.app.R.string.reportes_cat_neto)
        ReportsRepository.CAT_DEVOLUCIONES -> getString(com.multipos.app.R.string.reportes_cat_devoluciones)
        ReportsRepository.CAT_GANANCIA -> getString(com.multipos.app.R.string.reportes_cat_ganancia)
        ReportsRepository.CAT_COSTOS -> getString(com.multipos.app.R.string.reportes_cat_costos)
        ReportsRepository.CAT_INGRESO_NETO -> getString(com.multipos.app.R.string.reportes_cat_ingreso_neto)
        ReportsRepository.CAT_SESIONES -> getString(com.multipos.app.R.string.reportes_cat_sesiones)
        ReportsRepository.CAT_APERTURA -> getString(com.multipos.app.R.string.reportes_cat_apertura)
        ReportsRepository.CAT_INGRESOS -> getString(com.multipos.app.R.string.reportes_cat_ingresos)
        ReportsRepository.CAT_EGRESOS -> getString(com.multipos.app.R.string.reportes_cat_egresos)
        ReportsRepository.CAT_ESPERADO -> getString(com.multipos.app.R.string.reportes_cat_esperado)
        ReportsRepository.CAT_CONTADO -> getString(com.multipos.app.R.string.reportes_cat_contado)
        ReportsRepository.CAT_DIFERENCIA -> getString(com.multipos.app.R.string.reportes_cat_diferencia)
        ReportsRepository.CAT_STOCK_ACTUAL -> getString(com.multipos.app.R.string.reportes_cat_stock_actual)
        ReportsRepository.CAT_VALOR_COSTO -> getString(com.multipos.app.R.string.reportes_cat_valor_costo)
        ReportsRepository.CAT_STOCK_BAJO -> getString(com.multipos.app.R.string.reportes_cat_stock_bajo)
        ReportsRepository.CAT_ENTRADAS -> getString(com.multipos.app.R.string.reportes_cat_entradas)
        ReportsRepository.CAT_SALIDAS -> getString(com.multipos.app.R.string.reportes_cat_salidas)
        ReportsRepository.CAT_MOVIMIENTOS -> getString(com.multipos.app.R.string.reportes_cat_movimientos)
        ReportsRepository.CAT_VENTAS_CREDITO -> getString(com.multipos.app.R.string.reportes_cat_ventas_credito)
        ReportsRepository.CAT_ABONOS -> getString(com.multipos.app.R.string.reportes_cat_abonos)
        ReportsRepository.CAT_CREDITO_ANULACION -> getString(com.multipos.app.R.string.reportes_cat_anulacion_credito)
        ReportsRepository.CAT_CARTERA -> getString(com.multipos.app.R.string.reportes_cat_cartera)
        ReportsRepository.CAT_CLIENTES_SALDO -> getString(com.multipos.app.R.string.reportes_cat_clientes_saldo)
        else -> categoria
    }

    private fun tipoLabel(tipo: ReporteTipo): String = when (tipo) {
        ReporteTipo.VENTAS -> getString(com.multipos.app.R.string.reportes_tipo_ventas)
        ReporteTipo.RENTABILIDAD -> getString(com.multipos.app.R.string.reportes_tipo_rentabilidad)
        ReporteTipo.CAJA -> getString(com.multipos.app.R.string.reportes_tipo_caja)
        ReporteTipo.INVENTARIO -> getString(com.multipos.app.R.string.reportes_tipo_inventario)
        ReporteTipo.CREDITO -> getString(com.multipos.app.R.string.reportes_tipo_credito)
    }

    private fun tipoKey(tipo: ReporteTipo): String = when (tipo) {
        ReporteTipo.VENTAS -> "ventas"
        ReporteTipo.RENTABILIDAD -> "rentabilidad"
        ReporteTipo.CAJA -> "caja"
        ReporteTipo.INVENTARIO -> "inventario"
        ReporteTipo.CREDITO -> "credito"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}