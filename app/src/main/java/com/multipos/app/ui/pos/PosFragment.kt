package com.multipos.app.ui.pos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.multipos.app.R
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.api.ProductLookupService
import com.multipos.app.data.entities.Producto
import com.multipos.app.ui.components.MultiPOSTextField
import com.multipos.app.ui.pos.compose.POSScreen
import com.multipos.app.ui.pos.compose.SaleSuccessDialog
import com.multipos.app.ui.scanner.ScannerActivity
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import com.multipos.app.viewmodel.PosViewModel
import com.multipos.app.viewmodel.PosViewModelFactory
import kotlinx.coroutines.launch

class PosFragment : Fragment() {
    private lateinit var viewModel: PosViewModel
    private var scanConsumer: ((String, String) -> Unit)? = null

    private var showQuickAddDialog by mutableStateOf(false)
    private var foundProduct by mutableStateOf<Producto?>(null)
    private var isSearchingGlobal by mutableStateOf(false)

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val code = result.data?.getStringExtra(ScannerActivity.EXTRA_SCAN_RESULT).orEmpty()
            val format = result.data?.getStringExtra(ScannerActivity.EXTRA_SCAN_FORMAT).orEmpty()
            if (code.isNotBlank()) scanConsumer?.invoke(code, format)
        }
        scanConsumer = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        val factory = PosViewModelFactory(db, companyId)
        viewModel = ViewModelProvider(this, factory)[PosViewModel::class.java]
        val userId = com.multipos.app.data.UserSessionStore.userId(requireContext())

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        POSScreen(
                            products = state.filteredProducts,
                            cartLines = state.cart,
                            selectedClient = state.selectedClient,
                            paymentMethod = state.paymentMethod,
                            total = state.total,
                            searchQuery = state.searchQuery,
                            isLoading = state.isLoading,
                            warning = state.warning, // Pasamos el warning
                            onSearchChange = { viewModel.onSearchQueryChange(it) },
                            onAddToCart = { product, qty -> viewModel.addToCart(product, qty) },
                            onIncreaseQuantity = { viewModel.updateQuantity(it, 1) },
                            onDecreaseQuantity = { viewModel.updateQuantity(it, -1) },
                            onRemoveFromCart = { viewModel.removeFromCart(it) },
                            onClearCart = { viewModel.clearCart() },
                            onPaymentMethodSelected = { viewModel.setPaymentMethod(it) },
                            onClientSelected = { viewModel.setClient(it) },
                            onChargeClick = { 
                                viewModel.processSale(userId)
                            },
                            onScanProduct = { 
                                scanConsumer = { code, _ ->
                                    handleProductScan(db, companyId, code)
                                }
                                openScanner("Escanear producto")
                            },
                            onScanClientQr = { 
                                scanConsumer = { code, _ ->
                                    viewModel.setClient(code)
                                    Toast.makeText(context, "Cliente: $code", Toast.LENGTH_SHORT).show()
                                }
                                openScanner("Escanear QR de cliente")
                            },
                            onClearWarning = { viewModel.clearWarning() } // Acción para limpiar
                        )

                        // Dialogo de Éxito / Ticket
                        state.lastSaleId?.let { saleId ->
                            SaleSuccessDialog(
                                folio = "#${saleId.toString().padStart(6, '0')}",
                                total = state.lastSaleTotal, 
                                onDismiss = { viewModel.clearLastSale() }
                            )
                        }

                        if (state.error != null) {
                            Toast.makeText(context, "Error: ${state.error}", Toast.LENGTH_LONG).show()
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
                                    Text("Buscando producto en la red...", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        if (showQuickAddDialog) {
                            QuickAddDialog(
                                product = foundProduct!!,
                                onDismiss = { showQuickAddDialog = false },
                                onAdd = { price ->
                                    saveAndAddToCart(db, price)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleProductScan(db: com.multipos.app.data.AppDatabase, companyId: String, code: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            // 1. Buscar local
            val local = db.productoDao().getByBarcodeOnce(code, companyId)
            if (local != null) {
                viewModel.addToCart(local, 1) // Por defecto 1 al escanear
                Toast.makeText(context, "Agregado: ${local.nombre}", Toast.LENGTH_SHORT).show()
            } else {
                // 2. Buscar Global
                isSearchingGlobal = true
                val global = ProductLookupService.lookupByBarcode(code, companyId)
                isSearchingGlobal = false
                
                if (global != null) {
                    foundProduct = global
                    showQuickAddDialog = true
                } else {
                    Toast.makeText(context, "Producto no encontrado. Regístralo en Inventario.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Composable
    private fun QuickAddDialog(product: Producto, onDismiss: () -> Unit, onAdd: (Long) -> Unit) {
        var price by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Producto Detectado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.nombre, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Este producto no está en tu inventario. Ingresa un precio para venderlo ahora y guardarlo.")
                    MultiPOSTextField(value = price, onValueChange = { price = it }, label = "Precio de Venta")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = Money.parseMinorUnits(price) ?: 0L
                    if (p > 0) onAdd(p)
                    else Toast.makeText(context, "Ingresa un precio válido", Toast.LENGTH_SHORT).show()
                }) { Text("AGREGAR Y GUARDAR") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
        )
    }

    private fun saveAndAddToCart(db: com.multipos.app.data.AppDatabase, price: Long) {
        val product = foundProduct?.copy(precioVenta = price, stock = 100) ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            db.productoDao().insert(product)
            val saved = db.productoDao().getByBarcodeOnce(product.codigo, product.empresaId)
            if (saved != null) {
                viewModel.addToCart(saved, 1)
                showQuickAddDialog = false
                Toast.makeText(context, "Producto guardado y agregado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openScanner(title: String) {
        scannerLauncher.launch(Intent(requireContext(), ScannerActivity::class.java).putExtra(ScannerActivity.EXTRA_TITLE, title))
    }
}
