package com.multipos.app.ui.pos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.multipos.app.R
import com.multipos.app.data.entities.Producto
import com.multipos.app.data.models.CartLine
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money

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
    warning: String? = null,
    onSearchChange: (String) -> Unit,
    onAddToCart: (Producto, Int) -> Unit,
    onIncreaseQuantity: (Int) -> Unit,
    onDecreaseQuantity: (Int) -> Unit,
    onRemoveFromCart: (Int) -> Unit,
    onClearCart: () -> Unit,
    onPaymentMethodSelected: (String) -> Unit,
    onClientSelected: (String) -> Unit,
    onChargeClick: () -> Unit,
    onScanProduct: () -> Unit,
    onScanClientQr: () -> Unit,
    onClearWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchResults by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var lineToEdit by remember { mutableStateOf<Int?>(null) }
    var productToQtyDialog by remember { mutableStateOf<Producto?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(warning) {
        if (warning != null) {
            snackbarHostState.showSnackbar(warning)
            onClearWarning()
        }
    }

    Scaffold(
        modifier = modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Punto de Venta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    IconButton(onClick = onScanProduct) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Buscador Rectangular
            Box {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        onSearchChange(it)
                        showSearchResults = it.isNotEmpty()
                    },
                    placeholder = { Text("Buscar producto (ej: PA)...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                if (showSearchResults && products.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp)
                            .heightIn(max = 250.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        LazyColumn {
                            items(products) { product ->
                                ProductSearchRow(product) {
                                    productToQtyDialog = product
                                    showSearchResults = false
                                    onSearchChange("")
                                }
                                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }

            // --- TABLA TIPO EXCEL / GRID INDUSTRIAL ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    .background(Color.White)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell(stringResource(R.string.pos_quantity_header), Modifier.weight(0.15f), isHeader = true)
                        TableCell(stringResource(R.string.pos_product_header), Modifier.weight(0.50f), isHeader = true)
                        TableCell("P/U", Modifier.weight(0.17f), isHeader = true, alignEnd = true)
                        TableCell(stringResource(R.string.pos_total_header), Modifier.weight(0.18f), isHeader = true, alignEnd = true)
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(cartLines, key = { it.productId }) { line ->
                            CartRowExcel(
                                line = line,
                                isEditing = lineToEdit == line.productId,
                                onRowClick = { lineToEdit = if (lineToEdit == line.productId) null else line.productId },
                                onIncrease = { onIncreaseQuantity(line.productId) },
                                onDecrease = { onDecreaseQuantity(line.productId) },
                                onRemove = { onRemoveFromCart(line.productId) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(text = "${stringResource(R.string.pos_client_label)} ${selectedClient ?: stringResource(R.string.pos_client_general)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Text(text = "ITEMS: ${cartLines.sumOf { it.quantity }}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "TOTAL BS.", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Text(
                        text = Money.formatPlain(total),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            MultiPOSButton(
                text = "Finalizar Cobro",
                onClick = { if (cartLines.isNotEmpty()) showPaymentDialog = true },
                enabled = cartLines.isNotEmpty() && !isLoading,
                showLoading = isLoading
            )
        }

        if (showPaymentDialog) {
            PaymentMethodDialog(
                total = total,
                onDismiss = { showPaymentDialog = false },
                onConfirm = { method ->
                    onPaymentMethodSelected(method)
                    onChargeClick()
                    showPaymentDialog = false
                }
            )
        }

        // Pantalla de Cantidad
        productToQtyDialog?.let { product ->
            QuantitySelectionDialog(
                product = product,
                onDismiss = { productToQtyDialog = null },
                onConfirm = { qty ->
                    onAddToCart(product, qty)
                    productToQtyDialog = null
                }
            )
        }
    }
}



@Composable
fun QuantitySelectionDialog(
    product: Producto,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var qty by remember { mutableStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(stringResource(R.string.pos_select_quantity_title), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(product.nombre.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    IconButton(onClick = { if (qty > 1) qty-- }, modifier = Modifier.size(54.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)) {
                        Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    Text(text = qty.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { qty++ }, modifier = Modifier.size(54.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }

                if (qty > product.stock) {
                    Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text("Advertencia: Supera stock (${product.stock})", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text(stringResource(R.string.pos_cancel_button), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { onConfirm(qty) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text(stringResource(R.string.pos_add_button), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(text: String, modifier: Modifier, isHeader: Boolean = false, alignEnd: Boolean = false) {
    Text(
        text = text,
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant).padding(6.dp),
        style = if (isHeader) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        fontWeight = if (isHeader) FontWeight.Black else FontWeight.Bold,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun CartRowExcel(line: CartLine, isEditing: Boolean, onRowClick: () -> Unit, onIncrease: () -> Unit, onDecrease: () -> Unit, onRemove: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().clickable { onRowClick() }) {
            TableCell(line.quantity.toString(), Modifier.weight(0.15f))
            TableCell(line.productName.uppercase(), Modifier.weight(0.50f))
            TableCell(Money.formatPlain(line.price), Modifier.weight(0.17f), alignEnd = true)
            TableCell(Money.formatPlain(line.price * line.quantity), Modifier.weight(0.18f), alignEnd = true)
        }
        if (isEditing) {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)).border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease) { Icon(Icons.Default.RemoveCircle, null, tint = MaterialTheme.colorScheme.primary) }
                Text(text = "CANT: ${line.quantity}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = onIncrease) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun ProductSearchRow(product: Producto, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = product.nombre.uppercase(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = Money.formatPlain(product.precioVenta), style = MaterialTheme.typography.bodyMedium, color = Color.Blue, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PaymentMethodDialog(total: Long, onDismiss: () -> Unit, onConfirm: (method: String) -> Unit) {
    var amountPaidText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("EFECTIVO") }
    val amountPaid = amountPaidText.toDoubleOrNull() ?: 0.0
    val changeMinor = ((amountPaid * 100).toLong() - total).coerceAtLeast(0L)
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().heightIn(min = 500.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(20.dp)) { Text("PAGO Y COBRO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp) }
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.width(100.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).fillMaxHeight()) {
                        PaymentSidebarItem(stringResource(R.string.pos_payment_method_cash), Icons.Default.Payments, selectedMethod == "EFECTIVO") { selectedMethod = "EFECTIVO" }
                        PaymentSidebarItem(stringResource(R.string.pos_payment_method_qr), Icons.Default.QrCode, selectedMethod == "QR") { selectedMethod = "QR" }
                        PaymentSidebarItem(stringResource(R.string.pos_payment_method_card), Icons.Default.CreditCard, selectedMethod == "TARJETA") { selectedMethod = "TARJETA" }
                        PaymentSidebarItem(stringResource(R.string.pos_payment_method_credit), Icons.Default.Description, selectedMethod == "CREDITO") { selectedMethod = "CREDITO" }
                    }
                    Column(modifier = Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("TOTAL A PAGAR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.formatPlain(total), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        if (selectedMethod == "EFECTIVO") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text("EFECTIVO RECIBIDO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value = amountPaidText, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountPaidText = it }, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), placeholder = { Text("0.00", color = Color.LightGray) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                            Text("CAMBIO (VUELTO)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                            Text(Money.formatPlain(changeMinor), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color.Red)
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(imageVector = when(selectedMethod) { "QR" -> Icons.Default.QrCode; "TARJETA" -> Icons.Default.CreditCard; else -> Icons.Default.Description }, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)); Text(stringResource(R.string.pos_payment_via, selectedMethod), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { onConfirm(selectedMethod) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("FINALIZAR VENTA", fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
                            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(10.dp)); Text(stringResource(R.string.pos_payment_return), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentSidebarItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).background(if (isSelected) Color.White else Color.Transparent).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium, textAlign = TextAlign.Center, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun POSScreenPreview() {
    val dummyProducts = listOf(Producto(1, "Coca Cola 2L", "001", 1500, 1000, 24, 5, "Bebidas", "", "123", "EAN", "EMP"))
    val dummyCart = listOf(CartLine(dummyProducts[0], 2))
    MultiPOSTheme {
        POSScreen(dummyProducts, dummyCart, "JUAN PEREZ", "EFECTIVO", 3000, "", false, null, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
