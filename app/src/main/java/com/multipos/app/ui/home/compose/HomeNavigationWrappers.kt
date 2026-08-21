package com.multipos.app.ui.home.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.multipos.app.data.CashRepository
import com.multipos.app.data.UserSessionStore
import com.multipos.app.ui.cash.compose.CashScreen
import com.multipos.app.ui.clients.compose.ClientsScreen
import com.multipos.app.ui.dashboard.compose.DashboardScreen
import com.multipos.app.ui.employees.compose.EmployeesScreen
import com.multipos.app.ui.history.compose.HistoryScreen
import com.multipos.app.ui.inventory.compose.InventoryScreen
import com.multipos.app.ui.pos.compose.POSScreen
import com.multipos.app.ui.reports.compose.ReportsScreen
import com.multipos.app.viewmodel.*

// --- WRAPPERS QUE GESTIONAN EL ESTADO Y LO PASAN AL SCREEN ---

@Composable
fun DashboardScreenWrapper(
    companyId: String,
    companyName: String,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val db = com.multipos.app.data.DatabaseProvider.get(androidx.compose.ui.platform.LocalContext.current)
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(db, companyId))
    val uiState by viewModel.uiState.collectAsState()

    DashboardScreen(
        modifier = modifier,
        companyName = companyName,
        totalSalesToday = uiState.totalSalesToday,
        lowStockCount = uiState.lowStockCount,
        onLogoutClick = onLogoutClick,
    )
}

@Composable
fun POSScreenWrapper(
    companyId: String,
    userId: Int,
    modifier: Modifier = Modifier,
) {
    val db = com.multipos.app.data.DatabaseProvider.get(androidx.compose.ui.platform.LocalContext.current)
    val viewModel: PosViewModel = viewModel(factory = PosViewModelFactory(db, companyId))
    val uiState by viewModel.uiState.collectAsState()

    POSScreen(
        modifier = modifier,
        products = uiState.filteredProducts,
        cartLines = uiState.cart,
        selectedClient = uiState.selectedClient,
        total = uiState.total,
        searchQuery = uiState.searchQuery,
        isLoading = uiState.isLoading,
        warning = uiState.warning,
        onSearchChange = viewModel::onSearchQueryChange,
        onAddToCart = viewModel::addToCart,
        onIncreaseQuantity = { viewModel.updateQuantity(it, 1) },
        onDecreaseQuantity = { viewModel.updateQuantity(it, -1) },
        onRemoveFromCart = viewModel::removeFromCart,
        onPaymentMethodSelected = viewModel::setPaymentMethod,
        onChargeClick = { viewModel.processSale(userId) },
        onScanProduct = { /* Navigate to scanner or logic */ },
        onClearWarning = viewModel::clearWarning,
    )
}

@Composable
fun InventoryScreenWrapper(
    companyId: String,
    modifier: Modifier = Modifier,
) {
    val db = com.multipos.app.data.DatabaseProvider.get(androidx.compose.ui.platform.LocalContext.current)
    val viewModel: InventoryViewModel = viewModel(factory = InventoryViewModelFactory(db.productoDao(), companyId))
    val uiState by viewModel.uiState.collectAsState()

    InventoryScreen(
        modifier = modifier,
        products = uiState.filteredProducts,
        searchQuery = uiState.searchQuery,
        isLoading = uiState.isLoading,
        onSearchChange = viewModel::onSearchQueryChange,
        onAddProductClick = { /* Navigate */ },
        onEditProductClick = { /* Navigate */ },
        onDeleteProductClick = { /* Logic */ },
        onMovementsClick = { /* Navigate */ },
        onScanClick = { /* Navigate */ },
    )
}

@Composable
fun HistoryScreenWrapper(
    companyId: String,
    modifier: Modifier = Modifier,
) {
    val db = com.multipos.app.data.DatabaseProvider.get(androidx.compose.ui.platform.LocalContext.current)
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(db.ventaDao(), companyId))
    val uiState by viewModel.uiState.collectAsState()

    HistoryScreen(
        modifier = modifier,
        sales = uiState.filteredSales,
        totalToday = uiState.totalToday,
        searchQuery = uiState.searchQuery,
        isLoading = uiState.isLoading,
        onSearchChange = viewModel::onSearchQueryChange,
        onSaleClick = { /* Navigate to detail */ },
    )
}

@Composable
fun ClientsScreenWrapper(
    companyId: String,
    modifier: Modifier = Modifier,
) {
    val db = com.multipos.app.data.DatabaseProvider.get(androidx.compose.ui.platform.LocalContext.current)
    val viewModel: ClientsViewModel = viewModel(factory = ClientsViewModelFactory(db.clienteDao(), companyId))
    val uiState by viewModel.uiState.collectAsState()

    ClientsScreen(
        modifier = modifier,
        clients = uiState.filteredClients,
        searchQuery = uiState.searchQuery,
        isLoading = uiState.isLoading,
        onSearchChange = viewModel::onSearchQueryChange,
        onAddClientClick = { /* Navigate */ },
        onEditClientClick = { /* Navigate */ },
        onViewStatementClick = { /* Navigate */ },
    )
}

@Composable
fun EmployeesScreenWrapper(
    companyId: String,
    modifier: Modifier = Modifier,
) {
    val db = com.multipos.app.data.DatabaseProvider.get(androidx.compose.ui.platform.LocalContext.current)
    val viewModel: EmployeesViewModel = viewModel(factory = EmployeesViewModelFactory(db.usuarioDao(), db.usuarioEmpresaDao(), companyId))
    val uiState by viewModel.uiState.collectAsState()

    EmployeesScreen(
        modifier = modifier,
        employees = uiState.filteredEmployees,
        searchQuery = uiState.searchQuery,
        isLoading = uiState.isLoading,
        onSearchChange = viewModel::onSearchQueryChange,
        onAddEmployeeClick = { /* Navigate */ },
        onEditEmployeeClick = { /* Navigate */ },
    )
}

@Composable
fun CashScreenWrapper(
    companyId: String,
    userId: Int,
    modifier: Modifier = Modifier,
) {
    val db = com.multipos.app.data.DatabaseProvider.get(androidx.compose.ui.platform.LocalContext.current)
    val repository = CashRepository(db)
    val viewModel: CashViewModel = viewModel(factory = CashViewModelFactory(repository, companyId, userId))
    val uiState by viewModel.uiState.collectAsState()

    CashScreen(
        modifier = modifier,
        isCashOpen = uiState.session != null,
        expectedBalance = uiState.expected,
        ingresos = uiState.ingresos,
        egresos = uiState.egresos,
        movements = uiState.movements,
        onOpenCashClick = { viewModel.openSession(0L) },
        onCloseCashClick = { viewModel.closeSession(0L, "") },
        onAddIncomeClick = { viewModel.addManualMovement("INGRESO_MANUAL", 0L, "") },
        onAddExpenseClick = { viewModel.addManualMovement("EGRESO_MANUAL", 0L, "") },
    )
}

@Composable
fun ReportsScreenWrapper(
    companyId: String,
    modifier: Modifier = Modifier,
) {
    val db = com.multipos.app.data.DatabaseProvider.get(androidx.compose.ui.platform.LocalContext.current)
    val userId = UserSessionStore.userId(androidx.compose.ui.platform.LocalContext.current)
    val viewModel: ReportsViewModel = viewModel(factory = ReportsViewModelFactory(db, companyId, userId))
    val uiState by viewModel.uiState.collectAsState()

    ReportsScreen(
        modifier = modifier,
        reportType = uiState.reportType.name,
        reportData = uiState.reportData,
        isLoading = uiState.isLoading,
        onGenerateReport = viewModel::generateReport,
        onExportCsv = viewModel::exportCsv,
    )
}
