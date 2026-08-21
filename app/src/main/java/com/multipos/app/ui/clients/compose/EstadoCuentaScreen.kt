package com.multipos.app.ui.clients.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.multipos.app.R
import com.multipos.app.data.entities.Cliente
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.ui.theme.success
import com.multipos.app.util.Money

data class MovimientoRow(
    val fecha: String,
    val tipo: String,
    val importe: String,
    val saldoPosterior: String,
    val usuario: String,
    val isNegativo: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoCuentaScreen(
    cliente: Cliente?,
    movimientos: List<MovimientoRow>,
    desde: String,
    hasta: String,
    onDesdeChange: (String) -> Unit,
    onHastaChange: (String) -> Unit,
    onFiltrarClick: () -> Unit,
    onRegistrarAbonoClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onBackClick: () -> Unit,
    canRegisterAbono: Boolean,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.estado_cuenta_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Resumen de cliente
            item {
                cliente?.let {
                    MultiPOSCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = it.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(text = "Límite: ${Money.format(it.limiteCredito)}", style = MaterialTheme.typography.bodyMedium)
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(text = "Deuda", style = MaterialTheme.typography.labelSmall)
                                    Text(text = Money.format(it.creditoActual), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Disponible", style = MaterialTheme.typography.labelSmall)
                                    Text(text = Money.format(it.creditoDisponible), color = MaterialTheme.colorScheme.success, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            
            // Filtros
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = desde,
                                onValueChange = onDesdeChange,
                                label = { Text(stringResource(R.string.estado_cuenta_from)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = hasta,
                                onValueChange = onHastaChange,
                                label = { Text(stringResource(R.string.estado_cuenta_until)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Button(onClick = onFiltrarClick, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.estado_cuenta_filter))
                        }
                    }
                }
            }
            
            // Acciones de Abono y Exportación
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canRegisterAbono) {
                        Button(
                            onClick = onRegistrarAbonoClick,
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.success)
                        ) {
                            Text(stringResource(R.string.estado_cuenta_pay), color = Color.White)
                        }
                    }
                    
                    OutlinedIconButton(onClick = onExportCsvClick) {
                        Icon(Default.TableChart, contentDescription = "CSV")
                    }
                    OutlinedIconButton(onClick = onExportPdfClick) {
                        Icon(Default.PictureAsPdf, contentDescription = "PDF")
                    }
                }
            }
            
            // Lista de Movimientos
            if (movimientos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.estado_cuenta_no_movements), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(movimientos) { mov ->
                    MovimientoItem(mov)
                }
            }
        }
    }
}

@Composable
fun MovimientoItem(mov: MovimientoRow) {
    MultiPOSCard(modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = mov.tipo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(text = mov.fecha, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = mov.usuario, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = mov.importe,
                    fontWeight = FontWeight.Black,
                    color = if (mov.isNegativo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.success
                )
                Text(text = mov.saldoPosterior, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EstadoCuentaScreenPreview() {
    val sampleCliente = Cliente(
        id = 1,
        nombre = "Juan Pérez",
        documento = "12345678-9",
        limiteCredito = 500000,
        creditoActual = 150000,
        creditoHabilitado = true,
        estadoCredito = Cliente.ESTADO_ACTIVO
    )
    
    val sampleMovimientos = listOf(
        MovimientoRow("2026-08-01 10:00", "Venta #001", "$ 100.00", "$ 100.00", "Admin", true),
        MovimientoRow("2026-08-02 14:30", "Abono #001", "$ 50.00", "$ 50.00", "Caja", false),
        MovimientoRow("2026-08-05 09:15", "Venta #042", "$ 250.00", "$ 300.00", "Vendedor A", true)
    )

    MultiPOSTheme {
        EstadoCuentaScreen(
            cliente = sampleCliente,
            movimientos = sampleMovimientos,
            desde = "01/08/2026",
            hasta = "20/08/2026",
            onDesdeChange = {},
            onHastaChange = {},
            onFiltrarClick = {},
            onRegistrarAbonoClick = {},
            onExportCsvClick = {},
            onExportPdfClick = {},
            onBackClick = {},
            canRegisterAbono = true
        )
    }
}
