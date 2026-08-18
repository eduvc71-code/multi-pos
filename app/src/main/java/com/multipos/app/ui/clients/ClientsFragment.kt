package com.multipos.app.ui.clients

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
import com.multipos.app.R
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.entities.Cliente
import com.multipos.app.ui.clients.compose.ClientsScreen
import com.multipos.app.ui.components.MultiPOSTextField
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import com.multipos.app.viewmodel.ClientsViewModel
import com.multipos.app.viewmodel.ClientsViewModelFactory

class ClientsFragment : Fragment() {
    private lateinit var viewModel: ClientsViewModel
    
    private var showClientDialog by mutableStateOf(false)
    private var selectedClient by mutableStateOf<Cliente?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        val factory = ClientsViewModelFactory(db.clienteDao(), companyId)
        viewModel = ViewModelProvider(this, factory)[ClientsViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    ClientsScreen(
                        clients = state.filteredClients,
                        searchQuery = state.searchQuery,
                        isLoading = state.isLoading,
                        onSearchChange = { viewModel.onSearchQueryChange(it) },
                        onAddClientClick = { 
                            selectedClient = null
                            showClientDialog = true 
                        },
                        onEditClientClick = { client ->
                            selectedClient = client
                            showClientDialog = true
                        },
                        onViewStatementClick = { client ->
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.homeContainer, EstadoCuentaFragment.newInstance(client.id, companyId))
                                .addToBackStack(null)
                                .commit()
                        }
                    )

                    if (showClientDialog) {
                        ClientDialog(
                            client = selectedClient,
                            onDismiss = { showClientDialog = false },
                            onSave = { name, doc, phone, address, creditLimit ->
                                saveClient(name, doc, phone, address, creditLimit, companyId)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ClientDialog(
        client: Cliente?,
        onDismiss: () -> Unit,
        onSave: (String, String, String, String, Long) -> Unit
    ) {
        var nombre by remember { mutableStateOf(client?.nombre ?: "") }
        var documento by remember { mutableStateOf(client?.documento ?: "") }
        var telefono by remember { mutableStateOf(client?.telefono ?: "") }
        var direccion by remember { mutableStateOf(client?.direccion ?: "") }
        var limiteCredito by remember { mutableStateOf(if (client != null) (client.limiteCredito / 100.0).toString() else "0") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (client == null) "Nuevo Cliente" else "Editar Cliente") },
            confirmButton = {
                TextButton(onClick = {
                    val limit = Money.parseMinorUnits(limiteCredito) ?: 0L
                    onSave(nombre, documento, telefono, direccion, limit)
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MultiPOSTextField(value = nombre, onValueChange = { nombre = it }, label = "Nombre completo")
                    MultiPOSTextField(value = documento, onValueChange = { documento = it }, label = "Documento / NIT")
                    MultiPOSTextField(value = telefono, onValueChange = { telefono = it }, label = "Teléfono")
                    MultiPOSTextField(value = direccion, onValueChange = { direccion = it }, label = "Dirección")
                    MultiPOSTextField(value = limiteCredito, onValueChange = { limiteCredito = it }, label = "Límite de Crédito")
                }
            }
        )
    }

    private fun saveClient(nombre: String, documento: String, telefono: String, direccion: String, limiteCredito: Long, companyId: String) {
        if (nombre.isBlank()) {
            Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        val client = Cliente(
            id = selectedClient?.id ?: 0,
            nombre = nombre,
            documento = documento,
            telefono = telefono,
            direccion = direccion,
            limiteCredito = limiteCredito,
            creditoActual = selectedClient?.creditoActual ?: 0L,
            creditoHabilitado = selectedClient?.creditoHabilitado ?: (limiteCredito > 0),
            estadoCredito = selectedClient?.estadoCredito ?: if (limiteCredito > 0) "ACTIVO" else "NO_SOLICITADO",
            fechaInscripcion = selectedClient?.fechaInscripcion ?: System.currentTimeMillis(),
            empresaId = companyId
        )

        viewModel.saveClient(client) { success ->
            if (success) {
                showClientDialog = false
                Toast.makeText(requireContext(), "Cliente guardado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Error al guardar cliente. Verifique que el documento no esté repetido.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
