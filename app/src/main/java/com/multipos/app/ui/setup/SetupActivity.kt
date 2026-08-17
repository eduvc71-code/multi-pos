package com.multipos.app.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import com.multipos.app.databinding.ActivitySetupBinding
import com.multipos.app.security.PasswordHasher
import com.multipos.app.ui.home.HomeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootScrollView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                if (ime.bottom > 0) ime.bottom else systemBars.bottom
            )
            
            insets
        }

        binding.btnCreateWorkspace.setOnClickListener { createWorkspace() }
        
        setupAutoScroll()
    }

    private fun setupAutoScroll() {
        val inputs = listOf(
            binding.etBusinessName,
            binding.etBusinessNit,
            binding.etOwnerName,
            binding.etUsername,
            binding.etPassword,
            binding.etConfirmPassword
        )
        
        inputs.forEach { input ->
            input.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.postDelayed({
                        // Desplazar para que el campo esté arriba en el ScrollView
                        val parent = view.parent.parent as? android.view.View ?: view
                        binding.rootScrollView.smoothScrollTo(0, parent.top - 20)
                    }, 400)
                }
            }
        }
    }

    private fun createWorkspace() {
        clearErrors()
        val businessName = binding.etBusinessName.text?.toString().orEmpty().trim()
        val ownerName = binding.etOwnerName.text?.toString().orEmpty().trim()
        val username = binding.etUsername.text?.toString().orEmpty().trim().lowercase()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmation = binding.etConfirmPassword.text?.toString().orEmpty()

        var valid = true
        if (businessName.length < 2) { binding.layoutBusinessName.error = "Ingresa el nombre del negocio"; valid = false }
        if (ownerName.length < 3) { binding.layoutOwnerName.error = "Ingresa el nombre del propietario"; valid = false }
        if (!username.matches(Regex("[a-z0-9._-]{4,30}"))) { binding.layoutUsername.error = "Usa entre 4 y 30 letras, números, punto, guion o guion bajo"; valid = false }
        if (password.length < 8) { binding.layoutPassword.error = "Usa al menos 8 caracteres"; valid = false }
        if (password != confirmation) { binding.layoutConfirmPassword.error = "Las contraseñas no coinciden"; valid = false }
        if (!valid) return

        setLoading(true)
        lifecycleScope.launch {
            try {
                val digest = withContext(Dispatchers.Default) { PasswordHasher.hash(password.toCharArray()) }
                val company = Empresa(
                    id = UUID.randomUUID().toString(),
                    nombre = businessName,
                    nit = binding.etBusinessNit.text?.toString().orEmpty().trim()
                )
                val db = DatabaseProvider.get(this@SetupActivity)
                var createdUser: Usuario? = null
                db.withTransaction {
                    check(db.empresaDao().count() == 0 && db.usuarioDao().count() == 0) { "La configuración inicial ya fue completada" }
                    db.empresaDao().insert(company)
                    val id = db.usuarioDao().insert(
                        Usuario(
                            nombre = ownerName,
                            usuario = username,
                            passwordHash = digest.hash,
                            passwordSalt = digest.salt,
                            rol = Usuario.ROL_PROPIETARIO,
                            empresaId = company.id
                        )
                    ).toInt()
                    db.usuarioEmpresaDao().insert(UsuarioEmpresa(id, company.id, Usuario.ROL_PROPIETARIO))
                    createdUser = db.usuarioDao().getById(id)
                }
                val owner = checkNotNull(createdUser)
                ActiveCompanyStore.set(this@SetupActivity, company.id)
                ActiveCompanyStore.setColor(this@SetupActivity, company.colorPrimarioHex)
                UserSessionStore.set(this@SetupActivity, owner)
                startActivity(Intent(this@SetupActivity, HomeActivity::class.java))
                finishAffinity()
            } catch (_: Exception) {
                setLoading(false)
                Toast.makeText(this@SetupActivity, "No se pudo completar la configuración. Inténtalo nuevamente.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearErrors() {
        binding.layoutBusinessName.error = null
        binding.layoutOwnerName.error = null
        binding.layoutUsername.error = null
        binding.layoutPassword.error = null
        binding.layoutConfirmPassword.error = null
    }

    private fun setLoading(loading: Boolean) {
        binding.btnCreateWorkspace.isEnabled = !loading
        binding.setupProgress.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
