package com.multipos.app.ui.home

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.multipos.app.R
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions
import com.multipos.app.ui.cash.CashFragment
import com.multipos.app.ui.clients.ClientsFragment
import com.multipos.app.ui.dashboard.DashboardFragment
import com.multipos.app.ui.employees.EmployeesFragment
import com.multipos.app.ui.history.HistoryFragment
import com.multipos.app.ui.home.compose.HomeScreen
import com.multipos.app.ui.inventory.InventoryFragment
import com.multipos.app.ui.login.LoginActivity
import com.multipos.app.ui.pos.PosFragment
import com.multipos.app.ui.reports.ReportesFragment
import com.multipos.app.ui.theme.MultiPOSTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class HomeActivity : AppCompatActivity() {
    
    private var activeCompanyName by mutableStateOf("Cargando...")
    private var userName by mutableStateOf("...")
    private var userRole by mutableStateOf("...")
    private var companyColor by mutableStateOf(Color(0xFF1E40AF))
    private var selectedMenu by mutableStateOf("DASHBOARD")
    private var activeRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val db = DatabaseProvider.get(this)
        val userId = UserSessionStore.userId(this)
        
        if (userId == 0) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            MultiPOSTheme {
                HomeScreen(
                    activeCompanyName = activeCompanyName,
                    userName = userName,
                    userRole = userRole,
                    companyColor = companyColor,
                    onLogoutClick = { logout() },
                    onCompanyClick = { showCompanySelector() },
                    onMenuItemClick = { menu -> 
                        if (canNavigateTo(menu)) {
                            selectedMenu = menu
                            navigateToFragment(menu)
                        } else {
                            Toast.makeText(this, "No tienes permiso", Toast.LENGTH_SHORT).show()
                        }
                    },
                    selectedMenu = selectedMenu
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            FrameLayout(context).apply { id = R.id.homeContainer }
                        }
                    )
                }
            }
        }

        loadSessionData()
    }

    private fun loadSessionData() {
        val db = DatabaseProvider.get(this)
        val userId = UserSessionStore.userId(this)
        
        lifecycleScope.launch {
            val user = db.usuarioDao().getById(userId) ?: return@launch
            userName = user.nombre
            
            db.usuarioEmpresaDao().getCompaniesForUser(userId).collect { companies ->
                val active = companies.firstOrNull { it.id == ActiveCompanyStore.get(this@HomeActivity) } ?: companies.firstOrNull()
                if (active == null) {
                    logout()
                    return@collect
                }
                
                activeCompanyName = active.nombre
                val hex = active.colorPrimarioHex
                companyColor = try { Color(AndroidColor.parseColor(hex)) } catch (e: Exception) { Color(0xFF1E40AF) }
                
                // VERIFICACIÓN DE INVENTARIO: Si está vacío, sembrar 30 productos
                val productCount = db.productoDao().count(active.id)
                if (productCount == 0) {
                    android.util.Log.d("HomeActivity", "Inventario vacío detectado para ${active.id}. Sembrando productos...")
                    com.multipos.app.util.InventorySeeder.seedAbarrotes(db, active.id)
                }

                val membership = db.usuarioEmpresaDao().getActiveMembership(userId, active.id)
                activeRole = membership?.rol
                userRole = activeRole ?: "Sin rol"
                
                if (supportFragmentManager.findFragmentById(R.id.homeContainer) == null) {
                    showDefaultScreen()
                }
            }
        }
    }

    private fun canNavigateTo(menu: String): Boolean {
        val permission = when (menu) {
            "DASHBOARD" -> CompanyPermission.VIEW_DASHBOARD
            "POS" -> CompanyPermission.SELL
            "INVENTORY" -> CompanyPermission.MANAGE_INVENTORY
            "HISTORY" -> CompanyPermission.VIEW_HISTORY
            "CLIENTS" -> CompanyPermission.MANAGE_CLIENT_CREDIT
            "EMPLOYEES" -> CompanyPermission.MANAGE_EMPLOYEES
            "CASH" -> CompanyPermission.MANAGE_CASH
            "REPORTS" -> CompanyPermission.VIEW_REPORTS
            else -> return false
        }
        return CompanyPermissions.allows(activeRole, permission)
    }

    private fun navigateToFragment(menu: String) {
        val fragment: Fragment = when (menu) {
            "DASHBOARD" -> DashboardFragment()
            "POS" -> PosFragment()
            "INVENTORY" -> InventoryFragment()
            "HISTORY" -> HistoryFragment()
            "CLIENTS" -> ClientsFragment()
            "EMPLOYEES" -> EmployeesFragment()
            "CASH" -> CashFragment()
            "REPORTS" -> ReportesFragment()
            else -> return
        }
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.homeContainer, fragment)
            .commit()
    }

    private fun showDefaultScreen() {
        when {
            canNavigateTo("DASHBOARD") -> { selectedMenu = "DASHBOARD"; navigateToFragment("DASHBOARD") }
            canNavigateTo("POS") -> { selectedMenu = "POS"; navigateToFragment("POS") }
            else -> logout()
        }
    }

    private fun showCompanySelector() {
        val db = DatabaseProvider.get(this)
        val userId = UserSessionStore.userId(this)
        lifecycleScope.launch {
            val companies = db.usuarioEmpresaDao().getCompaniesForUser(userId).first()
            val options = companies.map { it.nombre }.toTypedArray()
            
            AlertDialog.Builder(this@HomeActivity)
                .setTitle("Seleccionar Empresa")
                .setItems(options) { _, which ->
                    val selected = companies[which]
                    ActiveCompanyStore.set(this@HomeActivity, selected.id)
                    ActiveCompanyStore.setColor(this@HomeActivity, selected.colorPrimarioHex)
                    recreate()
                }
                .show()
        }
    }

    fun logout() {
        UserSessionStore.clear(this)
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finishAffinity()
    }
}
