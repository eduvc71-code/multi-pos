package com.multipos.app.ui.history

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.multipos.app.R
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.AnnulSaleRequest
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.RefundCalculator
import com.multipos.app.data.RefundLineRequest
import com.multipos.app.data.RefundSaleRequest
import com.multipos.app.data.ReturnException
import com.multipos.app.data.ReturnRepository
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Venta
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.ui.history.compose.SaleDetailScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import com.multipos.app.viewmodel.SaleDetailViewModel
import com.multipos.app.viewmodel.SaleDetailViewModelFactory
import kotlinx.coroutines.launch

class SaleDetailFragment : Fragment() {

    private lateinit var viewModel: SaleDetailViewModel
    private var saleId: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        saleId = arguments?.getInt(ARG_SALE_ID) ?: 0
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            // Inicialización diferida del ViewModel con el rol
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
                            onAnnulClick = { showAnnulDialog() },
                            onRefundClick = { showRefundDialog() }
                        )
                    }
                }
            }
        }
    }

    private fun showAnnulDialog() {
        val sale = viewModel.uiState.value.sale ?: return
        val context = requireContext()
        val motivoInput = EditText(context).apply {
            hint = getString(R.string.sale_detail_motivo_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
        }
        val external = sale.tipoPago == PAYMENT_CARD || sale.tipoPago == PAYMENT_TRANSFER
        val externalCheck = if (external) {
            CheckBox(context).apply { text = getString(R.string.sale_detail_external_confirm) }
        } else {
            null
        }
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
            addView(motivoInput)
            externalCheck?.let(::addView)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.sale_detail_annul_title)
            .setMessage(R.string.sale_detail_annul_message)
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(R.string.sale_detail_annul_confirm, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val motivo = motivoInput.text.toString().trim()
                if (motivo.length !in 5..300) {
                    Toast.makeText(context, R.string.sale_detail_motivo_hint, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (external && externalCheck?.isChecked != true) {
                    Toast.makeText(context, R.string.sale_detail_external_confirm, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                confirmButton.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        ReturnRepository(DatabaseProvider.get(requireContext())).annulSale(
                            AnnulSaleRequest(
                                companyId = ActiveCompanyStore.get(requireContext()),
                                saleId = saleId,
                                userId = UserSessionStore.userId(requireContext()),
                                motivo = motivo,
                                externalRefundConfirmed = externalCheck?.isChecked == true
                            )
                        )
                        dialog.dismiss()
                        Toast.makeText(context, R.string.sale_detail_annulled, Toast.LENGTH_SHORT).show()
                        viewModel.loadData()
                    } catch (error: ReturnException) {
                        confirmButton.isEnabled = true
                        Toast.makeText(context, error.message ?: "Error al anular", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showRefundDialog() {
        val sale = viewModel.uiState.value.sale ?: return
        val details = viewModel.uiState.value.details
        val context = requireContext()
        val db = DatabaseProvider.get(context)
        
        viewLifecycleOwner.lifecycleScope.launch {
            val returnedByDetail = details.associate { detail ->
                detail.id to db.devolucionDao().returnedQuantity(sale.empresaId, sale.id, detail.id)
            }
            
            val previousRefundSubtotal = viewModel.uiState.value.refunds.sumOf { refund ->
                db.devolucionDao().getDetails(refund.id).sumOf { it.subtotal }
            }

            val motivoInput = EditText(context).apply {
                hint = getString(R.string.sale_detail_motivo_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 2
            }
            val inputsByDetail = LinkedHashMap<Int, EditText>()
            val form = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 16, 48, 16)
                addView(motivoInput)
            }
            
            for (detail in details) {
                val remaining = detail.cantidad - (returnedByDetail[detail.id] ?: 0)
                if (remaining <= 0) continue
                val label = TextView(context).apply {
                    text = "${detail.nombreProductoSnapshot.ifBlank { "Producto" }} (${remaining} disp.)"
                    textSize = 14f
                    setPadding(0, 8, 0, 4)
                }
                val input = EditText(context).apply {
                    hint = "Cantidad a devolver"
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(remaining.toString())
                }
                form.addView(label)
                form.addView(input)
                inputsByDetail[detail.id] = input
            }
            
            if (inputsByDetail.isEmpty()) {
                Toast.makeText(context, "No hay productos disponibles para devolver", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val external = sale.tipoPago == PAYMENT_CARD || sale.tipoPago == PAYMENT_TRANSFER
            val externalCheck = if (external) {
                CheckBox(context).apply { text = getString(R.string.sale_detail_external_confirm) }
            } else {
                null
            }
            externalCheck?.let(form::addView)

            val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.sale_detail_refund_title)
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(R.string.sale_detail_refund_confirm, null)
                .create()
                
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val motivo = motivoInput.text.toString().trim()
                    if (motivo.length !in 5..300) {
                        Toast.makeText(context, R.string.sale_detail_motivo_hint, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val quantities = inputsByDetail.mapValues { (_, input) -> input.text.toString().toIntOrNull() ?: 0 }
                    val lines = quantities.mapNotNull { (detailId, quantity) ->
                        if (quantity > 0) RefundLineRequest(detailId, quantity) else null
                    }
                    
                    if (lines.isEmpty()) {
                        Toast.makeText(context, "Ingresa al menos una cantidad", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (external && externalCheck?.isChecked != true) {
                        Toast.makeText(context, R.string.sale_detail_external_confirm, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    confirmButton.isEnabled = false
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            ReturnRepository(db).refundSale(
                                RefundSaleRequest(
                                    companyId = sale.empresaId,
                                    saleId = sale.id,
                                    userId = UserSessionStore.userId(context),
                                    motivo = motivo,
                                    externalRefundConfirmed = externalCheck?.isChecked == true,
                                    lines = lines
                                )
                            )
                            dialog.dismiss()
                            Toast.makeText(context, R.string.sale_detail_refunded, Toast.LENGTH_SHORT).show()
                            viewModel.loadData()
                        } catch (e: Exception) {
                            confirmButton.isEnabled = true
                            Toast.makeText(context, e.message ?: "Error al procesar", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            dialog.show()
        }
    }

    companion object {
        const val ARG_SALE_ID = "saleId"
        const val PAYMENT_CARD = "TARJETA"
        const val PAYMENT_TRANSFER = "TRANSFERENCIA"

        fun newInstance(saleId: Int) = SaleDetailFragment().apply {
            arguments = Bundle().apply { putInt(ARG_SALE_ID, saleId) }
        }
    }
}
