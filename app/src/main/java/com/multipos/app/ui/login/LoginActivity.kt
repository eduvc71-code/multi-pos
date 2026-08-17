package com.multipos.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.multipos.app.R
import com.multipos.app.data.AuthRepository
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Usuario
import com.multipos.app.databinding.ActivityLoginBinding
import com.multipos.app.security.PasswordHasher
import com.multipos.app.ui.home.HomeActivity
import com.multipos.app.ui.setup.SetupActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
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

        val db = DatabaseProvider.get(this)
        
        setupAutoScroll()
        
        lifecycleScope.launch {
            if (db.empresaDao().count() == 0 || db.usuarioDao().count() == 0) {
                startActivity(Intent(this@LoginActivity, SetupActivity::class.java))
                finish()
                return@launch
            }
            if (UserSessionStore.isAuthenticated(this@LoginActivity)) {
                val sessionUser = db.usuarioDao().getById(UserSessionStore.userId(this@LoginActivity))
                if (sessionUser != null) {
                    openHome(sessionUser)
                    return@launch
                }
                UserSessionStore.clear(this@LoginActivity)
            }
            binding.loginProgress.visibility = View.GONE
            binding.btnLogin.isEnabled = true
        }
        binding.btnLogin.setOnClickListener {
            val username = binding.etUser.text.toString().trim().lowercase()
            val password = binding.etPassword.text.toString()
            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Ingresa tu usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setLoading(true)
            lifecycleScope.launch {
                val blocked = withContext(Dispatchers.Default) {
                    db.usuarioDao().getByUsername(username)?.let { it.bloqueadoHasta }
                }
                val now = System.currentTimeMillis()
                if (blocked != null && blocked > now) {
                    val mins = ((blocked - now) / 60_000L).coerceAtLeast(1L)
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, getString(R.string.login_blocked, mins), Toast.LENGTH_LONG).show()
                    return@launch
                }
                val user = withContext(Dispatchers.Default) {
                    AuthRepository(db).authenticate(username, password.toCharArray())
                }
                if (user == null) {
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, R.string.login_error, Toast.LENGTH_SHORT).show()
                } else if (user.requiereCambioClave) {
                    setLoading(false)
                    showRequiredPasswordChange(user)
                } else {
                    openHome(user)
                }
            }
        }
    }

    private fun showRequiredPasswordChange(user: Usuario) {
        val password = EditText(this).apply { hint = "Nueva contraseña"; inputType = 0x00000081 }
        val confirmation = EditText(this).apply { hint = "Confirmar contraseña"; inputType = 0x00000081 }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 12, 48, 0)
            addView(password)
            addView(confirmation)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Actualiza tu contraseña")
            .setMessage("Esta cuenta proviene de una versión anterior. Crea una contraseña segura para continuar.")
            .setView(form)
            .setCancelable(false)
            .setPositiveButton("Guardar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newPassword = password.text.toString()
                if (newPassword.length < 8 || newPassword != confirmation.text.toString()) {
                    Toast.makeText(this, "Usa al menos 8 caracteres y confirma la misma contraseña", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    val digest = withContext(Dispatchers.Default) { PasswordHasher.hash(newPassword.toCharArray()) }
                    val updated = user.copy(password = "", passwordHash = digest.hash, passwordSalt = digest.salt, requiereCambioClave = false)
                    DatabaseProvider.get(this@LoginActivity).usuarioDao().update(updated)
                    dialog.dismiss()
                    openHome(updated)
                }
            }
        }
        dialog.show()
    }

    private suspend fun openHome(user: Usuario) {
        UserSessionStore.set(this, user)
        val currentCompany = com.multipos.app.data.ActiveCompanyStore.get(this)
        val membership = DatabaseProvider.get(this).usuarioEmpresaDao().getActiveMembership(user.id, currentCompany)
        if (membership == null) com.multipos.app.data.ActiveCompanyStore.set(this, user.empresaId)
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun setupAutoScroll() {
        val inputs = listOf(binding.etUser, binding.etPassword)
        inputs.forEach { input ->
            input.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.postDelayed({
                        val parent = view.parent.parent as? android.view.View ?: view
                        binding.rootScrollView.smoothScrollTo(0, parent.top - 20)
                    }, 400)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.loginProgress.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
