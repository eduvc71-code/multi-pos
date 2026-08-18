package com.multipos.app.ui.cash

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
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.CashRepository
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.Usuario
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.ui.cash.compose.CashScreen
import com.multipos.app.ui.components.MultiPOSTextField
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import com.multipos.app.viewmodel.CashViewModel
import com.multipos.app.viewmodel.CashViewModelFactory
import kotlinx.coroutines.launch

class CashFragment : Fragment() {
    private lateinit var viewModel: CashViewModel
    private var currentUserId: Int = 0
    private var canManageManualMovements by mutableStateOf(false)

    private var showOpenDialog by mutableStateOf(false)
    private var showCloseDialog by mutableStateOf(false)
    private var showMovementDialog by mutableStateOf(false)
    private var movementType by mutableStateOf("")

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
                        onOpenCashClick = { showOpenDialog = true },
                        onCloseCashClick = { showCloseDialog = true },
                        onAddIncomeClick = { 
                            movementType = MovimientoCaja.TIPO_INGRESO_MANUAL
                            showMovementDialog = true 
                        },
                        onAddExpenseClick = { 
                            movementType = MovimientoCaja.TIPO_EGRESO_MANUAL
                            showMovementDialog = true 
                        }
                    )

                    if (showOpenDialog) {
                        OpenCashDialog(onDismiss = { showOpenDialog = false })
                    }

                    if (showCloseDialog) {
                        CloseCashDialog(
                            expected = state.expected,
                            onDismiss = { showCloseDialog = false }
                        )
                    }

                    if (showMovementDialog) {
                        ManualMovementDialog(
                            tipo = movementType,
                            onDismiss = { showMovementDialog = false }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun OpenCashDialog(onDismiss: () -> Unit) {
        var monto by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Abrir Caja") },
            text = {
                MultiPOSTextField(value = monto, onValueChange = { monto = it }, label = "Monto de apertura")
            },
            confirmButton = {
                TextButton(onClick = {
                    val valMonto = Money.parseMinorUnits(monto)
                    if (valMonto != null && valMonto >= 0) {
                        viewModel.openSession(valMonto)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Monto inválido", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Abrir") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }

    @Composable
    private fun CloseCashDialog(expected: Long, onDismiss: () -> Unit) {
        var contado by remember { mutableStateOf("") }
        var nota by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Cerrar Caja") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Total esperado: ${Money.format(expected)}")
                    MultiPOSTextField(value = contado, onValueChange = { contado = it }, label = "Efectivo en caja")
                    MultiPOSTextField(value = nota, onValueChange = { nota = it }, label = "Nota (obligatoria si hay diferencia)")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val valContado = Money.parseMinorUnits(contado)
                    if (valContado != null && valContado >= 0) {
                        val diff = valContado - expected
                        if (diff != 0L && nota.trim().length < 5) {
                            Toast.makeText(context, "La nota es obligatoria cuando hay diferencia", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.closeSession(valContado, nota)
                            onDismiss()
                        }
                    } else {
                        Toast.makeText(context, "Monto inválido", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Cerrar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }

    @Composable
    private fun ManualMovementDialog(tipo: String, onDismiss: () -> Unit) {
        var monto by remember { mutableStateOf("") }
        var concepto by remember { mutableStateOf("") }
        val title = if (tipo == MovimientoCaja.TIPO_INGRESO_MANUAL) "Ingreso Manual" else "Egreso Manual"
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MultiPOSTextField(value = monto, onValueChange = { monto = it }, label = "Monto")
                    MultiPOSTextField(value = concepto, onValueChange = { concepto = it }, label = "Concepto")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val valMonto = Money.parseMinorUnits(monto)
                    if (valMonto != null && valMonto > 0 && concepto.trim().length >= 3) {
                        viewModel.addManualMovement(tipo, valMonto, concepto)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Monto o concepto inválido", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }
}
