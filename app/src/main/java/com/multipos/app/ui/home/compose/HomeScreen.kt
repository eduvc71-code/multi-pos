package com.multipos.app.ui.home.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.multipos.app.R
import com.multipos.app.ui.theme.MultiPOSTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    companyColor: Color,
    onLogoutClick: () -> Unit,
    onMenuItemClick: (String) -> Unit,
    selectedMenu: String,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 4.dp) // Reducido vertical
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onLogoutClick,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(48.dp) // Aumentado a 48dp para touch target estándar
                        ) {
                            Text(stringResource(R.string.home_logout), color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Menu Horizontal con botones más grandes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuItem(Icons.Default.Analytics, stringResource(R.string.menu_dashboard_item), selectedMenu == "DASHBOARD", companyColor) { onMenuItemClick("DASHBOARD") }
                        MenuItem(Icons.Default.ShoppingCart, stringResource(R.string.menu_sales_item), selectedMenu == "POS", companyColor) { onMenuItemClick("POS") }
                        MenuItem(Icons.Default.Inventory, stringResource(R.string.menu_inventory), selectedMenu == "INVENTORY", companyColor) { onMenuItemClick("INVENTORY") }
                        MenuItem(Icons.Default.History, stringResource(R.string.menu_history), selectedMenu == "HISTORY", companyColor) { onMenuItemClick("HISTORY") }
                        MenuItem(Icons.Default.People, stringResource(R.string.menu_clients), selectedMenu == "CLIENTS", companyColor) { onMenuItemClick("CLIENTS") }
                        MenuItem(Icons.Default.Payments, stringResource(R.string.menu_cash), selectedMenu == "CASH", companyColor) { onMenuItemClick("CASH") }
                        MenuItem(Icons.Default.BarChart, stringResource(R.string.menu_reports), selectedMenu == "REPORTS", companyColor) { onMenuItemClick("REPORTS") }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            content()
        }
    }
}

@Composable
fun MenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(48.dp), // Target táctil de 48dp
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) Color.White else accentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MultiPOSTheme {
        HomeScreen(
            userName = "Admin",
            companyColor = MaterialTheme.colorScheme.primary,
            onLogoutClick = {},
            onMenuItemClick = {},
            selectedMenu = "POS",
            content = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Contenido de Prueba")
                }
            }
        )
    }
}
