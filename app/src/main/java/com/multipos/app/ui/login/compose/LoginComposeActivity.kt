package com.multipos.app.ui.login.compose

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.AuthRepository
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Usuario
import com.multipos.app.R
import com.multipos.app.security.PasswordHasher
import com.multipos.app.ui.home.HomeActivity
import com.multipos.app.ui.login.LoginActivity
import com.multipos.app.ui.setup.SetupActivity
import com.multipos.app.ui.theme.MultiPOSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginComposeActivity : ComponentActivity() {
    
    private var username by mutableStateOf("")
    private var password by mutableStateOf("")
    private var isLoading by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MultiPOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        username = username,
                        password = password,
                        isLoading = isLoading,
                        onUsernameChange = { username = it },
                        onPasswordChange = { password = it },
                        onLoginClick = ::attemptLogin
                    )
                }
            }
        }
        
        // Verificar si necesita setup o ya está autenticado
        lifecycleScope.launch {
            val db = DatabaseProvider.get(this@LoginComposeActivity)
            if (db.empresaDao().count() == 0 || db.usuarioDao().count() == 0) {
                startActivity(Intent(this@LoginComposeActivity, SetupActivity::class.java))
                finish()
                return@launch
            }
            
            if (UserSessionStore.isAuthenticated(this@LoginComposeActivity)) {
                val sessionUser = db.usuarioDao().getById(UserSessionStore.userId(this@LoginComposeActivity))
                if (sessionUser != null) {
                    openHome(sessionUser)
                    return@launch
                }
                UserSessionStore.clear(this@LoginComposeActivity)
            }
            
            isLoading = false
        }
    }
    
    private fun attemptLogin() {
        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Ingresa tu usuario y contraseña", Toast.LENGTH_SHORT).show()
            return
        }
        
        isLoading = true
        
        lifecycleScope.launch {
            val db = DatabaseProvider.get(this@LoginComposeActivity)
            
            // Verificar si está bloqueado
            val blocked = withContext(Dispatchers.Default) {
                db.usuarioDao().getByUsername(username.trim().lowercase())?.bloqueadoHasta
            }
            
            val now = System.currentTimeMillis()
            if (blocked != null && blocked > now) {
                val mins = ((blocked - now) / 60_000L).coerceAtLeast(1L)
                isLoading = false
                Toast.makeText(
                    this@LoginComposeActivity, 
                    getString(R.string.login_blocked, mins.toInt()), 
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            
            // Autenticar
            val user = withContext(Dispatchers.Default) {
                AuthRepository(db).authenticate(username.trim().lowercase(), password.toCharArray())
            }
            
            isLoading = false
            
            if (user == null) {
                Toast.makeText(
                    this@LoginComposeActivity, 
                    R.string.login_error, 
                    Toast.LENGTH_SHORT
                ).show()
            } else if (user.requiereCambioClave) {
                // TODO: Mostrar diálogo para cambio de contraseña
                Toast.makeText(
                    this@LoginComposeActivity,
                    "Debes cambiar tu contraseña",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                openHome(user)
            }
        }
    }
    
    private suspend fun openHome(user: Usuario) {
        UserSessionStore.set(this, user)
        val currentCompany = ActiveCompanyStore.get(this)
        val membership = DatabaseProvider.get(this)
            .usuarioEmpresaDao()
            .getActiveMembership(user.id, currentCompany)
        
        if (membership == null) {
            ActiveCompanyStore.set(this, user.empresaId)
        }
        
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
