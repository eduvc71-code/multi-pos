package com.multipos.app.ui.clients.compose

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.multipos.app.data.entities.Cliente
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.components.MultiPOSSearchField
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.ui.theme.success
import com.multipos.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    clients: List<Cliente>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onAddClientClick: () -> Unit,
    onEditClientClick: (Cliente) -> Unit,
    onViewStatementClick: (Cliente) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Clientes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    IconButton(onClick = onAddClientClick) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
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
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MultiPOSSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Buscar cliente..."
            )
            
            // Stats Executive
            MultiPOSCard(elevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(label = "TOTAL", value = clients.size.toString(), icon = Icons.Default.People)
                    StatItem(label = "CON DEUDA", value = clients.count { it.deuda > 0 }.toString(), icon = Icons.Default.Warning)
                }
            }
            
            // List Container
            MultiPOSCard(modifier = Modifier.weight(1f), elevation = 1.dp) {
                if (clients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay clientes registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        items(clients, key = { it.id }) { client ->
                            ClientListItemPremium(
                                client = client,
                                onEditClick = { onEditClientClick(client) },
                                onViewStatementClick = { onViewStatementClick(client) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ClientListItemPremium(
    client: Cliente,
    onEditClick: () -> Unit,
    onViewStatementClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = client.nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Deuda: ${Money.format(client.deuda)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = if (client.deuda > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.success
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onViewStatementClick) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ClientsScreenPreview() {
    val dummyClients = listOf(
        Cliente(1, "María García", "DNI 123456", "555-0101", "Calle 123", 50000, 10000, true, "ACTIVO", System.currentTimeMillis(), null, null, true, "EMP01"),
        Cliente(2, "Tienda Don Pepe", "NIT 100200", "555-0103", "Bario Lindo", 100000, 65000, true, "ACTIVO", System.currentTimeMillis(), null, null, true, "EMP01")
    )
    MultiPOSTheme {
        ClientsScreen(
            clients = dummyClients,
            searchQuery = "",
            isLoading = false,
            onSearchChange = {},
            onAddClientClick = {},
            onEditClientClick = {},
            onViewStatementClick = {}
        )
    }
}
