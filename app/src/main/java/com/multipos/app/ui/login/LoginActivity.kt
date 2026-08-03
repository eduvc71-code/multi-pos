package com.multipos.app.ui.login 
 
import android.os.Bundle 
import androidx.appcompat.app.AppCompatActivity 
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.widget.Toast
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.entities.Producto
import com.multipos.app.data.entities.Usuario
import com.multipos.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import com.multipos.app.R 
 
class LoginActivity : AppCompatActivity() { 
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) { 
        super.onCreate(savedInstanceState) 
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val db = DatabaseProvider.get(this)
        lifecycleScope.launch {
            if (db.usuarioDao().count() == 0) db.usuarioDao().insert(Usuario(nombre = "Administrador", usuario = "admin", password = "admin", rol = "ADMIN"))
            if (db.productoDao().count() == 0) {
                db.productoDao().insert(Producto(nombre = "Café", codigo = "CAF-001", precioVenta = 12.0, costoUnitario = 5.0, stock = 20))
                db.productoDao().insert(Producto(nombre = "Pan dulce", codigo = "PAN-001", precioVenta = 8.0, costoUnitario = 3.0, stock = 30))
                db.productoDao().insert(Producto(nombre = "Agua", codigo = "AGU-001", precioVenta = 6.0, costoUnitario = 2.0, stock = 50))
            }
        }
        binding.btnLogin.setOnClickListener {
            val user = binding.etUser.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            if (user.isBlank() || pass.isBlank()) return@setOnClickListener
            lifecycleScope.launch {
                if (db.usuarioDao().login(user, pass) != null) {
                    startActivity(Intent(this@LoginActivity, com.multipos.app.ui.home.HomeActivity::class.java)); finish()
                } else Toast.makeText(this@LoginActivity, R.string.login_error, Toast.LENGTH_SHORT).show()
            }
        }
    } 
} 
