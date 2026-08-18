package com.multipos.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.multipos.app.R
import com.multipos.app.data.*
import com.multipos.app.data.entities.Venta
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.ui.components.MultiPOSTextField
import com.multipos.app.ui.history.compose.SaleDetailScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import com.multipos.app.viewmodel.SaleDetailViewModel
import com.multipos.app.viewmodel.SaleDetailViewModelFactory
import kotlinx.coroutines.launch

class SaleDetailFragment : Fragment() {

    private lateinit var viewModel: SaleDetailViewModel
    private var saleId: Int = 0

    private var showAnnulDialog by mutableStateOf(false)
    private var showRefundDialog by mutableStateOf(false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        saleId = arguments?.getInt(ARG_SALE_ID) ?: 0
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            viewLifecycleOwner.lifecycleScope.launch {
                val role = ActiveCompanyAccess.role(requireContext(), db)
                val factory = SaleDetailViewModelFactory(db, saleId, companyId, role)
                viewModel = ViewModelProvider(this@SaleDetailFragment, factory).get(SaleDetailViewModel::class.java)
                
                setContent {
                    MultiPOSTheme {
                        val state by viewModel.uiState.collectAsState()
                        
                        SaleDetailScreen(
                            sale = state.sale,
                            details = state.details,
                            refunds = state.refunds,
                            vendedorName = state.vendedorName,
                            clienteName = state.clienteName,
                            canManageReturns = state.canManageReturns,
                            onBackClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                            onAnnulClick = { showAnnulDialog = true },
                            onRefundClick = { showRefundDialog = true }
                        )

                        if (showAnnulDialog) {
                            AnnulDialog(
                                sale = state.sale,
                                onDismiss = { showAnnulDialog = false },
                                onConfirm = { motivo, confirmed ->
                                    annulSale(motivo, confirmed)
                                }
                            )
                        }

                        if (showRefundDialog) {
                            RefundDialog(
                                sale = state.sale,
                                details = state.details,
                                onDismiss = { showRefundDialog = false },
                                onConfirm = { motivo, confirmed, lines ->
                                    refundSale(motivo, confirmed, lines)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AnnulDialog(
        sale: Venta?,
        onDismiss: () -> Unit,
        onConfirm: (String, Boolean) -> Unit
    ) {
        var motivo by remember { mutableStateOf("") }
        var confirmed by remember { mutableStateOf(false) }
        val isExternal = sale?.tipoPago == "TARJETA" || sale?.tipoPago == "TRANSFERENCIA"

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Anular Venta") },
            confirmButton = {
                TextButton(onClick = {
                    if (motivo.length >= 5 && (!isExternal || confirmed)) {
                        onConfirm(motivo, confirmed)
                    } else {
                        Toast.makeText(requireContext(), "Verifique los datos", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Confirmar Anulación") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("¿Estás seguro de anular esta venta? Esta acción no se puede deshacer.")
                    MultiPOSTextField(value = motivo, onValueChange = { motivo = it }, label = "Motivo (mín. 5 car.)")
                    
                    if (isExternal) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                            Text("Confirmo que el reembolso externo fue procesado", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun RefundDialog(
        sale: Venta?,
        details: List<com.multipos.app.data.entities.DetalleVenta>,
        onDismiss: () -> Unit,
        onConfirm: (String, Boolean, List<RefundLineRequest>) -> Unit
    ) {
        var motivo by remember { mutableStateOf("") }
        var confirmed by remember { mutableStateOf(false) }
        val isExternal = sale?.tipoPago == "TARJETA" || sale?.tipoPago == "TRANSFERENCIA"
        
        val refundQuantities = remember { mutableStateMapOf<Int, String>() }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Registrar Devolución") },
            confirmButton = {
                TextButton(onClick = {
                    val lines = refundQuantities.mapNotNull { (id, qty) ->
                        val q = qty.toIntOrNull() ?: 0
                        if (q > 0) RefundLineRequest(id, q) else null
                    }
                    if (motivo.length >= 5 && lines.isNotEmpty() && (!isExternal || confirmed)) {
                        onConfirm(motivo, confirmed, lines)
                    } else {
                        Toast.makeText(requireContext(), "Verifique los datos", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Guardar Devolución") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MultiPOSTextField(value = motivo, onValueChange = { motivo = it }, label = "Motivo (mín. 5 car.)")
                    
                    if (isExternal) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                            Text("Confirmo que el reembolso externo fue procesado", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Text("Productos a devolver:", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    details.forEach { detail ->
                        // Aquí idealmente filtraríamos los ya devueltos, pero por ahora mostramos todos
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(detail.nombreProductoSnapshot, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = refundQuantities[detail.id] ?: "",
                                onValueChange = { if (it.isEmpty() || (it.toIntOrNull() ?: 0) <= detail.cantidad) refundQuantities[detail.id] = it },
                                label = { Text("Cant (máx ${detail.cantidad})") },
                                modifier = Modifier.width(120.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    }
                }
            }
        )
    }

    private fun annulSale(motivo: String, externalConfirmed: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ReturnRepository(DatabaseProvider.get(requireContext())).annulSale(
                    AnnulSaleRequest(
                        companyId = ActiveCompanyStore.get(requireContext()),
                        saleId = saleId,
                        userId = UserSessionStore.userId(requireContext()),
                        motivo = motivo,
                        externalRefundConfirmed = externalConfirmed
                    )
                )
                showAnnulDialog = false
                Toast.makeText(context, "Venta anulada", Toast.LENGTH_SHORT).show()
                viewModel.loadData()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refundSale(motivo: String, externalConfirmed: Boolean, lines: List<RefundLineRequest>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ReturnRepository(DatabaseProvider.get(requireContext())).refundSale(
                    RefundSaleRequest(
                        companyId = ActiveCompanyStore.get(requireContext()),
                        saleId = saleId,
                        userId = UserSessionStore.userId(requireContext()),
                        motivo = motivo,
                        externalRefundConfirmed = externalConfirmed,
                        lines = lines
                    )
                )
                showRefundDialog = false
                Toast.makeText(context, "Devolución registrada", Toast.LENGTH_SHORT).show()
                viewModel.loadData()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val ARG_SALE_ID = "saleId"
        fun newInstance(saleId: Int) = SaleDetailFragment().apply {
            arguments = Bundle().apply { putInt(ARG_SALE_ID, saleId) }
        }
    }
}
