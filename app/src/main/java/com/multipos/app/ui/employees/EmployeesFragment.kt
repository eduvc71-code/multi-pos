package com.multipos.app.ui.employees

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.entities.Usuario
import com.multipos.app.ui.components.MultiPOSTextField
import com.multipos.app.ui.employees.compose.EmployeesScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.viewmodel.EmployeesViewModel
import com.multipos.app.viewmodel.EmployeesViewModelFactory

class EmployeesFragment : Fragment() {
    private lateinit var viewModel: EmployeesViewModel
    
    private var showAddDialog by mutableStateOf(false)
    private var selectedEmployee by mutableStateOf<Usuario?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        val factory = EmployeesViewModelFactory(db.usuarioDao(), db.usuarioEmpresaDao(), companyId)
        viewModel = ViewModelProvider(this, factory)[EmployeesViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    EmployeesScreen(
                        employees = state.filteredEmployees,
                        searchQuery = state.searchQuery,
                        isLoading = state.isLoading,
                        onSearchChange = { viewModel.onSearchQueryChange(it) },
                        onAddEmployeeClick = { showAddDialog = true },
                        onEditEmployeeClick = { employee ->
                            selectedEmployee = employee
                        }
                    )

                    if (showAddDialog) {
                        AddEmployeeDialog(onDismiss = { showAddDialog = false })
                    }

                    selectedEmployee?.let { employee ->
                        EditEmployeeDialog(
                            employee = employee,
                            onDismiss = { selectedEmployee = null },
                            onSave = { name, role ->
                                viewModel.updateEmployee(employee.id, name, role) { success, error ->
                                    if (success) {
                                        selectedEmployee = null
                                        Toast.makeText(context, "Colaborador actualizado", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EditEmployeeDialog(
        employee: Usuario,
        onDismiss: () -> Unit,
        onSave: (String, String) -> Unit
    ) {
        var nombre by remember { mutableStateOf(employee.nombre) }
        var selectedRole by remember { mutableStateOf(employee.rol) }
        val roles = listOf(Usuario.ROL_ADMINISTRADOR, Usuario.ROL_CAJERO, Usuario.ROL_VENDEDOR)
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Editar Colaborador") },
            confirmButton = {
                TextButton(onClick = { onSave(nombre, selectedRole) }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MultiPOSTextField(value = nombre, onValueChange = { nombre = it }, label = "Nombre completo")
                    Text("Usuario: @${employee.username}", style = MaterialTheme.typography.bodySmall)
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Rol: $selectedRole")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            roles.forEach { role ->
                                DropdownMenuItem(text = { Text(role) }, onClick = { selectedRole = role; expanded = false })
                            }
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun AddEmployeeDialog(onDismiss: () -> Unit) {
        var nombre by remember { mutableStateOf("") }
        var usuario by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf(Usuario.ROL_VENDEDOR) }
        var passVisible by remember { mutableStateOf(false) }
        
        val roles = listOf(Usuario.ROL_ADMINISTRADOR, Usuario.ROL_CAJERO, Usuario.ROL_VENDEDOR)
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Registrar Colaborador") },
            confirmButton = {
                TextButton(onClick = {
                    if (nombre.isNotBlank() && usuario.isNotBlank() && password.length >= 4) {
                        viewModel.registerEmployee(nombre, usuario.lowercase(), password, selectedRole) { success, error ->
                            if (success) {
                                onDismiss()
                                Toast.makeText(context, "Empleado registrado", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Verifique los datos", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MultiPOSTextField(value = nombre, onValueChange = { nombre = it }, label = "Nombre completo")
                    MultiPOSTextField(value = usuario, onValueChange = { usuario = it }, label = "Nombre de usuario")
                    
                    MultiPOSTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Contraseña temporal",
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        }
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rol: $selectedRole")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            roles.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        selectedRole = role
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
