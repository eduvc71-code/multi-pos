package com.multipos.app.ui.cash.compose

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
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.components.MultiPOSOutlineButton
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashScreen(
    expectedBalance: Long,
    ingresos: Long,
    egresos: Long,
    movements: List<MovimientoCaja>,
    isCashOpen: Boolean,
    isLoading: Boolean,
    onOpenCashClick: () -> Unit,
    onCloseCashClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Caja",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (isCashOpen) {
                        IconButton(onClick = onCloseCashClick) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Cerrar caja"
                            )
                        }
                    } else {
                        IconButton(onClick = onOpenCashClick) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Abrir caja"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isCashOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = if (isCashOpen) Color.White else MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = if (isCashOpen) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Card de resumen
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCashOpen) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isCashOpen) "Efectivo Esperado" else "Caja Cerrada",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = Money.format(expectedBalance),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        if (isCashOpen) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SummaryItem(
                                    label = "Ingresos",
                                    value = Money.format(ingresos),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                SummaryItem(
                                    label = "Egresos",
                                    value = Money.format(egresos),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onOpenCashClick) {
                                Text("Abrir Caja")
                            }
                        }
                    }
                }
            }
            
            if (isCashOpen) {
                // Acciones rápidas
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MultiPOSOutlineButton(
                            text = "+ Ingreso",
                            onClick = onAddIncomeClick,
                            modifier = Modifier.weight(1f)
                        )
                        MultiPOSOutlineButton(
                            text = "- Egreso",
                            onClick = onAddExpenseClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    Text(
                        text = "Movimientos recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (movements.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay movimientos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(movements, key = { it.id }) { movement ->
                        MovementCard(movement = movement)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CashScreenPreview() {
    val dummyMovements = listOf(
        MovimientoCaja(1, 1L, "EMP01", 1, "VENTA_EFECTIVO", 1500, null, null, null, "Venta #F-001", System.currentTimeMillis(), ""),
        MovimientoCaja(2, 1L, "EMP01", 1, "EGRESO_MANUAL", 500, null, null, null, "Pago de basura", System.currentTimeMillis() - 3600000, ""),
        MovimientoCaja(3, 1L, "EMP01", 1, "INGRESO_MANUAL", 2000, null, null, null, "Aporte inicial", System.currentTimeMillis() - 7200000, "")
    )
    MultiPOSTheme {
        CashScreen(
            expectedBalance = 3000,
            ingresos = 3500,
            egresos = 500,
            movements = dummyMovements,
            isCashOpen = true,
            isLoading = false,
            onOpenCashClick = {},
            onCloseCashClick = {},
            onAddIncomeClick = {},
            onAddExpenseClick = {}
        )
    }
}

@Composable
fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun MovementCard(movement: MovimientoCaja) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (movement.tipo) {
                "INGRESO_MANUAL", "VENTA_EFECTIVO" -> Icons.Default.AddCircle
                "EGRESO_MANUAL" -> Icons.Default.RemoveCircle
                else -> Icons.Default.Info
            }
            val tint = when (movement.tipo) {
                "INGRESO_MANUAL", "VENTA_EFECTIVO" -> MaterialTheme.colorScheme.primary
                "EGRESO_MANUAL" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = movement.descripcion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(text = formatDateTime(movement.fecha), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = Money.format(movement.monto),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

private fun formatDateTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        timestamp.toString()
    }
}
