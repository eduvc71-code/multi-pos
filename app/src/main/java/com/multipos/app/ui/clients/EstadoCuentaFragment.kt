package com.multipos.app.ui.clients

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.multipos.app.adapters.MovimientoCreditoAdapter
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.CreditRepository
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.RegisterAbonoRequest
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Abono
import com.multipos.app.data.entities.MovimientoCredito
import com.multipos.app.databinding.FragmentEstadoCuentaBinding
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import com.multipos.app.util.EstadoCuentaExport
import com.multipos.app.util.EstadoCuentaFilters
import com.multipos.app.util.Money
import com.multipos.app.util.ReceiptPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EstadoCuentaFragment : Fragment() {

    private var _binding: FragmentEstadoCuentaBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var companyId: String
    private var clientId: Int = 0
    private val adapter = MovimientoCreditoAdapter(emptyList())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEstadoCuentaBinding.inflate(inflater, container, false)
        db = DatabaseProvider.get(requireContext())
        companyId = arguments?.getString(ARG_COMPANY) ?: ActiveCompanyStore.get(requireContext())
        clientId = arguments?.getInt(ARG_CLIENT, 0) ?: 0
        binding.recyclerMovimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMovimientos.adapter = adapter
        binding.btnFiltrar.setOnClickListener { reload() }
        binding.btnRegistrarAbono.setOnClickListener { showRegisterAbonoDialog() }
        binding.btnExportarCSV.setOnClickListener { export(true) }
        binding.btnExportarPDF.setOnClickListener { export(false) }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            val canRegister = ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.MANAGE_CLIENT_CREDIT)
            binding.btnRegistrarAbono.visibility = if (canRegister) View.VISIBLE else View.GONE
            reload()
        }
    }

    private fun reload() {
        viewLifecycleOwner.lifecycleScope.launch {
            val range = EstadoCuentaFilters.parseDateRange(binding.etDesde.text.toString(), binding.etHasta.text.toString())
            if (range is EstadoCuentaFilters.RangoResult.Invalid) {
                Toast.makeText(requireContext(), "Fecha inválida", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val client = db.clienteDao().getByIdIncludingInactive(clientId, companyId)
            if (client == null) {
                Toast.makeText(requireContext(), "El cliente no existe", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val userId = UserSessionStore.userId(requireContext())
            val movements = try {
                when (range) {
                    is EstadoCuentaFilters.RangoResult.Range -> CreditRepository(db).estadoDeCuenta(companyId, clientId, userId, range.desde, range.hastaExclusive)
                    else -> CreditRepository(db).estadoDeCuenta(companyId, clientId, userId)
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "No se pudo consultar el estado de cuenta", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val userNames = movements.mapNotNull { it.usuarioId }.distinct()
                .associateWith { db.usuarioDao().getById(it)?.nombre ?: "#$it" }
            binding.txtLimite.text = getString(com.multipos.app.R.string.cliente_limite_format, Money.format(client.limiteCredito))
            binding.txtDeuda.text = getString(com.multipos.app.R.string.cliente_deuda_format, Money.format(client.creditoActual))
            binding.txtDisponible.text = getString(com.multipos.app.R.string.cliente_disponible_format, Money.format(client.creditoDisponible))
            binding.txtEstado.text = getString(com.multipos.app.R.string.cliente_estado_format, client.estadoCredito)
            bindRows(movements, userNames)
        }
    }

    private fun bindRows(movements: List<MovimientoCredito>, userNames: Map<Int, String>) {
        val rows = movements.map { m ->
            MovimientoCreditoAdapter.Row(
                fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(m.fecha)),
                importe = m.importeFirmado,
                tipoReferencia = tipoLabel(m),
                saldoPosterior = "Saldo ${Money.format(m.saldoPosterior)}",
                usuario = userNames[m.usuarioId] ?: "#${m.usuarioId}"
            )
        }
        binding.txtEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerMovimientos.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
        adapter.update(rows)
    }

    private fun tipoLabel(m: MovimientoCredito): String {
        val ref = m.ventaId ?: m.abonoId?.toInt() ?: m.devolucionId?.toInt()
        val tipo = when (m.tipo) {
            MovimientoCredito.TIPO_VENTA_CREDITO -> getString(com.multipos.app.R.string.tipo_venta_credito)
            MovimientoCredito.TIPO_ABONO -> getString(com.multipos.app.R.string.tipo_abono)
            MovimientoCredito.TIPO_DEVOLUCION -> getString(com.multipos.app.R.string.tipo_devolucion)
            MovimientoCredito.TIPO_ANULACION -> getString(com.multipos.app.R.string.tipo_anulacion)
            else -> m.tipo
        }
        return if (ref != null) "$tipo ${getString(com.multipos.app.R.string.referencia_format, ref)}" else tipo
    }

    private fun export(isCsv: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val range = EstadoCuentaFilters.parseDateRange(binding.etDesde.text.toString(), binding.etHasta.text.toString())
            if (range is EstadoCuentaFilters.RangoResult.Invalid) {
                Toast.makeText(requireContext(), "Fecha inválida", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val movements = try {
                when (range) {
                    is EstadoCuentaFilters.RangoResult.Range -> CreditRepository(db).estadoDeCuenta(companyId, clientId, UserSessionStore.userId(requireContext()), range.desde, range.hastaExclusive)
                    else -> CreditRepository(db).estadoDeCuenta(companyId, clientId, UserSessionStore.userId(requireContext()))
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "No se pudo exportar", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val userNames = movements.mapNotNull { it.usuarioId }.distinct()
                .associateWith { db.usuarioDao().getById(it)?.nombre ?: "#$it" }
            val rows = movements.map { m ->
                EstadoCuentaExport.Row(
                    fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(m.fecha)),
                    tipo = tipoLabel(m),
                    importeFirmado = m.importeFirmado,
                    saldoPosterior = m.saldoPosterior,
                    usuario = userNames[m.usuarioId] ?: "#${m.usuarioId}"
                )
            }
            val client = db.clienteDao().getByIdIncludingInactive(clientId, companyId)
            val company = db.empresaDao().getById(companyId)?.nombre ?: "MultiPOS"
            val desdeLabel = if (range is EstadoCuentaFilters.RangoResult.Range) {
                EstadoCuentaFilters.formatBoundary(range.desde) ?: getString(com.multipos.app.R.string.todos)
            } else getString(com.multipos.app.R.string.todos)
            val hastaLabel = if (range is EstadoCuentaFilters.RangoResult.Range) {
                EstadoCuentaFilters.formatBoundary(range.hastaExclusive - 1) ?: getString(com.multipos.app.R.string.todos)
            } else getString(com.multipos.app.R.string.todos)
            val file = withContext(Dispatchers.IO) {
                if (isCsv) EstadoCuentaExport.exportCsv(requireContext(), company, client?.nombre ?: "", desdeLabel, hastaLabel, rows)
                else EstadoCuentaExport.exportPdf(requireContext(), company, client?.nombre ?: "", desdeLabel, hastaLabel, rows)
            }
            ReceiptPdfGenerator.share(requireContext(), file, getString(if (isCsv) com.multipos.app.R.string.estado_cuenta_export_csv else com.multipos.app.R.string.estado_cuenta_export_pdf))
        }
    }

    private fun showRegisterAbonoDialog() {
        val context = requireContext()
        val amount = EditText(context).apply {
            hint = context.getString(com.multipos.app.R.string.abono_monto_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(24, 12, 24, 12)
        }
        val note = EditText(context).apply {
            hint = context.getString(com.multipos.app.R.string.abono_nota_hint)
            setPadding(24, 12, 24, 12)
        }
        val paymentSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listOf(Abono.MEDIO_EFECTIVO, Abono.MEDIO_TARJETA, Abono.MEDIO_TRANSFERENCIA))
        }
        val confirmCheckbox = android.widget.CheckBox(context).apply {
            text = context.getString(com.multipos.app.R.string.confirm_external_payment)
            isChecked = false
        }
        paymentSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                confirmCheckbox.visibility = if (position == 0) View.GONE else View.VISIBLE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        confirmCheckbox.visibility = View.GONE
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(paymentSpinner); addView(confirmCheckbox); addView(amount); addView(note)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(com.multipos.app.R.string.register_payment)
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = Money.parseMinorUnits(amount.text.toString())
                if (value == null || value <= 0) {
                    Toast.makeText(context, "Indica un monto válido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val medioPago = when (paymentSpinner.selectedItemPosition) {
                    1 -> Abono.MEDIO_TARJETA
                    2 -> Abono.MEDIO_TRANSFERENCIA
                    else -> Abono.MEDIO_EFECTIVO
                }
                if (medioPago != Abono.MEDIO_EFECTIVO && !confirmCheckbox.isChecked) {
                    Toast.makeText(context, "Confirma el cobro externo para continuar", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val result = CreditRepository(db).registerAbono(
                            RegisterAbonoRequest(
                                companyId = companyId,
                                clientId = clientId,
                                userId = UserSessionStore.userId(context),
                                monto = value,
                                medioPago = medioPago,
                                nota = note.text.toString().trim(),
                                externalPaymentConfirmed = confirmCheckbox.isChecked
                            )
                        )
                        val company = db.empresaDao().getById(companyId)?.nombre ?: "MultiPOS"
                        val clientName = db.clienteDao().getById(clientId, companyId)?.nombre ?: "Cliente"
                        val receipt = withContext(Dispatchers.IO) {
                            ReceiptPdfGenerator.createPayment(context, company, clientName, value, result.saldoAnterior, result.saldoNuevo)
                        }
                        dialog.dismiss()
                        Toast.makeText(context, "Abono registrado", Toast.LENGTH_SHORT).show()
                        ReceiptPdfGenerator.share(context, receipt, "Comprobante de abono")
                        reload()
                    } catch (_: Exception) {
                        Toast.makeText(context, "No se pudo registrar el abono", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    companion object {
        private const val ARG_CLIENT = "clientId"
        private const val ARG_COMPANY = "companyId"

        fun newInstance(clientId: Int, companyId: String) = EstadoCuentaFragment().apply {
            arguments = Bundle().apply { putInt(ARG_CLIENT, clientId); putString(ARG_COMPANY, companyId) }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}