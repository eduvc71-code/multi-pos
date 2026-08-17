package com.multipos.app.ui.employees

import android.os.Bundle
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.multipos.app.R
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.entities.Usuario
import com.multipos.app.ui.employees.compose.EmployeesScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.viewmodel.EmployeesViewModel
import com.multipos.app.viewmodel.EmployeesViewModelFactory

class EmployeesFragment : Fragment() {
    private lateinit var viewModel: EmployeesViewModel

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
                        onAddEmployeeClick = { showAddEmployeeDialog() },
                        onEditEmployeeClick = { employee ->
                            Toast.makeText(context, "Editar: ${employee.nombre}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    private fun showAddEmployeeDialog() {
        val context = requireContext()
        val nameInput = EditText(context).apply { hint = "Nombre completo"; setPadding(32, 16, 32, 16) }
        val userInput = EditText(context).apply { hint = "Usuario (ej. juan24)"; setPadding(32, 16, 32, 16) }
        val passInput = EditText(context).apply { hint = "Contraseña temporal"; setPadding(32, 16, 32, 16); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        
        val roleSpinner = Spinner(context).apply {
            val roles = listOf(Usuario.ROL_ADMINISTRADOR, Usuario.ROL_CAJERO, Usuario.ROL_VENDEDOR)
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, roles)
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            addView(nameInput)
            addView(userInput)
            addView(passInput)
            addView(roleSpinner)
        }

        AlertDialog.Builder(context)
            .setTitle("Registrar colaborador")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val name = nameInput.text.toString().trim()
                val user = userInput.text.toString().trim().lowercase()
                val pass = passInput.text.toString()
                val role = roleSpinner.selectedItem.toString()

                if (name.isEmpty() || user.isEmpty() || pass.length < 4) {
                    Toast.makeText(context, "Completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.registerEmployee(name, user, pass, role) { success, error ->
                    if (success) {
                        Toast.makeText(context, "Empleado registrado con éxito", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error: ${error ?: "Usuario ya existe"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
