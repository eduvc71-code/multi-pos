package com.multipos.app.ui.home 
 
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.content.res.ColorStateList
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.multipos.app.R 
import com.multipos.app.databinding.ActivityHomeBinding
import com.multipos.app.ui.inventory.InventoryFragment
import com.multipos.app.ui.pos.PosFragment
import com.multipos.app.ui.history.HistoryFragment
import com.multipos.app.ui.history.SaleDetailFragment
import com.multipos.app.ui.clients.ClientsFragment
import com.multipos.app.ui.dashboard.DashboardFragment
import com.multipos.app.ui.employees.EmployeesFragment
import com.multipos.app.ui.cash.CashFragment
import com.multipos.app.ui.reports.ReportesFragment
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import com.multipos.app.ui.login.LoginActivity
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions
import androidx.room.withTransaction
import com.multipos.app.security.SessionPolicy
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.UUID
 
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var activeRole: String? = null
    private var navigationInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) { 
        super.onCreate(savedInstanceState) 
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val db = DatabaseProvider.get(this)
        val userId = UserSessionStore.userId(this)
        if (userId == 0) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        // Expiración de sesión (12 h máx. / 30 min inactividad) antes de operar.
        if (isSessionExpired()) {
            UserSessionStore.clear(this)
            Toast.makeText(this, R.string.session_expired, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
            return
        }
        UserSessionStore.touchActivity(this)
        lifecycleScope.launch {
            val user = db.usuarioDao().getById(userId)
            if (user == null) {
                logout()
                return@launch
            }
            db.usuarioEmpresaDao().getCompaniesForUser(userId).collect { companies ->
                val active = companies.firstOrNull { it.id == ActiveCompanyStore.get(this@HomeActivity) } ?: companies.firstOrNull()
                if (active == null) {
                    Toast.makeText(this@HomeActivity, "Tu usuario no tiene una empresa activa", Toast.LENGTH_LONG).show()
                    logout()
                    return@collect
                }
                if (active.id != ActiveCompanyStore.get(this@HomeActivity)) {
                    ActiveCompanyStore.set(this@HomeActivity, active.id)
                }
                val membership = db.usuarioEmpresaDao().getActiveMembership(userId, active.id)
                if (membership == null) {
                    logout()
                    return@collect
                }
                activeRole = membership.rol
                binding.txtSessionUser.text = getString(R.string.session_user_format, user.nombre, formatRole(membership.rol))
                binding.btnCompany.text = getString(R.string.company_format, active.nombre)
                ActiveCompanyStore.setColor(this@HomeActivity, active.colorPrimarioHex)
                applyCompanyColor(binding, active.colorPrimarioHex)
                applyPermissions(membership.rol)

                val current = supportFragmentManager.findFragmentById(R.id.homeContainer)
                if (!navigationInitialized || current == null || !isFragmentAllowed(current, membership.rol)) {
                    showDefaultScreen(membership.rol)
                }
                navigationInitialized = true
            }
        }
        binding.btnCompany.setOnClickListener {
            lifecycleScope.launch {
                val companies = db.usuarioEmpresaDao().getCompaniesForUser(userId).first()
                val canCreate = CompanyPermissions.allows(activeRole, CompanyPermission.CREATE_COMPANY)
                val options = companies.map { it.nombre }.toMutableList().apply { if (canCreate) add("+ Registrar nueva empresa") }
                AlertDialog.Builder(this@HomeActivity).setTitle(R.string.select_company_title).setItems(options.toTypedArray()) { _, which ->
                    if (canCreate && which == companies.size) showCreateCompanyDialog(db, userId) else { ActiveCompanyStore.set(this@HomeActivity, companies[which].id); ActiveCompanyStore.setColor(this@HomeActivity, companies[which].colorPrimarioHex); recreate() }
                }.show()
            }
        }
        binding.btnLogout.setOnClickListener { logout() }
        hideNavigationUntilMembershipLoads()
        binding.btnDashboard.setOnClickListener { navigate(CompanyPermission.VIEW_DASHBOARD) { DashboardFragment() } }
        binding.btnSales.setOnClickListener { navigate(CompanyPermission.SELL) { PosFragment() } }
        binding.btnInventory.setOnClickListener { navigate(CompanyPermission.MANAGE_INVENTORY) { InventoryFragment() } }
        binding.btnHistory.setOnClickListener { navigate(CompanyPermission.VIEW_HISTORY) { HistoryFragment() } }
        binding.btnClients.setOnClickListener { navigate(CompanyPermission.MANAGE_CLIENT_CREDIT) { ClientsFragment() } }
        binding.btnEmployees.setOnClickListener { navigate(CompanyPermission.MANAGE_EMPLOYEES) { EmployeesFragment() } }
        binding.btnCash.setOnClickListener { navigate(CompanyPermission.MANAGE_CASH) { CashFragment() } }
        binding.btnReports.setOnClickListener { navigate(CompanyPermission.VIEW_REPORTS) { ReportesFragment() } }
    }

    private fun hideNavigationUntilMembershipLoads() {
        listOf(
            binding.btnDashboard,
            binding.btnSales,
            binding.btnInventory,
            binding.btnHistory,
            binding.btnClients,
            binding.btnEmployees,
            binding.btnCash,
            binding.btnReports
        ).forEach { it.visibility = View.GONE }
    }

    private fun applyPermissions(role: String) {
        binding.btnDashboard.visibility = visibilityFor(role, CompanyPermission.VIEW_DASHBOARD)
        binding.btnSales.visibility = visibilityFor(role, CompanyPermission.SELL)
        binding.btnInventory.visibility = visibilityFor(role, CompanyPermission.MANAGE_INVENTORY)
        binding.btnHistory.visibility = visibilityFor(role, CompanyPermission.VIEW_HISTORY)
        binding.btnClients.visibility = visibilityFor(role, CompanyPermission.MANAGE_CLIENT_CREDIT)
        binding.btnEmployees.visibility = visibilityFor(role, CompanyPermission.MANAGE_EMPLOYEES)
        binding.btnCash.visibility = visibilityFor(role, CompanyPermission.MANAGE_CASH)
        binding.btnReports.visibility = visibilityFor(role, CompanyPermission.VIEW_REPORTS)
    }

    private fun visibilityFor(role: String?, permission: CompanyPermission): Int =
        if (CompanyPermissions.allows(role, permission)) View.VISIBLE else View.GONE

    private fun navigate(permission: CompanyPermission, fragment: () -> Fragment) {
        if (!CompanyPermissions.allows(activeRole, permission)) {
            Toast.makeText(this, "No tienes permiso para abrir esta sección", Toast.LENGTH_SHORT).show()
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.homeContainer, fragment())
            .commit()
    }

    private fun showDefaultScreen(role: String) {
        when {
            CompanyPermissions.allows(role, CompanyPermission.VIEW_DASHBOARD) ->
                navigate(CompanyPermission.VIEW_DASHBOARD) { DashboardFragment() }
            CompanyPermissions.allows(role, CompanyPermission.SELL) ->
                navigate(CompanyPermission.SELL) { PosFragment() }
            else -> logout()
        }
    }

    private fun isFragmentAllowed(fragment: Fragment, role: String): Boolean {
        val permission = when (fragment) {
            is DashboardFragment -> CompanyPermission.VIEW_DASHBOARD
            is PosFragment -> CompanyPermission.SELL
            is InventoryFragment -> CompanyPermission.MANAGE_INVENTORY
            is HistoryFragment -> CompanyPermission.VIEW_HISTORY
            is SaleDetailFragment -> CompanyPermission.VIEW_HISTORY
            is ClientsFragment -> CompanyPermission.MANAGE_CLIENT_CREDIT
            is EmployeesFragment -> CompanyPermission.MANAGE_EMPLOYEES
            is CashFragment -> CompanyPermission.MANAGE_CASH
            is ReportesFragment -> CompanyPermission.VIEW_REPORTS
            else -> return false
        }
        return CompanyPermissions.allows(role, permission)
    }

    private fun applyCompanyColor(binding: ActivityHomeBinding, hex: String) {
        val color = runCatching { Color.parseColor(hex) }.getOrDefault(Color.rgb(37, 99, 235))
        val tint = ColorStateList.valueOf(color)
        val white = ContextCompat.getColor(this, R.color.white)
        val black = ContextCompat.getColor(this, R.color.black)
        val contrastBackground = ColorUtils.compositeColors(color, ContextCompat.getColor(this, R.color.surface))
        val textColor = if (ColorUtils.calculateContrast(white, contrastBackground) >= 4.5) white else black
        binding.btnCompany.backgroundTintList = tint
        binding.btnCompany.setTextColor(textColor)
        listOf(binding.btnDashboard, binding.btnSales, binding.btnInventory, binding.btnHistory, binding.btnClients, binding.btnEmployees, binding.btnCash).forEach { it.backgroundTintList = tint; it.setTextColor(textColor) }
        window.statusBarColor = darken(color)
    }

    private fun darken(color: Int): Int = Color.rgb((Color.red(color) * .75).toInt(), (Color.green(color) * .75).toInt(), (Color.blue(color) * .75).toInt())

    private fun showCreateCompanyDialog(db: com.multipos.app.data.AppDatabase, userId: Int) {
        val name = EditText(this).apply { hint = "Nombre de la empresa"; setPadding(24, 12, 24, 12) }
        val type = Spinner(this).apply {
            adapter = ArrayAdapter(this@HomeActivity, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.business_types).toList())
        }
        val color = EditText(this).apply { hint = getString(R.string.corporate_color_hint); setText(R.string.corporate_color_default); setPadding(24, 12, 24, 12) }
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(name); addView(type); addView(color) }
        val dialog = AlertDialog.Builder(this).setTitle(R.string.register_company_title).setView(form).setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.save_button, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val companyName = name.text.toString().trim(); val hex = color.text.toString().trim().uppercase()
                val validColor = runCatching { Color.parseColor(hex); true }.getOrDefault(false)
                if (companyName.isBlank() || !validColor) { Toast.makeText(this, R.string.invalid_company_name_or_color, Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                lifecycleScope.launch {
                    val company = Empresa(UUID.randomUUID().toString(), companyName, type.selectedItem.toString(), hex)
                    db.withTransaction {
                        db.empresaDao().insert(company)
                        db.usuarioEmpresaDao().insert(UsuarioEmpresa(userId, company.id, Usuario.ROL_PROPIETARIO))
                    }
                    ActiveCompanyStore.set(this@HomeActivity, company.id); ActiveCompanyStore.setColor(this@HomeActivity, company.colorPrimarioHex); dialog.dismiss(); recreate()
                }
            }
        }
        dialog.show()
    }

    private fun formatRole(role: String): String = role.lowercase().replaceFirstChar(Char::uppercase)

    private fun logout() {
        UserSessionStore.clear(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    override fun onResume() {
        super.onResume()
        // Revalida al reanudar la app (vuelta del fondo o actividad restaurada).
        if (UserSessionStore.isAuthenticated(this) && isSessionExpired()) {
            UserSessionStore.clear(this)
            Toast.makeText(this, R.string.session_expired, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun isSessionExpired(): Boolean {
        val startedAt = UserSessionStore.sessionStartedAt(this)
        val lastActivity = UserSessionStore.lastActivityAt(this)
        return SessionPolicy.isSessionExpired(startedAt, lastActivity, System.currentTimeMillis())
    }
} 
