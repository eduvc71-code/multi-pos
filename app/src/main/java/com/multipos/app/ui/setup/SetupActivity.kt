package com.multipos.app.ui.setup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import com.multipos.app.security.PasswordHasher
import com.multipos.app.ui.home.HomeActivity
import com.multipos.app.ui.setup.compose.SetupScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.InventorySeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        setContent {
            MultiPOSTheme {
                var businessName by remember { mutableStateOf("") }
                var businessNit by remember { mutableStateOf("") }
                var ownerName by remember { mutableStateOf("") }
                var username by remember { mutableStateOf("") }
                var pass by remember { mutableStateOf("") }
                var confirmPass by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(false) }

                SetupScreen(
                    businessName = businessName,
                    onBusinessNameChange = { businessName = it },
                    businessNit = businessNit,
                    onBusinessNitChange = { businessNit = it },
                    ownerName = ownerName,
                    onOwnerNameChange = { ownerName = it },
                    username = username,
                    onUsernameChange = { username = it },
                    pass = pass,
                    onPassChange = { pass = it },
                    confirmPass = confirmPass,
                    onConfirmPassChange = { confirmPass = it },
                    isLoading = isLoading,
                    onCreateClick = {
                        createWorkspace(
                            businessName, businessNit, ownerName, username, pass, confirmPass
                        ) { loading -> isLoading = loading }
                    }
                )
            }
        }
    }

    private fun createWorkspace(
        businessName: String,
        businessNit: String,
        ownerName: String,
        username: String,
        pass: String,
        confirmPass: String,
        setLoading: (Boolean) -> Unit
    ) {
        val userTrim = username.trim().lowercase()
        
        if (businessName.length < 2) { 
            Toast.makeText(this, "Ingresa el nombre del negocio", Toast.LENGTH_SHORT).show()
            return 
        }
        if (ownerName.length < 3) { 
            Toast.makeText(this, "Ingresa el nombre del propietario", Toast.LENGTH_SHORT).show()
            return 
        }
        if (!userTrim.matches(Regex("[a-z0-9._-]{4,30}"))) { 
            Toast.makeText(this, "Usuario inválido (4-30 caracteres)", Toast.LENGTH_SHORT).show()
            return 
        }
        if (pass.length < 8) { 
            Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
            return 
        }
        if (pass != confirmPass) { 
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return 
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val digest = withContext(Dispatchers.Default) { PasswordHasher.hash(pass.toCharArray()) }
                val company = Empresa(
                    id = UUID.randomUUID().toString(),
                    nombre = businessName,
                    nit = businessNit.trim()
                )
                val db = DatabaseProvider.get(this@SetupActivity)
                var createdUser: Usuario? = null
                db.withTransaction {
                    if (db.empresaDao().count() > 0) throw IllegalStateException("Ya existe una empresa")
                    db.empresaDao().insert(company)
                    val id = db.usuarioDao().insert(
                        Usuario(
                            nombre = ownerName,
                            usuario = userTrim,
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
                
                // Sembrar productos de abarrotes inmediatamente
                InventorySeeder.seedAbarrotes(db, company.id)
                
                ActiveCompanyStore.set(this@SetupActivity, company.id)
                ActiveCompanyStore.setColor(this@SetupActivity, company.colorPrimarioHex)
                UserSessionStore.set(this@SetupActivity, owner)
                
                val intent = Intent(this@SetupActivity, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finishAffinity()
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(this@SetupActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
