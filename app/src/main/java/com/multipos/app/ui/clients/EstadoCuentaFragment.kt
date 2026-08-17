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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.CreditRepository
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.RegisterAbonoRequest
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Abono
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.MovimientoCredito
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import com.multipos.app.ui.clients.compose.EstadoCuentaScreen
import com.multipos.app.ui.clients.compose.MovimientoRow
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.EstadoCuentaExport
import com.multipos.app.util.EstadoCuentaFilters
import com.multipos.app.util.Money
import com.multipos.app.util.ReceiptPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EstadoCuentaFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var companyId: String
    private var clientId: Int = 0

    private val clienteState = MutableStateFlow<Cliente?>(null)
    private val movimientosState = MutableStateFlow<List<MovimientoRow>>(emptyList())
    private var desdeState by mutableStateOf("")
    private var hastaState by mutableStateOf("")
    private var canRegisterAbono by mutableStateOf(false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        db = DatabaseProvider.get(requireContext())
        companyId = arguments?.getString(ARG_COMPANY) ?: ActiveCompanyStore.get(requireContext())
        clientId = arguments?.getInt(ARG_CLIENT, 0) ?: 0

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val cliente by clienteState.collectAsState()
                    val movimientos by movimientosState.collectAsState()
                    
                    EstadoCuentaScreen(
                        cliente = cliente,
                        movimientos = movimientos,
                        desde = desdeState,
                        hasta = hastaState,
                        onDesdeChange = { desdeState = it },
                        onHastaChange = { hastaState = it },
                        onFiltrarClick = { reload() },
                        onRegistrarAbonoClick = { showRegisterAbonoDialog() },
                        onExportCsvClick = { export(true) },
                        onExportPdfClick = { export(false) },
                        onBackClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        canRegisterAbono = canRegisterAbono
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            canRegisterAbono = ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.MANAGE_CLIENT_CREDIT)
            reload()
        }
    }

    private fun reload() {
        viewLifecycleOwner.lifecycleScope.launch {
            val range = EstadoCuentaFilters.parseDateRange(desdeState, hastaState)
            if (range is EstadoCuentaFilters.RangoResult.Invalid) {
                Toast.makeText(requireContext(), "Fecha inválida", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val client = db.clienteDao().getByIdIncludingInactive(clientId, companyId)
            if (client == null) {
                Toast.makeText(requireContext(), "El cliente no existe", Toast.LENGTH_SHORT).show()
                return@launch
            }
            clienteState.value = client
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
            
            movimientosState.value = movements.map { m ->
                MovimientoRow(
                    fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(m.fecha)),
                    tipo = tipoLabel(m),
                    importe = Money.format(m.importeFirmado),
                    saldoPosterior = "Saldo: ${Money.format(m.saldoPosterior)}",
                    usuario = userNames[m.usuarioId] ?: "#${m.usuarioId}",
                    isNegativo = m.importeFirmado < 0
                )
            }
        }
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
            val range = EstadoCuentaFilters.parseDateRange(desdeState, hastaState)
            val movements = try {
                when (range) {
                    is EstadoCuentaFilters.RangoResult.Range -> CreditRepository(db).estadoDeCuenta(companyId, clientId, UserSessionStore.userId(requireContext()), range.desde, range.hastaExclusive)
                    else -> CreditRepository(db).estadoDeCuenta(companyId, clientId, UserSessionStore.userId(requireContext()))
                }
            } catch (_: Exception) { return@launch }

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
            val client = clienteState.value
            val company = db.empresaDao().getById(companyId)?.nombre ?: "MultiPOS"
            val desdeLabel = if (range is EstadoCuentaFilters.RangoResult.Range) EstadoCuentaFilters.formatBoundary(range.desde) ?: "Inicio" else "Inicio"
            val hastaLabel = if (range is EstadoCuentaFilters.RangoResult.Range) EstadoCuentaFilters.formatBoundary(range.hastaExclusive - 1) ?: "Hoy" else "Hoy"
            
            val file = withContext(Dispatchers.IO) {
                if (isCsv) EstadoCuentaExport.exportCsv(requireContext(), company, client?.nombre ?: "", desdeLabel, hastaLabel, rows)
                else EstadoCuentaExport.exportPdf(requireContext(), company, client?.nombre ?: "", desdeLabel, hastaLabel, rows)
            }
            ReceiptPdfGenerator.share(requireContext(), file, "Estado de Cuenta")
        }
    }

    private fun showRegisterAbonoDialog() {
        val context = requireContext()
        val amount = EditText(context).apply {
            hint = "Monto"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(32, 16, 32, 16)
        }
        val note = EditText(context).apply {
            hint = "Nota (opcional)"
            setPadding(32, 16, 32, 16)
        }
        val paymentSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listOf(Abono.MEDIO_EFECTIVO, Abono.MEDIO_TARJETA, Abono.MEDIO_TRANSFERENCIA))
        }
        val confirmCheckbox = android.widget.CheckBox(context).apply {
            text = "Confirmar cobro externo"
            visibility = View.GONE
        }
        paymentSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                confirmCheckbox.visibility = if (position == 0) View.GONE else View.VISIBLE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            addView(paymentSpinner); addView(confirmCheckbox); addView(amount); addView(note)
        }
        
        AlertDialog.Builder(context)
            .setTitle("Registrar Abono")
            .setView(form)
            .setPositiveButton("Guardar") { _, _ ->
                val value = Money.parseMinorUnits(amount.text.toString())
                if (value == null || value <= 0) return@setPositiveButton
                
                val medioPago = when (paymentSpinner.selectedItemPosition) {
                    1 -> Abono.MEDIO_TARJETA
                    2 -> Abono.MEDIO_TRANSFERENCIA
                    else -> Abono.MEDIO_EFECTIVO
                }
                
                if (medioPago != Abono.MEDIO_EFECTIVO && !confirmCheckbox.isChecked) {
                    Toast.makeText(context, "Confirma el cobro externo", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
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
                        Toast.makeText(context, "Abono registrado", Toast.LENGTH_SHORT).show()
                        reload()
                    } catch (_: Exception) {
                        Toast.makeText(context, "Error al abonar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        private const val ARG_CLIENT = "clientId"
        private const val ARG_COMPANY = "companyId"
        fun newInstance(clientId: Int, companyId: String) = EstadoCuentaFragment().apply {
            arguments = Bundle().apply { putInt(ARG_CLIENT, clientId); putString(ARG_COMPANY, companyId) }
        }
    }
}
