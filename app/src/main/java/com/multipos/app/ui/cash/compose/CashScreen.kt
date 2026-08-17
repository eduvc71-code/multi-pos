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
import androidx.compose.ui.unit.dp
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.util.Money
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashScreen(
    currentBalance: Long,
    movements: List<MovimientoCaja>,
    isLoading: Boolean,
    onOpenCashClick: () -> Unit,
    onCloseCashClick: () -> Unit,
    onRegisterMovementClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCashOpen = movements.isNotEmpty() || currentBalance > 0
    
    Scaffold(
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
                        IconButton(onClick = onRegisterMovementClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Registrar movimiento"
                            )
                        }
                        IconButton(onClick = onCloseCashClick) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Cerrar caja"
                            )
                        }
                    } else {
                        IconButton(onClick = onOpenCashClick) {
                            Icon(
                                imageVector = Icons.Default.Unlock,
                                contentDescription = "Abrir caja"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isCashOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    titleContentColor = if (isCashOpen) Color.White else MaterialTheme.colorScheme.surface,
                    actionIconContentColor = if (isCashOpen) Color.White else MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCashOpen) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isCashOpen) Icons.Default.AccountBalance else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isCashOpen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isCashOpen) "Saldo Actual" else "Caja Cerrada",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCashOpen) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Money.format(currentBalance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCashOpen) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    if (!isCashOpen) {
                        Spacer(modifier = Modifier.height(16.dp))
                        MultiPOSButton(
                            text = "Abrir Caja",
                            onClick = onOpenCashClick,
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }
            
            if (isCashOpen) {
                Text(
                    text = "Movimientos del Día",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                if (movements.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay movimientos registrados",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(movements, key = { it.id }) { movement ->
                            MovementCard(movement = movement)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovementCard(
    movement: MovimientoCaja,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (movement.tipo) {
                "INGRESO" -> MaterialTheme.colorScheme.secondaryContainer
                "EGRESO" -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
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
                    text = movement.descripcion,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDateTime(movement.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Text(
                text = if (movement.tipo == "INGRESO") "+${Money.format(movement.monto)}" else "-${Money.format(movement.monto)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (movement.tipo) {
                    "INGRESO" -> MaterialTheme.colorScheme.onSecondaryContainer
                    "EGRESO" -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
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
