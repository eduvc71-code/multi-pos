package com.multipos.app.ui.home.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.multipos.app.ui.theme.MultiPOSTheme

data class HomeNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val permission: String,
    val color: Color? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    userRole: String,
    companyName: String,
    companyColor: Color,
    onNavigateToDashboard: () -> Unit,
    onNavigateToPOS: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToCash: () -> Unit,
    onNavigateToReports: () -> Unit,
    onCompanyClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = remember {
        listOf(
            HomeNavItem("dashboard", "Dashboard", Icons.Default.Dashboard, "VIEW_DASHBOARD"),
            HomeNavItem("pos", "Ventas", Icons.Default.ShoppingCart, "SELL"),
            HomeNavItem("inventory", "Inventario", Icons.Default.Inventory, "MANAGE_INVENTORY"),
            HomeNavItem("history", "Historial", Icons.Default.History, "VIEW_HISTORY"),
            HomeNavItem("clients", "Clientes", Icons.Default.People, "MANAGE_CLIENT_CREDIT"),
            HomeNavItem("employees", "Empleados", Icons.Default.Badge, "MANAGE_EMPLOYEES"),
            HomeNavItem("cash", "Caja", Icons.Default.AccountBalance, "MANAGE_CASH"),
            HomeNavItem("reports", "Reportes", Icons.Default.Assessment, "VIEW_REPORTS")
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MultiPOS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$userName - $userRole",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCompanyClick) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Cambiar empresa"
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = companyColor,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Card de empresa activa
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = companyColor.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Empresa Activa",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = companyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = companyColor
                        )
                    }
                    IconButton(onClick = onCompanyClick) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Cambiar empresa",
                            tint = companyColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Menú Principal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grid de navegación
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(navItems) { item ->
                    NavGridItem(
                        navItem = item,
                        onClick = {
                            when (item.id) {
                                "dashboard" -> onNavigateToDashboard()
                                "pos" -> onNavigateToPOS()
                                "inventory" -> onNavigateToInventory()
                                "history" -> onNavigateToHistory()
                                "clients" -> onNavigateToClients()
                                "employees" -> onNavigateToEmployees()
                                "cash" -> onNavigateToCash()
                                "reports" -> onNavigateToReports()
                            }
                        },
                        baseColor = companyColor
                    )
                }
            }
        }
    }
}

@Composable
fun NavGridItem(
    navItem: HomeNavItem,
    onClick: () -> Unit,
    baseColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = baseColor.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = navItem.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = baseColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = navItem.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = baseColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MultiPOSTheme {
        HomeScreen(
            userName = "Juan Pérez",
            userRole = "Administrador",
            companyName = "Mi Tienda MultiPOS",
            companyColor = Color(0xFF1976D2),
            onNavigateToDashboard = {},
            onNavigateToPOS = {},
            onNavigateToInventory = {},
            onNavigateToHistory = {},
            onNavigateToClients = {},
            onNavigateToEmployees = {},
            onNavigateToCash = {},
            onNavigateToReports = {},
            onCompanyClick = {},
            onLogout = {}
        )
    }
}
