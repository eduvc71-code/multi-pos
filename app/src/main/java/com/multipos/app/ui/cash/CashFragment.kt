package com.multipos.app.ui.cash

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
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
import com.multipos.app.data.CashRepository
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.Usuario
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import com.multipos.app.ui.cash.compose.CashScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import com.multipos.app.viewmodel.CashViewModel
import com.multipos.app.viewmodel.CashViewModelFactory
import kotlinx.coroutines.launch

class CashFragment : Fragment() {
    private lateinit var viewModel: CashViewModel
    private var currentUserId: Int = 0
    private var canManageManualMovements = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        val userId = UserSessionStore.userId(requireContext())
        currentUserId = userId
        val repository = CashRepository(db)
        val factory = CashViewModelFactory(repository, companyId, userId)
        viewModel = ViewModelProvider(this, factory)[CashViewModel::class.java]

        viewLifecycleOwner.lifecycleScope.launch {
            val role = ActiveCompanyAccess.role(requireContext(), db)
            canManageManualMovements = role == Usuario.ROL_PROPIETARIO || role == Usuario.ROL_ADMINISTRADOR
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    CashScreen(
                        expectedBalance = state.expected,
                        ingresos = state.ingresos,
                        egresos = state.egresos,
                        movements = state.movements,
                        isCashOpen = state.session != null,
                        isLoading = state.isLoading,
                        onOpenCashClick = { showOpenDialog() },
                        onCloseCashClick = { showCloseDialog() },
                        onAddIncomeClick = { showMovementDialog(MovimientoCaja.TIPO_INGRESO_MANUAL) },
                        onAddExpenseClick = { showMovementDialog(MovimientoCaja.TIPO_EGRESO_MANUAL) }
                    )
                }
            }
        }
    }

    private fun showOpenDialog() {
        val context = requireContext()
        val amountInput = EditText(context).apply {
            hint = getString(R.string.cash_hint_apertura)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 12, 48, 0)
            addView(amountInput)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Abrir caja")
            .setMessage("Ingrese el monto inicial de efectivo en caja.")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Abrir", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val raw = amountInput.text.toString()
                val monto = Money.parseMinorUnits(raw)
                if (monto == null || monto < 0) {
                    Toast.makeText(context, getString(R.string.cash_error_monto_invalid), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.openSession(monto)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showMovementDialog(tipo: String) {
        if (!canManageManualMovements) {
            Toast.makeText(requireContext(), "No tienes permiso para registrar movimientos manuales", Toast.LENGTH_SHORT).show()
            return
        }
        val context = requireContext()
        val amountInput = EditText(context).apply {
            hint = getString(R.string.cash_hint_monto)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val conceptInput = EditText(context).apply {
            hint = getString(R.string.cash_hint_concepto)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 12, 48, 0)
            addView(amountInput); addView(conceptInput)
        }
        val title = if (tipo == MovimientoCaja.TIPO_INGRESO_MANUAL) "Ingreso manual" else "Egreso manual"
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val raw = amountInput.text.toString()
                val monto = Money.parseMinorUnits(raw)
                val concepto = conceptInput.text.toString().trim()
                if (monto == null || monto <= 0) {
                    Toast.makeText(context, getString(R.string.cash_error_monto_invalid), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (concepto.length < 3 || concepto.length > 200) {
                    Toast.makeText(context, getString(R.string.cash_error_concepto_invalid), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.addManualMovement(tipo, monto, concepto)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showCloseDialog() {
        val state = viewModel.uiState.value
        if (state.session == null) return
        if (!canManageManualMovements && state.session.abiertaPorUsuarioId != currentUserId) {
            Toast.makeText(requireContext(), "No tienes permiso para cerrar esta sesión de caja", Toast.LENGTH_SHORT).show()
            return
        }

        val context = requireContext()
        val countInput = EditText(context).apply {
            hint = getString(R.string.cash_hint_conteo)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val noteInput = EditText(context).apply {
            hint = "Nota del cierre"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 12, 48, 0)
            addView(countInput); addView(noteInput)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Cerrar caja")
            .setMessage("Ingrese el conteo de efectivo. La nota es obligatoria si existe diferencia.")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cerrar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val raw = countInput.text.toString()
                val contado = Money.parseMinorUnits(raw)
                val nota = noteInput.text.toString().trim()
                if (contado == null || contado < 0) {
                    Toast.makeText(context, getString(R.string.cash_error_monto_invalid), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val expected = viewModel.uiState.value.expected
                val difference = contado - expected
                if (nota.length > 300 || (difference != 0L && nota.length !in 5..300)) {
                    Toast.makeText(context, "La nota debe tener entre 5 y 300 caracteres cuando existe diferencia", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                confirmClose(contado, nota)
            }
        }
        dialog.show()
    }

    private fun confirmClose(contado: Long, nota: String) {
        val state = viewModel.uiState.value
        val difference = contado - state.expected
        val message = buildString {
            append("Ingresos: ${Money.format(state.ingresos)}\n")
            append("Egresos: ${Money.format(state.egresos)}\n")
            append("Esperado: ${Money.format(state.expected)}\n")
            append("Contado: ${Money.format(contado)}\n")
            append("Diferencia: ${Money.format(difference)}")
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar cierre de caja")
            .setMessage(message)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cerrar caja") { _, _ -> viewModel.closeSession(contado, nota) }
            .show()
    }
}
