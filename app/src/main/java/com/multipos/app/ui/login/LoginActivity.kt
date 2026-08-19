package com.multipos.app.ui.login

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
import androidx.room.withTransaction
import androidx.lifecycle.lifecycleScope
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.AuthRepository
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Usuario
import com.multipos.app.R
import com.multipos.app.data.entities.Auditoria
import com.multipos.app.ui.home.HomeActivity
import com.multipos.app.ui.login.compose.LoginScreen
import com.multipos.app.ui.setup.SetupActivity
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.InventorySeeder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {
    
    private var username by mutableStateOf("")
    private var password by mutableStateOf("")
    private var isLoading by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
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
                        onLoginClick = ::attemptLogin,
                        onExitClick = { finishAffinity() }
                    )
                }
            }
        }
        android.util.Log.d("LoginActivity", "onCreate: LoginActivity iniciada")
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("LoginActivity", "onDestroy: LoginActivity FINALIZADA/MUERTA")
    }
    
    override fun onResume() {
        super.onResume()
        // Verificación inicial
        lifecycleScope.launch {
            val db = DatabaseProvider.get(this@LoginActivity)
            
            // Limpiar datos demo si existen (para cumplir con "ya no es demo")
            val demoCompany = db.empresaDao().getById("demo-grocery-store")
            if (demoCompany != null) {
                android.util.Log.d("LoginActivity", "Limpiando empresa demo detectada...")
                db.withTransaction {
                    db.productoDao().deleteAll("demo-grocery-store")
                    db.usuarioDao().deleteByCompany("demo-grocery-store") // Borrar solo usuarios de la demo
                    db.empresaDao().deleteById("demo-grocery-store")
                }
                UserSessionStore.clear(this@LoginActivity)
            }
            
            if (db.empresaDao().count() == 0 || db.usuarioDao().count() == 0) {
                android.util.Log.d("LoginActivity", "No hay empresas/usuarios. Redirigiendo a SetupActivity.")
                val intent = Intent(this@LoginActivity, SetupActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
                return@launch
            }
            
            if (UserSessionStore.isAuthenticated(this@LoginActivity)) {
                val sessionUserId = UserSessionStore.userId(this@LoginActivity)
                android.util.Log.d("LoginActivity", "Sesión detectada para usuario ID: $sessionUserId. Verificando integridad...")
                val sessionUser = db.usuarioDao().getById(sessionUserId)
                if (sessionUser != null) {
                    db.auditoriaDao().insert(
                        Auditoria(
                            empresaId = sessionUser.empresaId,
                            usuarioId = sessionUser.id,
                            accion = Auditoria.ACCION_LOGIN,
                            entidad = "usuario",
                            entidadId = sessionUser.id.toString(),
                            detalle = "login automático (sesión persistente)"
                        )
                    )
                    openHome(sessionUser)
                    return@launch
                } else {
                    android.util.Log.w("LoginActivity", "Usuario de sesión no encontrado. Limpiando sesión.")
                    UserSessionStore.clear(this@LoginActivity)
                }
            }
            android.util.Log.d("LoginActivity", "Esperando login manual del usuario.")
        }
    }
    
    private fun attemptLogin() {
        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Ingresa tu usuario y contraseña", Toast.LENGTH_SHORT).show()
            return
        }
        
        isLoading = true
        lifecycleScope.launch {
            val db = DatabaseProvider.get(this@LoginActivity)
            val userLower = username.trim().lowercase()
            
            // Verificar bloqueo
            val blocked = withContext(Dispatchers.Default) {
                db.usuarioDao().getByUsername(userLower)?.bloqueadoHasta
            }
            
            val now = System.currentTimeMillis()
            if (blocked != null && blocked > now) {
                val mins = ((blocked - now) / 60_000L).coerceAtLeast(1L)
                isLoading = false
                Toast.makeText(this@LoginActivity, getString(R.string.login_blocked, mins.toInt()), Toast.LENGTH_LONG).show()
                return@launch
            }
            
            // Autenticar
            val user = withContext(Dispatchers.Default) {
                AuthRepository(db).authenticate(userLower, password.toCharArray())
            }
            
            isLoading = false
            if (user == null) {
                Toast.makeText(this@LoginActivity, R.string.login_error, Toast.LENGTH_SHORT).show()
            } else {
                openHome(user)
            }
        }
    }
    
    private suspend fun openHome(user: Usuario) {
        try {
            android.util.Log.d("LoginActivity", "openHome: Iniciando para usuario ${user.usuario} en empresa ${user.empresaId}")
            UserSessionStore.set(this, user)
            val db = DatabaseProvider.get(this)
            
            // Carga de productos si el inventario está vacío - ASEGURADO Y ROBUSTO
            val count = withContext(Dispatchers.IO) { db.productoDao().count(user.empresaId) }
            android.util.Log.d("LoginActivity", "openHome: Productos actuales en empresa ${user.empresaId} = $count")
            if (count == 0) {
                android.util.Log.d("LoginActivity", "openHome: Sembrando 30 productos de abarrotes...")
                InventorySeeder.seedAbarrotes(db, user.empresaId)
            }

            val currentCompany = ActiveCompanyStore.get(this)
            val membership = withContext(Dispatchers.IO) { 
                db.usuarioEmpresaDao().getActiveMembership(user.id, currentCompany) 
            }
            
            if (membership == null) {
                android.util.Log.d("LoginActivity", "openHome: Configurando empresa activa: ${user.empresaId}")
                ActiveCompanyStore.set(this, user.empresaId)
                val company = withContext(Dispatchers.IO) { db.empresaDao().getById(user.empresaId) }
                if (company != null) {
                    ActiveCompanyStore.setName(this@LoginActivity, company.nombre)
                    ActiveCompanyStore.setColor(this@LoginActivity, company.colorPrimarioHex)
                }
            }
            
            android.util.Log.d("LoginActivity", "openHome: Saltando a HomeActivity y matando LoginActivity definitivamente")
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish() 
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "openHome: CRITICAL ERROR", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginActivity, "Error al iniciar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
