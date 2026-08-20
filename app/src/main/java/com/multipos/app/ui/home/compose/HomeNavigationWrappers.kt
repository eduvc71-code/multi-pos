package com.multipos.app.ui.home.compose

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.UserSessionStore
import com.multipos.app.ui.cash.compose.CashScreen
import com.multipos.app.ui.clients.compose.ClientsScreen
import com.multipos.app.ui.dashboard.compose.DashboardScreen
import com.multipos.app.ui.employees.compose.EmployeesScreen
import com.multipos.app.ui.history.compose.HistoryScreen
import com.multipos.app.ui.inventory.compose.InventoryScreen
import com.multipos.app.ui.pos.PosViewModel
import com.multipos.app.ui.pos.compose.POSScreen
import com.multipos.app.ui.pos.compose.SaleSuccessDialog
import com.multipos.app.ui.reports.compose.ReportsScreen
import com.multipos.app.ui.scanner.ScannerActivity
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.viewmodel.PosViewModelFactory
import kotlinx.coroutines.launch

// ==================== POS Wrapper ====================
@Composable
fun POSScreenWrapper(
    companyId: String,
    userId: Int,
    companyColor: Color
) {
    val context = LocalContext.current
    val db = DatabaseProvider.get(context)
    val factory = remember(companyId) { PosViewModelFactory(db, companyId) }
    val viewModel: PosViewModel = viewModel(factory = factory)
    
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var foundProduct by remember { mutableStateOf<com.multipos.app.data.entities.Producto?>(null) }
    var isSearchingGlobal by remember { mutableStateOf(false) }
    var scanCallback by remember { mutableStateOf<((String, String) -> Unit)?>(null) }
    
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val code = result.data?.getStringExtra(ScannerActivity.EXTRA_SCAN_RESULT).orEmpty()
            val format = result.data?.getStringExtra(ScannerActivity.EXTRA_SCAN_FORMAT).orEmpty()
            if (code.isNotBlank()) scanCallback?.invoke(code, format)
        }
        scanCallback = null
    }
    
    fun openScanner(title: String) {
        val intent = Intent(context, ScannerActivity::class.java).putExtra(ScannerActivity.EXTRA_TITLE, title)
        scannerLauncher.launch(intent)
    }
    
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize()) {
        POSScreen(
            products = state.filteredProducts,
            cartLines = state.cart,
            selectedClient = state.selectedClient,
            paymentMethod = state.paymentMethod,
            total = state.total,
            searchQuery = state.searchQuery,
            isLoading = state.isLoading,
            warning = state.warning,
            onSearchChange = { viewModel.onSearchQueryChange(it) },
            onAddToCart = { product, qty -> viewModel.addToCart(product, qty) },
            onIncreaseQuantity = { viewModel.updateQuantity(it, 1) },
            onDecreaseQuantity = { viewModel.updateQuantity(it, -1) },
            onRemoveFromCart = { viewModel.removeFromCart(it) },
            onClearCart = { viewModel.clearCart() },
            onPaymentMethodSelected = { viewModel.setPaymentMethod(it) },
            onClientSelected = { viewModel.setClient(it) },
            onChargeClick = { viewModel.processSale(userId) },
            onScanProduct = {
                scanCallback = { code, _ ->
                    scope.launch {
                        val local = db.productoDao().getByBarcodeOnce(code, companyId)
                        if (local != null) {
                            viewModel.addToCart(local, 1)
                        } else {
                            isSearchingGlobal = true
                            val global = com.multipos.app.data.api.ProductLookupService.lookupByBarcode(code, companyId)
                            isSearchingGlobal = false
                            if (global != null) {
                                foundProduct = global
                                showQuickAddDialog = true
                            }
                        }
                    }
                }
                openScanner("Escanear producto")
            },
            onScanClientQr = {
                scanCallback = { code, _ ->
                    viewModel.setClient(code)
                }
                openScanner("Escanear QR de cliente")
            },
            onClearWarning = { viewModel.clearWarning() }
        )
        
        state.lastSaleId?.let { saleId ->
            SaleSuccessDialog(
                folio = "#${saleId.toString().padStart(6, '0')}",
                total = state.lastSaleTotal,
                onDismiss = { viewModel.clearLastSale() }
            )
        }
        
        if (isSearchingGlobal) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.product_detected_searching), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        
        if (showQuickAddDialog && foundProduct != null) {
            QuickAddDialog(
                product = foundProduct!!,
                onDismiss = { showQuickAddDialog = false },
                onAdd = { price ->
                    val product = foundProduct?.copy(precioVenta = price, stock = 100)
                    if (product != null) {
                        scope.launch {
                            db.productoDao().insert(product)
                            val saved = db.productoDao().getByBarcodeOnce(product.codigo, product.empresaId)
                            if (saved != null) {
                                viewModel.addToCart(saved, 1)
                                showQuickAddDialog = false
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickAddDialog(
    product: com.multipos.app.data.entities.Producto,
    onDismiss: () -> Unit,
    onAdd: (Long) -> Unit
) {
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.product_detected_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(product.nombre, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(stringResource(R.string.product_detected_message))
                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text(stringResource(R.string.product_detected_price_label)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = com.multipos.app.util.Money.parseMinorUnits(price) ?: 0L
                if (p > 0) onAdd(p)
            }) { Text(stringResource(R.string.product_detected_add_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}

// ==================== Inventory Wrapper ====================
@Composable
fun InventoryScreenWrapper(
    companyId: String,
    companyColor: Color
) {
    val context = LocalContext.current
    val db = DatabaseProvider.get(context)
    InventoryScreen(
        companyId = companyId,
        accentColor = companyColor
    )
}

// ==================== History Wrapper ====================
@Composable
fun HistoryScreenWrapper(
    companyId: String,
    companyColor: Color
) {
    val context = LocalContext.current
    val db = DatabaseProvider.get(context)
    HistoryScreen(
        companyId = companyId,
        accentColor = companyColor
    )
}

// ==================== Clients Wrapper ====================
@Composable
fun ClientsScreenWrapper(
    companyId: String,
    companyColor: Color
) {
    val context = LocalContext.current
    val db = DatabaseProvider.get(context)
    ClientsScreen(
        companyId = companyId,
        accentColor = companyColor
    )
}

// ==================== Employees Wrapper ====================
@Composable
fun EmployeesScreenWrapper(
    companyId: String,
    companyColor: Color
) {
    val context = LocalContext.current
    val db = DatabaseProvider.get(context)
    EmployeesScreen(
        companyId = companyId,
        accentColor = companyColor
    )
}

// ==================== Cash Wrapper ====================
@Composable
fun CashScreenWrapper(
    companyId: String,
    userId: Int,
    companyColor: Color
) {
    val context = LocalContext.current
    val db = DatabaseProvider.get(context)
    CashScreen(
        companyId = companyId,
        userId = userId,
        accentColor = companyColor
    )
}

// ==================== Reports Wrapper ====================
@Composable
fun ReportsScreenWrapper(
    companyId: String,
    companyColor: Color
) {
    val context = LocalContext.current
    val db = DatabaseProvider.get(context)
    ReportsScreen(
        companyId = companyId,
        accentColor = companyColor
    )
}
