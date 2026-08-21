package com.multipos.app.ui.history.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.multipos.app.R
import com.multipos.app.data.entities.*
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    sale: Venta?,
    details: List<DetalleVenta>,
    refunds: List<Devolucion>,
    vendedorName: String,
    clienteName: String,
    onBackClick: () -> Unit,
    onAnnulClick: () -> Unit,
    onRefundClick: () -> Unit,
    canManageReturns: Boolean,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.sale_detail_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.sale_detail_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (sale == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card de información principal
            MultiPOSCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.history_folio_prefix, sale.id.toString()),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (sale.estado) {
                                Venta.ESTADO_COMPLETADA -> MaterialTheme.colorScheme.secondaryContainer
                                Venta.ESTADO_ANULADA -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = sale.estado,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = when (sale.estado) {
                                    Venta.ESTADO_COMPLETADA -> MaterialTheme.colorScheme.onSecondaryContainer
                                    Venta.ESTADO_ANULADA -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InfoRow(stringResource(R.string.sale_detail_date_label), dateFormat.format(Date(sale.fecha)))
                    InfoRow(stringResource(R.string.sale_detail_seller_label), vendedorName)
                    InfoRow(stringResource(R.string.sale_detail_customer_label), clienteName)
                    InfoRow(stringResource(R.string.sale_detail_payment_method_label), sale.tipoPago)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    InfoRow(stringResource(R.string.sale_detail_subtotal_label), Money.format(sale.subtotal))
                    if (sale.descuento > 0) {
                        InfoRow(stringResource(R.string.sale_detail_discount_label), "-${Money.format(sale.descuento)}", Color.Red)
                    }
                    if (sale.impuesto > 0) {
                        InfoRow(stringResource(R.string.sale_detail_tax_label), Money.format(sale.impuesto))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InfoRow(
                        stringResource(R.string.sale_detail_total_label),
                        Money.format(sale.total),
                        MaterialTheme.colorScheme.primary,
                        FontWeight.Bold
                    )
                }
            }
            
            // Sección de Devoluciones (si existen)
            if (refunds.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.sale_detail_refunds_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                refunds.forEach { refund ->
                    MultiPOSCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = dateFormat.format(Date(refund.fecha)), style = MaterialTheme.typography.bodySmall)
                                Text(text = Money.format(refund.monto), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Text(text = stringResource(R.string.sale_detail_refund_reason_prefix, refund.motivo), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            // Lista de items
            Text(
                text = stringResource(R.string.sale_detail_products_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            details.forEach { detail ->
                SaleDetailItem(detail = detail)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Botones de acción
            if (canManageReturns && sale.estado == Venta.ESTADO_COMPLETADA) {
                if (refunds.isEmpty()) {
                    Button(
                        onClick = onAnnulClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sale_detail_annul_complete))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onRefundClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.sale_detail_partial_return))
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueFontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueFontWeight,
            color = valueColor
        )
    }
}

@Composable
fun SaleDetailItem(
    detail: DetalleVenta,
    modifier: Modifier = Modifier
) {
    MultiPOSCard(
        modifier = modifier.fillMaxWidth(),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detail.nombreProductoSnapshot.ifBlank { stringResource(R.string.sale_detail_product_default, detail.idProducto) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(R.string.sale_detail_quantity_format, detail.cantidad),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = stringResource(R.string.sale_detail_unit_price_prefix) + Money.format(detail.precioUnitario),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = Money.format(detail.subtotal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SaleDetailScreenPreview() {
    val sampleSale = Venta(
        id = 1,
        total = 50000,
        subtotal = 45000,
        impuesto = 5000,
        tipoPago = "EFECTIVO",
        fecha = System.currentTimeMillis(),
        idUsuario = 1
    )
    val sampleDetails = listOf(
        DetalleVenta(id = 1, idVenta = 1, idProducto = 1, nombreProductoSnapshot = "Producto 1", cantidad = 2, precioUnitario = 20000, subtotal = 40000),
        DetalleVenta(id = 2, idVenta = 1, idProducto = 2, nombreProductoSnapshot = "Producto 2", cantidad = 1, precioUnitario = 5000, subtotal = 5000)
    )
    
    MultiPOSTheme {
        SaleDetailScreen(
            sale = sampleSale,
            details = sampleDetails,
            refunds = emptyList(),
            vendedorName = "Admin",
            clienteName = "Cliente General",
            onBackClick = {},
            onAnnulClick = {},
            onRefundClick = {},
            canManageReturns = true
        )
    }
}
