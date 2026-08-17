package com.multipos.app.ui.history

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
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
import com.multipos.app.data.entities.DetalleVenta
import com.multipos.app.data.entities.Devolucion
import com.multipos.app.data.entities.Venta
import com.multipos.app.databinding.FragmentSaleDetailBinding
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions
import com.multipos.app.util.Money
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaleDetailFragment : Fragment() {

    private var _binding: FragmentSaleDetailBinding? = null
    private val binding get() = _binding!!

    private var saleId: Int = 0
    private var canManageReturns = false
    private var currentSale: Venta? = null
    private var details: List<DetalleVenta> = emptyList()
    private var refunds: List<Devolucion> = emptyList()
    private var refundDetailsByRefund: Map<Long, List<com.multipos.app.data.entities.DetalleDevolucion>> = emptyMap()
    private var returnedByDetail: Map<Int, Int> = emptyMap()
    private var previousRefundSubtotal = 0L
    private var previousRefundMonto = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSaleDetailBinding.inflate(inflater, container, false)
        saleId = arguments?.getInt(ARG_SALE_ID) ?: 0
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnAnnulSale.setOnClickListener { showAnnulDialog() }
        binding.btnRefundSale.setOnClickListener { showRefundDialog() }
        loadSale()
        return binding.root
    }

    private fun loadSale() {
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val role = ActiveCompanyAccess.role(requireContext(), db)
            canManageReturns = CompanyPermissions.allows(role, CompanyPermission.MANAGE_RETURNS)
            if (!CompanyPermissions.allows(role, CompanyPermission.VIEW_HISTORY)) {
                Toast.makeText(requireContext(), "No tienes permiso para ver el historial", Toast.LENGTH_LONG).show()
                return@launch
            }
            val sale = db.ventaDao().getById(saleId, companyId)
            if (sale == null) {
                Toast.makeText(requireContext(), R.string.sale_detail_not_found, Toast.LENGTH_LONG).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
                return@launch
            }
            currentSale = sale
            details = db.ventaDao().getDetails(saleId, companyId)
            refunds = db.devolucionDao().getBySale(companyId, saleId)
            refundDetailsByRefund = refunds.associate { refund ->
                refund.id to db.devolucionDao().getDetails(refund.id)
            }
            returnedByDetail = details.associate { detail ->
                detail.id to db.devolucionDao().returnedQuantity(companyId, saleId, detail.id)
            }
            previousRefundSubtotal = refundDetailsByRefund.values.flatten().sumOf { it.subtotal }
            previousRefundMonto = refunds.sumOf { it.monto }
            renderSale(db, companyId)
        }
    }

    private suspend fun renderSale(db: com.multipos.app.data.AppDatabase, companyId: String) {
        val sale = currentSale ?: return
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(sale.fecha))
        binding.txtSaleId.text = getString(R.string.sale_detail_id_fecha, sale.id, date)
        val vendedor = db.usuarioDao().getById(sale.idUsuario)?.nombre ?: "—"
        val cliente = sale.idCliente?.let { db.clienteDao().getByIdIncludingInactive(it, companyId)?.nombre }
            ?: getString(R.string.sale_detail_no_client)
        binding.txtSaleMeta.text = buildString {
            append("${sale.tipoPago} · ${sale.estado}\n")
            append(getString(R.string.sale_detail_customer, cliente)).append("\n")
            append(getString(R.string.sale_detail_seller, vendedor))
        }
        binding.txtSaleTotals.text = getString(
            R.string.sale_detail_totals_format,
            Money.format(sale.subtotal),
            Money.format(sale.descuento),
            Money.format(sale.impuesto),
            Money.format(sale.total)
        )
        binding.txtSaleLines.text = details.joinToString("\n") { detail ->
            getString(
                R.string.sale_detail_line_format,
                detail.nombreProductoSnapshot.ifBlank { "Producto #${detail.idProducto}" },
                detail.cantidad,
                Money.format(detail.subtotal)
            )
        }
        binding.txtSaleRefunds.text = if (refunds.isEmpty()) {
            getString(R.string.sale_detail_no_refunds)
        } else {
            refunds.joinToString("\n") { refund ->
                getString(
                    R.string.sale_detail_refund_format,
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(refund.fecha)),
                    Money.format(refund.monto),
                    "${refund.medioReembolso} · ${refund.estadoReembolso}"
                )
            }
        }

        val completed = sale.estado == Venta.ESTADO_COMPLETADA
        binding.btnAnnulSale.visibility =
            if (canManageReturns && completed && refunds.isEmpty()) View.VISIBLE else View.GONE
        val hasRemaining = details.any { (it.cantidad - (returnedByDetail[it.id] ?: 0)) > 0 }
        binding.btnRefundSale.visibility =
            if (canManageReturns && completed && hasRemaining) View.VISIBLE else View.GONE
    }

    private fun showAnnulDialog() {
        val sale = currentSale ?: return
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
                        loadSale()
                    } catch (error: ReturnException) {
                        confirmButton.isEnabled = true
                        Toast.makeText(context, errorMessage(error), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showRefundDialog() {
        val sale = currentSale ?: return
        val context = requireContext()
        val motivoInput = EditText(context).apply {
            hint = getString(R.string.sale_detail_motivo_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
        }
        val inputsByDetail = LinkedHashMap<Int, EditText>()
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(motivoInput)
        }
        for (detail in details) {
            val remaining = detail.cantidad - (returnedByDetail[detail.id] ?: 0)
            if (remaining <= 0) continue
            val label = TextView(context).apply {
                text = getString(
                    R.string.sale_detail_remaining_format,
                    detail.nombreProductoSnapshot.ifBlank { "Producto #${detail.idProducto}" },
                    detail.cantidad,
                    remaining
                )
                textSize = 14f
            }
            val input = EditText(context).apply {
                hint = getString(R.string.sale_detail_refund_quantity_hint, remaining)
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(remaining.toString())
            }
            form.addView(label)
            form.addView(input)
            inputsByDetail[detail.id] = input
        }
        if (inputsByDetail.isEmpty()) return

        val external = sale.tipoPago == PAYMENT_CARD || sale.tipoPago == PAYMENT_TRANSFER
        val externalCheck = if (external) {
            CheckBox(context).apply { text = getString(R.string.sale_detail_external_confirm) }
        } else {
            null
        }
        externalCheck?.let(form::addView)
        val preview = TextView(context).apply {
            textSize = 15f
            setPadding(0, 12, 0, 0)
        }
        form.addView(preview)

        val updatePreview = {
            val quantities = inputsByDetail.mapValues { (_, input) -> input.text.toString().toIntOrNull() }
            val computation = buildComputation(quantities)
            preview.text = if (computation == null) {
                ""
            } else {
                getString(R.string.sale_detail_refund_preview, Money.format(computation.refundMonto))
            }
        }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updatePreview()
            override fun afterTextChanged(s: Editable?) = Unit
        }
        inputsByDetail.values.forEach { it.addTextChangedListener(watcher) }
        updatePreview()

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
                    Toast.makeText(context, "Ingresa al menos una cantidad a devolver", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (buildComputation(quantities) == null) {
                    Toast.makeText(context, "Revisa las cantidades a devolver", Toast.LENGTH_SHORT).show()
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
                        ReturnRepository(DatabaseProvider.get(requireContext())).refundSale(
                            RefundSaleRequest(
                                companyId = ActiveCompanyStore.get(requireContext()),
                                saleId = saleId,
                                userId = UserSessionStore.userId(requireContext()),
                                motivo = motivo,
                                externalRefundConfirmed = externalCheck?.isChecked == true,
                                lines = lines
                            )
                        )
                        dialog.dismiss()
                        Toast.makeText(context, R.string.sale_detail_refunded, Toast.LENGTH_SHORT).show()
                        loadSale()
                    } catch (error: ReturnException) {
                        confirmButton.isEnabled = true
                        Toast.makeText(context, errorMessage(error), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun buildComputation(quantities: Map<Int, Int?>): RefundCalculator.RefundComputation? {
        val sale = currentSale ?: return null
        if (quantities.values.none { it != null && it > 0 }) return null
        var refundSubtotal = 0L
        for ((detailId, quantity) in quantities) {
            val qty = quantity ?: continue
            if (qty <= 0) continue
            val detail = details.firstOrNull { it.id == detailId } ?: return null
            val remaining = detail.cantidad - (returnedByDetail[detailId] ?: 0)
            if (qty > remaining) return null
            refundSubtotal = Math.addExact(
                refundSubtotal,
                Math.multiplyExact(qty.toLong(), detail.precioUnitario)
            )
        }
        if (refundSubtotal <= 0) return null
        return runCatching {
            RefundCalculator.compute(
                subtotal = sale.subtotal,
                discount = sale.descuento,
                tax = sale.impuesto,
                refundSubtotal = refundSubtotal,
                previousRefundSubtotal = previousRefundSubtotal
            )
        }.getOrNull()
    }

    private fun errorMessage(error: ReturnException): String = when (error) {
        is ReturnException.NotAuthorized -> "No tienes permiso para realizar esta operación"
        is ReturnException.SaleNotFound -> "La venta ya no está disponible"
        is ReturnException.SaleNotCompleted -> "La venta ya no puede anularse o devolverse"
        is ReturnException.SaleNotToday -> getString(R.string.sale_detail_annul_same_day_error)
        is ReturnException.PriorRefundExists -> "La venta ya tiene devoluciones y no puede anularse"
        is ReturnException.InvalidReason -> "El motivo debe tener entre 5 y 300 caracteres"
        is ReturnException.NoActiveCashSession -> "No hay caja abierta. Abre la caja antes de continuar"
        is ReturnException.ExternalRefundNotConfirmed -> "Debes confirmar el reembolso externo"
        is ReturnException.InvalidQuantity -> "La cantidad a devolver debe ser mayor a cero"
        is ReturnException.RefundExceedsSoldQuantity -> "La devolución supera la cantidad vendida"
        is ReturnException.InsufficientStock -> "No se pudo reponer el stock"
        is ReturnException.CreditDebtNotEnough -> "La deuda del cliente no cubre el importe a reversar"
        is ReturnException.InconsistentTotals -> "No se pudo calcular el monto a devolver"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
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
