package com.multipos.app.ui.pos.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.multipos.app.data.entities.Producto
import com.multipos.app.ui.components.CartItemCard
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.components.MultiPOSSearchField
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.ui.theme.success
import com.multipos.app.ui.theme.warning

data class CartLine(
    val productId: Int,
    val productName: String,
    val price: Long,
    val quantity: Int,
    val product: Producto? = null
)

data class CartItem(
    val product: com.multipos.app.data.entities.Producto,
    val quantity: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    products: List<Producto>,
    cartLines: List<CartLine>,
    selectedClient: String?,
    paymentMethod: String,
    total: Long,
    searchQuery: String,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onAddToCart: (Producto) -> Unit,
    onIncreaseQuantity: (Int) -> Unit,
    onDecreaseQuantity: (Int) -> Unit,
    onRemoveFromCart: (Int) -> Unit,
    onClearCart: () -> Unit,
    onPaymentMethodSelected: (String) -> Unit,
    onClientSelected: (String) -> Unit,
    onChargeClick: () -> Unit,
    onScanProduct: () -> Unit,
    onScanClientQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPaymentOptions by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = modifier.imePadding(), // Evita que el teclado tape el resumen del carrito
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Punto de Venta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onScanProduct) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Escanear producto"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Panel izquierdo - Catálogo de productos
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Buscador
                MultiPOSSearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Buscar producto por nombre o código..."
                )
                
                // Lista de productos
                Card(
                    modifier = Modifier.fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(products) { product ->
                            ProductListItem(
                                product = product,
                                onClick = { onAddToCart(product) }
                            )
                        }
                    }
                }
            }
            
            // Panel derecho - Carrito
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header del carrito
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Carrito",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (cartLines.isNotEmpty()) {
                        TextButton(onClick = onClearCart) {
                            Text("Limpiar")
                        }
                    }
                }
                
                // Lista del carrito
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (cartLines.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "El carrito está vacío",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(cartLines, key = { it.productId }) { line ->
                                CartItemCard(
                                    productName = line.productName,
                                    quantity = line.quantity,
                                    price = Money.format(line.price),
                                    subtotal = Money.format(line.price * line.quantity),
                                    onIncreaseQuantity = { onIncreaseQuantity(line.productId) },
                                    onDecreaseQuantity = { onDecreaseQuantity(line.productId) }
                                )
                            }
                        }
                    }
                }
                
                // Resumen y controles de pago
                MultiPOSCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Selector de cliente
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cliente:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedClient ?: "No aplica",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(
                                    onClick = onScanClientQr,
                                    enabled = paymentMethod == "CREDITO"
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "Escanear QR cliente",
                                        tint = if (paymentMethod == "CREDITO") 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Selector de método de pago
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Método de pago:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilterChip(
                                selected = true,
                                onClick = { showPaymentOptions = true },
                                label = { Text(paymentMethod) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (paymentMethod) {
                                            "EFECTIVO" -> Icons.Default.Money
                                            "TARJETA" -> Icons.Default.CreditCard
                                            "TRANSFERENCIA" -> Icons.Default.AccountBalance
                                            "CREDITO" -> Icons.Default.Description
                                            else -> Icons.Default.Payment
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                        
                        Divider()
                        
                        // Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Money.format(total),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Botón de cobrar
                        MultiPOSButton(
                            text = "Cobrar",
                            onClick = onChargeClick,
                            enabled = cartLines.isNotEmpty() && !isLoading,
                            showLoading = isLoading
                        )
                    }
                }
            }
        }
        
        // Diálogo de selección de método de pago
        if (showPaymentOptions) {
            AlertDialog(
                onDismissRequest = { showPaymentOptions = false },
                title = { Text("Selecciona método de pago") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EFECTIVO", "TARJETA", "TRANSFERENCIA", "CREDITO").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = {
                                    onPaymentMethodSelected(method)
                                    showPaymentOptions = false
                                },
                                label = { Text(method) }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPaymentOptions = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun ProductListItem(
    product: Producto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                if (product.codigo.isNotBlank()) {
                    Text(
                        text = "Cód: ${product.codigo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(product.precioVenta),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (product.stock >= 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (product.stock > 10) 
                            MaterialTheme.colorScheme.success.copy(alpha = 0.1f)
                        else if (product.stock > 0)
                            MaterialTheme.colorScheme.warning.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Stock: ${product.stock}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (product.stock > 10)
                                MaterialTheme.colorScheme.success
                            else if (product.stock > 0)
                                MaterialTheme.colorScheme.warning
                            else
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun POSScreenPreview() {
    val dummyProducts = listOf(
        Producto(1, "Coca Cola 2L", "PROD001", 1500, 1000, 24, 5, "Bebidas", "", "7501055300075", "EAN_13", "EMP01"),
        Producto(2, "Pan de Molde", "PROD002", 2200, 1500, 12, 3, "Panadería", "", "7501055300082", "EAN_13", "EMP01"),
        Producto(3, "Leche Entera", "PROD003", 1100, 800, 4, 10, "Lácteos", "", "7501055300099", "EAN_13", "EMP01")
    )
    val dummyCart = listOf(
        CartLine(1, "Coca Cola 2L", 1500, 2),
        CartLine(2, "Pan de Molde", 2200, 1)
    )
    
    MultiPOSTheme {
        POSScreen(
            products = dummyProducts,
            cartLines = dummyCart,
            selectedClient = "Juan Cliente",
            paymentMethod = "EFECTIVO",
            total = 5200,
            searchQuery = "",
            isLoading = false,
            onSearchChange = {},
            onAddToCart = {},
            onIncreaseQuantity = {},
            onDecreaseQuantity = {},
            onRemoveFromCart = {},
            onClearCart = {},
            onPaymentMethodSelected = {},
            onClientSelected = {},
            onChargeClick = {},
            onScanProduct = {},
            onScanClientQr = {}
        )
    }
}

// Helper para formateo de dinero
object Money {
    fun format(cents: Long): String {
        val dollars = cents / 100
        val centsPart = cents % 100
        return "$${dollars}.${centsPart.toString().padStart(2, '0')}"
    }
}
