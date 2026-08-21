package com.multipos.app.ui.cash.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.multipos.app.R
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
    modifier: Modifier = Modifier,
    onOpenCashClick: () -> Unit,
    onCloseCashClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.cash_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    if (isCashOpen) {
                        IconButton(onClick = onCloseCashClick) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        }
                    } else {
                        IconButton(onClick = onOpenCashClick) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                        }
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Card Caja
            MultiPOSCard(elevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isCashOpen) "EFECTIVO ESPERADO" else "CAJA CERRADA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Money.format(expectedBalance),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (isCashOpen) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SummaryItemPremium(label = stringResource(R.string.cash_summary_income), value = Money.format(ingresos), color = MaterialTheme.colorScheme.primary)
                            SummaryItemPremium(label = stringResource(R.string.cash_summary_expense), value = Money.format(egresos), color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        MultiPOSButton(text = stringResource(R.string.cash_open), onClick = onOpenCashClick, modifier = Modifier.width(200.dp))
                    }
                }
            }
            
            if (isCashOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MultiPOSOutlineButton(text = stringResource(R.string.cash_income), onClick = onAddIncomeClick, modifier = Modifier.weight(1f))
                    MultiPOSOutlineButton(text = stringResource(R.string.cash_expense), onClick = onAddExpenseClick, modifier = Modifier.weight(1f))
                }
                
                Text(
                    text = "Movimientos del día",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                
                MultiPOSCard(modifier = Modifier.weight(1f), elevation = 1.dp) {
                    if (movements.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.cash_no_movements), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            items(movements, key = { it.id }) { movement ->
                                MovementListItemPremium(movement = movement)
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryItemPremium(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun MovementListItemPremium(movement: MovimientoCaja) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isIncome = movement.tipo.contains("INGRESO") || movement.tipo.contains("VENTA")
        val color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = movement.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(movement.fecha)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = (if (isIncome) "+ " else "- ") + Money.format(movement.monto),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CashScreenPreview() {
    val dummyMovements = listOf(
        MovimientoCaja(1, 1L, "EMP01", 1, "VENTA_EFECTIVO", 1500, null, null, null, "Venta #F-001", System.currentTimeMillis(), ""),
        MovimientoCaja(2, 1L, "EMP01", 1, "EGRESO_MANUAL", 500, null, null, null, "Pago a proveedor", System.currentTimeMillis() - 3600000, "")
    )
    MultiPOSTheme {
        CashScreen(
            expectedBalance = 3000,
            ingresos = 3500,
            egresos = 500,
            movements = dummyMovements,
            isCashOpen = true,
            modifier = Modifier,
            onOpenCashClick = {},
            onCloseCashClick = {},
            onAddIncomeClick = {},
            onAddExpenseClick = {}
        )
    }
}
