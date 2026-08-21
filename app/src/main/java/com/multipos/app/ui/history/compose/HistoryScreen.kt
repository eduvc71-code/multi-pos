package com.multipos.app.ui.history.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multipos.app.R
import com.multipos.app.data.entities.Venta
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.components.MultiPOSSearchField
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    sales: List<Venta>,
    searchQuery: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onSearchChange: (String) -> Unit,
    totalToday: Long,
    onSaleClick: (Venta) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Executive
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // Search
        MultiPOSSearchField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = stringResource(R.string.history_search_placeholder)
        )
        
        // Summary Card
        MultiPOSCard(elevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_total_today),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = Money.format(totalToday),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // List Container
        MultiPOSCard(modifier = Modifier.weight(1f), elevation = 1.dp) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (sales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.history_no_transactions), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(sales, key = { it.id }) { sale ->
                        SaleListItemPremium(sale = sale, onClick = { onSaleClick(sale) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun SaleListItemPremium(sale: Venta, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.history_folio_prefix, sale.id.toString()),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sale.tipoPago,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${dateFormat.format(Date(sale.fecha))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Money.format(sale.total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (sale.estado == Venta.ESTADO_ANULADA) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (sale.estado == Venta.ESTADO_ANULADA) {
                Text(
                    text = stringResource(R.string.sale_detail_annulled_badge),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HistoryScreenPreview() {
    val sampleSales = listOf(
        Venta(id = 1, total = 50000, tipoPago = "EFECTIVO", fecha = System.currentTimeMillis(), idUsuario = 1),
        Venta(id = 2, total = 25000, tipoPago = "TARJETA", fecha = System.currentTimeMillis() - 3600000, idUsuario = 1),
        Venta(id = 3, total = 10000, tipoPago = "EFECTIVO", fecha = System.currentTimeMillis() - 7200000, idUsuario = 1, estado = Venta.ESTADO_ANULADA)
    )
    
    MultiPOSTheme {
        HistoryScreen(
            sales = sampleSales,
            searchQuery = "",
            isLoading = false,
            onSearchChange = {},
            totalToday = 75000,
            onSaleClick = {}
        )
    }
}
