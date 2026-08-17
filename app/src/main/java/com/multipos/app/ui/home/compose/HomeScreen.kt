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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multipos.app.ui.components.MultiPOSCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    activeCompanyName: String,
    userName: String,
    userRole: String,
    companyColor: Color,
    onLogoutClick: () -> Unit,
    onCompanyClick: () -> Unit,
    onMenuItemClick: (String) -> Unit,
    selectedMenu: String,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Card
                    MultiPOSCard(elevation = 2.dp) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ESPACIO DE TRABAJO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                TextButton(onClick = onLogoutClick) {
                                    Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                            
                            Button(
                                onClick = onCompanyClick,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = companyColor)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = "Empresa: $activeCompanyName", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "$userName · $userRole", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Menu Horizontal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuItem(Icons.Default.Analytics, "Inicio", selectedMenu == "DASHBOARD", companyColor) { onMenuItemClick("DASHBOARD") }
                        MenuItem(Icons.Default.ShoppingCart, "Ventas", selectedMenu == "POS", companyColor) { onMenuItemClick("POS") }
                        MenuItem(Icons.Default.Inventory, "Inventario", selectedMenu == "INVENTORY", companyColor) { onMenuItemClick("INVENTORY") }
                        MenuItem(Icons.Default.History, "Historial", selectedMenu == "HISTORY", companyColor) { onMenuItemClick("HISTORY") }
                        MenuItem(Icons.Default.People, "Clientes", selectedMenu == "CLIENTS", companyColor) { onMenuItemClick("CLIENTS") }
                        MenuItem(Icons.Default.Badge, "Equipo", selectedMenu == "EMPLOYEES", companyColor) { onMenuItemClick("EMPLOYEES") }
                        MenuItem(Icons.Default.Payments, "Caja", selectedMenu == "CASH", companyColor) { onMenuItemClick("CASH") }
                        MenuItem(Icons.Default.BarChart, "Reportes", selectedMenu == "REPORTS", companyColor) { onMenuItemClick("REPORTS") }
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
        modifier = Modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 14.dp)
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
