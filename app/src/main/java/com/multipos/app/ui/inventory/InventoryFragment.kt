package com.multipos.app.ui.inventory

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.InventoryMovementRepository
import com.multipos.app.data.InventoryMovementRequest
import com.multipos.app.data.api.ProductLookupService
import com.multipos.app.data.entities.MovimientoInventario
import com.multipos.app.data.entities.Producto
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import com.multipos.app.ui.components.MultiPOSTextField
import com.multipos.app.ui.inventory.compose.InventoryScreen
import com.multipos.app.ui.scanner.ScannerActivity
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import com.multipos.app.viewmodel.InventoryViewModel
import com.multipos.app.viewmodel.InventoryViewModelFactory
import kotlinx.coroutines.launch

class InventoryFragment : Fragment() {
    private lateinit var viewModel: InventoryViewModel
    private var scanConsumer: ((String, String) -> Unit)? = null

    private var showProductDialog by mutableStateOf(false)
    private var showMovementDialog by mutableStateOf(false)
    private var selectedProduct by mutableStateOf<Producto?>(null)
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
        val factory = InventoryViewModelFactory(db.productoDao(), companyId)
        viewModel = ViewModelProvider(this, factory)[InventoryViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        InventoryScreen(
                            products = state.filteredProducts,
                            searchQuery = state.searchQuery,
                            isLoading = state.isLoading,
                            onSearchChange = { viewModel.onSearchQueryChange(it) },
                            onAddProductClick = {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    if (ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.MANAGE_INVENTORY)) {
                                        selectedProduct = null
                                        showProductDialog = true
                                    }
                                }
                            },
                            onEditProductClick = { product ->
                                selectedProduct = product
                                showProductDialog = true
                            },
                            onDeleteProductClick = { product ->
                                confirmDeleteProduct(product, db)
                            },
                            onMovementsClick = {
                                showMovementDialog = true
                            },
                            onScanClick = {
                                openScanner(db, companyId)
                            }
                        )

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
                                    Text("Consultando base de datos global...", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }

                    if (showProductDialog) {
                        ProductDialog(
                            product = selectedProduct,
                            onDismiss = { showProductDialog = false },
                            onSave = { name, code, barcode, price, cost, stock ->
                                saveProduct(db, name, code, barcode, price, cost, stock)
                            },
                            onScan = { 
                                openScanner(db, companyId)
                            }
                        )
                    }
                    if (showMovementDialog) {
                        MovementDialog(
                            products = state.products,
                            onDismiss = { showMovementDialog = false },
                            onSave = { product, tipo, cantidad, motivo ->
                                registerMovement(db, companyId, product, tipo, cantidad, motivo)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun openScanner(db: AppDatabase, companyId: String) {
        val intent = Intent(requireContext(), ScannerActivity::class.java).apply {
            putExtra(ScannerActivity.EXTRA_TITLE, "Buscar Producto")
        }
        
        scanConsumer = { code, _ ->
            viewLifecycleOwner.lifecycleScope.launch {
                val localProduct = db.productoDao().getByBarcodeOnce(code, companyId)
                if (localProduct != null) {
                    selectedProduct = localProduct
                    showProductDialog = true
                } else {
                    isSearchingGlobal = true
                    val globalProduct = ProductLookupService.lookupByBarcode(code, companyId)
                    isSearchingGlobal = false
                    
                    if (globalProduct != null) {
                        selectedProduct = globalProduct
                        showProductDialog = true
                    } else {
                        selectedProduct = Producto(
                            nombre = "",
                            codigo = code,
                            codigoBarras = code,
                            precioVenta = 0,
                            costoUnitario = 0,
                            stock = 0,
                            empresaId = companyId
                        )
                        showProductDialog = true
                    }
                }
            }
        }
        scannerLauncher.launch(intent)
    }

    @Composable
    private fun MovementDialog(
        products: List<Producto>,
        onDismiss: () -> Unit,
        onSave: (Producto, String, Int, String) -> Unit
    ) {
        if (products.isEmpty()) {
            Toast.makeText(requireContext(), "No hay productos", Toast.LENGTH_SHORT).show()
            onDismiss()
            return
        }

        var selectedProduct by remember { mutableStateOf(products[0]) }
        var expandedProduct by remember { mutableStateOf(false) }
        
        val types = listOf(
            MovimientoInventario.TIPO_ENTRADA_MANUAL to "Entrada manual",
            MovimientoInventario.TIPO_SALIDA_MANUAL to "Salida manual",
            MovimientoInventario.TIPO_AJUSTE to "Ajuste de inventario"
        )
        var selectedType by remember { mutableStateOf(types[0]) }
        var expandedType by remember { mutableStateOf(false) }
        
        var cantidad by remember { mutableStateOf("") }
        var motivo by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Movimiento de Inventario") },
            confirmButton = {
                TextButton(onClick = {
                    val cant = cantidad.toIntOrNull() ?: 0
                    if (cant != 0 && motivo.length >= 5) {
                        onSave(selectedProduct, selectedType.first, cant, motivo)
                    } else {
                        Toast.makeText(requireContext(), "Verifique los datos", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box {
                        OutlinedButton(onClick = { expandedProduct = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Producto: ${selectedProduct.nombre}")
                        }
                        DropdownMenu(expanded = expandedProduct, onDismissRequest = { expandedProduct = false }) {
                            products.forEach { p ->
                                DropdownMenuItem(text = { Text(p.nombre) }, onClick = { selectedProduct = p; expandedProduct = false })
                            }
                        }
                    }

                    Box {
                        OutlinedButton(onClick = { expandedType = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Tipo: ${selectedType.second}")
                        }
                        DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                            types.forEach { t ->
                                DropdownMenuItem(text = { Text(t.second) }, onClick = { selectedType = t; expandedType = false })
                            }
                        }
                    }

                    MultiPOSTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it },
                        label = if (selectedType.first == MovimientoInventario.TIPO_AJUSTE) "Nuevo stock" else "Cantidad"
                    )

                    MultiPOSTextField(value = motivo, onValueChange = { motivo = it }, label = "Motivo (mín. 5 car.)")
                }
            }
        )
    }

    private fun registerMovement(db: AppDatabase, companyId: String, product: Producto, tipo: String, cantidad: Int, motivo: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                InventoryMovementRepository(db).registerMovement(
                    InventoryMovementRequest(
                        companyId = companyId,
                        productId = product.id,
                        userId = com.multipos.app.data.UserSessionStore.userId(requireContext()),
                        tipo = tipo,
                        cantidad = cantidad,
                        motivo = motivo
                    )
                )
                showMovementDialog = false
                Toast.makeText(context, "Movimiento registrado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Composable
    private fun ProductDialog(
        product: Producto?,
        onDismiss: () -> Unit,
        onSave: (String, String, String?, Long, Long, Int) -> Unit,
        onScan: () -> Unit
    ) {
        var name by remember(product) { mutableStateOf(product?.nombre ?: "") }
        var code by remember(product) { mutableStateOf(product?.codigo ?: "") }
        var barcode by remember(product) { mutableStateOf(product?.codigoBarras ?: "") }
        var price by remember(product) { mutableStateOf(if (product != null && product.precioVenta > 0) (product.precioVenta / 100.0).toString() else "") }
        var cost by remember(product) { mutableStateOf(if (product != null && product.costoUnitario > 0) (product.costoUnitario / 100.0).toString() else "") }
        var stock by remember(product) { mutableStateOf(product?.stock?.toString() ?: "0") }

        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val p = Money.parseMinorUnits(price) ?: 0L
                    val c = Money.parseMinorUnits(cost) ?: 0L
                    val s = stock.toIntOrNull() ?: 0
                    onSave(name, code, barcode.ifBlank { null }, p, c, s)
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
            title = { Text(if (product?.id == 0 || product == null) "Nuevo Producto" else "Editar Producto") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MultiPOSTextField(value = name, onValueChange = { name = it }, label = "Nombre")
                    MultiPOSTextField(value = code, onValueChange = { code = it }, label = "Código Interno")
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MultiPOSTextField(
                            value = barcode, 
                            onValueChange = { barcode = it }, 
                            label = "Código de Barras",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onScan) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MultiPOSTextField(value = price, onValueChange = { price = it }, label = "Precio", modifier = Modifier.weight(1f))
                        MultiPOSTextField(value = cost, onValueChange = { cost = it }, label = "Costo", modifier = Modifier.weight(1f))
                    }
                    MultiPOSTextField(value = stock, onValueChange = { stock = it }, label = "Stock inicial")
                }
            }
        )
    }

    private fun saveProduct(db: AppDatabase, name: String, code: String, barcode: String?, price: Long, cost: Long, stock: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val companyId = ActiveCompanyStore.get(requireContext())
            val product = Producto(
                id = selectedProduct?.id ?: 0,
                nombre = name,
                codigo = code,
                codigoBarras = barcode,
                precioVenta = price,
                costoUnitario = cost,
                stock = stock,
                empresaId = companyId,
                categoria = selectedProduct?.categoria ?: "General",
                stockMinimo = selectedProduct?.stockMinimo ?: 5,
                fotoUrl = selectedProduct?.fotoUrl ?: ""
            )
            try {
                if (product.id == 0) db.productoDao().insert(product)
                else db.productoDao().update(product)
                showProductDialog = false
                Toast.makeText(context, "Producto guardado", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteConstraintException) {
                Toast.makeText(context, "Código ya existe", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteProduct(product: Producto, db: AppDatabase) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar producto")
            .setMessage("¿Estás seguro?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    db.productoDao().archive(product.id, product.empresaId)
                    Toast.makeText(requireContext(), "Eliminado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
