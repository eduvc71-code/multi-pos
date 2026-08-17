package com.multipos.app.ui.employees

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
import androidx.room.withTransaction
import com.multipos.app.adapters.EmployeeAdapter
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import com.multipos.app.databinding.FragmentEmployeesBinding
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions
import com.multipos.app.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmployeesFragment : Fragment() {
    private var _binding: FragmentEmployeesBinding? = null
    private val binding get() = _binding!!
    private var activeRole: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEmployeesBinding.inflate(inflater, container, false)
        val companyId = ActiveCompanyStore.get(requireContext())
        val db = DatabaseProvider.get(requireContext())
        binding.btnAddEmployee.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val role = ActiveCompanyAccess.role(requireContext(), db)
            if (!CompanyPermissions.allows(role, CompanyPermission.MANAGE_EMPLOYEES)) {
                Toast.makeText(requireContext(), "No tienes permiso para administrar empleados", Toast.LENGTH_LONG).show()
                return@launch
            }
            activeRole = role
            binding.btnAddEmployee.isEnabled = true
            val adapter = EmployeeAdapter(UserSessionStore.userId(requireContext()), role!!) { user, active ->
                viewLifecycleOwner.lifecycleScope.launch {
                    if (!ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.MANAGE_EMPLOYEES) ||
                        (user.rol == Usuario.ROL_ADMINISTRADOR &&
                            ActiveCompanyAccess.role(requireContext(), db) != Usuario.ROL_PROPIETARIO) ||
                        db.usuarioEmpresaDao().setActive(user.id, companyId, active) == 0
                    ) {
                        Toast.makeText(requireContext(), "No se pudo actualizar el empleado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            binding.recyclerEmployees.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerEmployees.adapter = adapter
            binding.btnAddEmployee.setOnClickListener { showCreateEmployeeDialog() }
            db.usuarioDao().getByCompany(companyId).collect { users ->
                adapter.submitList(users)
                binding.txtEmployeesEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerEmployees.visibility = if (users.isEmpty()) View.GONE else View.VISIBLE
            }
        }
        return binding.root
    }

    private fun showCreateEmployeeDialog() {
        val context = requireContext()
        val name = EditText(context).apply { hint = "Nombre completo"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS }
        val username = EditText(context).apply { hint = "Usuario"; inputType = InputType.TYPE_CLASS_TEXT }
        val password = EditText(context).apply { hint = "Contraseña temporal"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val roles = if (activeRole == Usuario.ROL_PROPIETARIO) {
            listOf(Usuario.ROL_ADMINISTRADOR, Usuario.ROL_CAJERO, Usuario.ROL_VENDEDOR)
        } else listOf(Usuario.ROL_CAJERO, Usuario.ROL_VENDEDOR)
        val role = Spinner(context).apply { adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, roles) }
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 12, 48, 0)
            addView(name); addView(username); addView(password); addView(role)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Registrar empleado")
            .setMessage("El empleado deberá cambiar la contraseña temporal en su primer acceso.")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Registrar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val employeeName = name.text.toString().trim()
                val employeeUsername = username.text.toString().trim().lowercase()
                val temporaryPassword = password.text.toString()
                if (employeeName.length < 3 || !employeeUsername.matches(Regex("[a-z0-9._-]{4,30}")) || temporaryPassword.length < 8) {
                    Toast.makeText(context, "Completa el nombre, un usuario válido y una contraseña de al menos 8 caracteres", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val db = DatabaseProvider.get(context)
                        val currentRole = ActiveCompanyAccess.role(context, db)
                        if (!CompanyPermissions.allows(currentRole, CompanyPermission.MANAGE_EMPLOYEES) ||
                            (role.selectedItem == Usuario.ROL_ADMINISTRADOR && currentRole != Usuario.ROL_PROPIETARIO)
                        ) {
                            error("Permiso revocado")
                        }
                        val digest = withContext(Dispatchers.Default) { PasswordHasher.hash(temporaryPassword.toCharArray()) }
                        val companyId = ActiveCompanyStore.get(context)
                        db.withTransaction {
                            val userId = db.usuarioDao().insert(
                                Usuario(
                                    nombre = employeeName,
                                    usuario = employeeUsername,
                                    passwordHash = digest.hash,
                                    passwordSalt = digest.salt,
                                    rol = role.selectedItem.toString(),
                                    empresaId = companyId,
                                    requiereCambioClave = true
                                )
                            ).toInt()
                            db.usuarioEmpresaDao().insert(UsuarioEmpresa(userId, companyId, role.selectedItem.toString()))
                        }
                        dialog.dismiss()
                        Toast.makeText(context, "Empleado registrado", Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        val message = if (error.message == "Permiso revocado") {
                            "Ya no tienes permiso para registrar empleados"
                        } else {
                            "Ese nombre de usuario ya está registrado"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
