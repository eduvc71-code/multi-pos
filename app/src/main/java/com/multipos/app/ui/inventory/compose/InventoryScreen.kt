package com.multipos.app.ui.inventory.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multipos.app.data.entities.Producto
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    products: List<Producto>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onAddProductClick: () -> Unit,
    onEditProductClick: (Producto) -> Unit,
    onDeleteProductClick: (Producto) -> Unit,
    onMovementsClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inventario",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    IconButton(onClick = onScanClick) { Icon(Icons.Default.QrCodeScanner, null) }
                    IconButton(onClick = onMovementsClick) { Icon(Icons.Default.History, null) }
                    IconButton(onClick = onAddProductClick) { Icon(Icons.Default.Add, null) }
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
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Buscar producto...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )
            
            // TABLA INDUSTRIAL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
                    .background(Color.White)
            ) {
                Column {
                    // Header Tabla con Iconos y texto separado (Prompt User)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE0E0E0))
                            .border(androidx.compose.foundation.BorderStroke(0.5.dp, Color.Black)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCellHeader("PRODUCTO", Icons.Default.Inventory2, Modifier.weight(0.45f))
                        TableCellHeader("COSTO\n(UNIT)", Icons.Default.AttachMoney, Modifier.weight(0.18f))
                        TableCellHeader("PRECIO\n(VENTA)", Icons.Default.Sell, Modifier.weight(0.18f))
                        TableCellHeader("STOCK\n(ACT)", Icons.Default.Inventory, Modifier.weight(0.19f))
                    }

                    if (products.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay resultados", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(products, key = { it.id }) { product ->
                                ProductRowExcel(
                                    product = product,
                                    onEdit = { onEditProductClick(product) },
                                    onDelete = { onDeleteProductClick(product) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableCellHeader(text: String, icon: ImageVector, modifier: Modifier) {
    Column(
        modifier = modifier
            .border(0.5.dp, Color.Black)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color.DarkGray)
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 10.sp
        )
    }
}

@Composable
fun TableCell(text: String, modifier: Modifier, alignEnd: Boolean = false, color: Color = Color.Black) {
    Text(
        text = text,
        modifier = modifier
            .border(0.5.dp, Color.Black)
            .padding(8.dp),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        fontWeight = FontWeight.Bold,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = color
    )
}

@Composable
fun ProductRowExcel(product: Producto, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showActions by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.clickable { showActions = !showActions }) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TableCell(product.nombre.uppercase(), Modifier.weight(0.45f))
            // Solo números (formatPlain)
            TableCell(Money.formatPlain(product.costoUnitario), Modifier.weight(0.18f), alignEnd = true)
            TableCell(Money.formatPlain(product.precioVenta), Modifier.weight(0.18f), alignEnd = true, color = MaterialTheme.colorScheme.primary)
            TableCell(product.stock.toString(), Modifier.weight(0.19f), alignEnd = true)
        }
        
        if (showActions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.Black)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun InventoryScreenPreview() {
    val dummyProducts = listOf(
        Producto(1, "Arroz 1kg", "PROD001", 1200, 800, 50, 5, "Abarrotes", "", "123", "EAN", "EMP")
    )
    MultiPOSTheme {
        InventoryScreen(dummyProducts, "", false, {}, {}, {}, {}, {}, {})
    }
}
