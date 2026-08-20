package com.multipos.app.ui.dashboard.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multipos.app.R
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.ui.theme.success
import com.multipos.app.util.Money

@Composable
fun DashboardScreen(
    companyName: String,
    totalSalesToday: Long,
    totalProducts: Int,
    lowStockCount: Int,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isSmallScreen = this.maxHeight < 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 10.dp else 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = companyName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Control Center",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Salir",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 100.dp else 120.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MultiPOSCard(
                    modifier = Modifier.weight(1.4f).fillMaxHeight(),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.dashboard_sales_today), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(Money.formatPlain(totalSalesToday), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.success.copy(alpha = 0.15f)
                        ) {
                            Text("+18%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.success, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }

                MultiPOSCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.dashboard_alerts_title), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text(lowStockCount.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            MultiPOSCard(modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 80.dp else 100.dp), elevation = 1.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("INGRESOS POR MEDIO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)) {
                        Box(modifier = Modifier.weight(0.7f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                        Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(Color(0xFF00ACC1)))
                        Box(modifier = Modifier.weight(0.1f).fillMaxHeight().background(Color(0xFFFFA000)))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("EFECTIVO 70%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color.Gray)
                        Text("OTROS 30%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color.Gray)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                QuickActionButton(Modifier.weight(1f).height(if (isSmallScreen) 70.dp else 80.dp), stringResource(R.string.dashboard_quick_inventory), Icons.Default.Inventory, Color(0xFF5C6BC0))
                QuickActionButton(Modifier.weight(1f).height(if (isSmallScreen) 70.dp else 80.dp), stringResource(R.string.dashboard_quick_clients), Icons.Default.People, Color(0xFF66BB6A))
            }
        }
    }
}

@Composable
fun QuickActionButton(modifier: Modifier, label: String, icon: ImageVector, color: Color) {
    MultiPOSCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), fontWeight = FontWeight.Black, color = Color.Gray)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() {
    MultiPOSTheme {
        DashboardScreen(
            companyName = "Demo MultiPOS",
            totalSalesToday = 1250500,
            totalProducts = 45,
            lowStockCount = 3,
            onLogoutClick = {}
        )
    }
}
