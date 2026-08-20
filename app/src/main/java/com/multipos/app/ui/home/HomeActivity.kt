package com.multipos.app.ui.home

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions
import com.multipos.app.ui.cash.compose.CashScreen
import com.multipos.app.ui.clients.compose.ClientsScreen
import com.multipos.app.ui.dashboard.compose.DashboardScreen
import com.multipos.app.ui.employees.compose.EmployeesScreen
import com.multipos.app.ui.history.compose.HistoryScreen
import com.multipos.app.ui.home.compose.HomeScreen
import com.multipos.app.ui.inventory.compose.InventoryScreen
import com.multipos.app.ui.login.LoginActivity
import com.multipos.app.ui.pos.compose.POSScreen
import com.multipos.app.ui.reports.compose.ReportsScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {
    
    private var activeCompanyName by mutableStateOf("Cargando...")
    private var userName by mutableStateOf("...")
    private var userRole by mutableStateOf("...")
    private var companyColor by mutableStateOf(Color(0xFF1E40AF))
    private var selectedMenu by mutableStateOf("DASHBOARD")
    private var activeRole: String? = null
    private var companyId by mutableStateOf("")

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
                val navController = rememberNavController()
                
                // Estado de sesión compartido
                LaunchedEffect(Unit) {
                    loadSessionData()
                }
                
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
                            navController.navigate(menu) {
                                popUpTo("DASHBOARD") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            Toast.makeText(this@HomeActivity, "No tienes permiso", Toast.LENGTH_SHORT).show()
                        }
                    },
                    selectedMenu = selectedMenu
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = getStartDestination(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("DASHBOARD") {
                            DashboardScreen(
                                companyName = activeCompanyName,
                                userRole = userRole,
                                companyColor = companyColor
                            )
                        }
                        composable("POS") {
                            POSScreenWrapper(
                                companyId = companyId,
                                userId = userId,
                                companyColor = companyColor
                            )
                        }
                        composable("INVENTORY") {
                            InventoryScreenWrapper(
                                companyId = companyId,
                                companyColor = companyColor
                            )
                        }
                        composable("HISTORY") {
                            HistoryScreenWrapper(
                                companyId = companyId,
                                companyColor = companyColor
                            )
                        }
                        composable("CLIENTS") {
                            ClientsScreenWrapper(
                                companyId = companyId,
                                companyColor = companyColor
                            )
                        }
                        composable("EMPLOYEES") {
                            EmployeesScreenWrapper(
                                companyId = companyId,
                                companyColor = companyColor
                            )
                        }
                        composable("CASH") {
                            CashScreenWrapper(
                                companyId = companyId,
                                userId = userId,
                                companyColor = companyColor
                            )
                        }
                        composable("REPORTS") {
                            ReportsScreenWrapper(
                                companyId = companyId,
                                companyColor = companyColor
                            )
                        }
                    }
                }
            }
        }
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

                companyId = active.id
                val membership = db.usuarioEmpresaDao().getActiveMembership(userId, active.id)
                activeRole = membership?.rol
                userRole = activeRole ?: "Sin rol"
            }
        }
    }

    private fun getStartDestination(): String {
        return when {
            canNavigateTo("DASHBOARD") -> "DASHBOARD"
            canNavigateTo("POS") -> "POS"
            else -> "DASHBOARD"
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
