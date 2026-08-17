package com.multipos.app.ui.employees.compose

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
import com.multipos.app.data.entities.Usuario
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.components.MultiPOSSearchField
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.ui.theme.warningContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(
    employees: List<Usuario>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onAddEmployeeClick: () -> Unit,
    onEditEmployeeClick: (Usuario) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Empleados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onAddEmployeeClick) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Agregar empleado"
                        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            item {
                MultiPOSSearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Buscar empleado por nombre o usuario..."
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            label = "Total Empleados",
                            value = employees.size.toString(),
                            icon = Icons.Default.Badge
                        )
                        StatItem(
                            label = "Activos",
                            value = employees.count { !it.bloqueado }.toString(),
                            icon = Icons.Default.CheckCircle
                        )
                        StatItem(
                            label = "Bloqueados",
                            value = employees.count { it.bloqueadoHasta != null && it.bloqueadoHasta!! > System.currentTimeMillis() }.toString(),
                            icon = Icons.Default.Block
                        )
                    }
                }
            }
            
            if (employees.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay empleados registrados",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MultiPOSButton(
                                text = "Agregar primer empleado",
                                onClick = onAddEmployeeClick,
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                }
            } else {
                items(employees, key = { it.id }) { employee ->
                    EmployeeCard(
                        employee = employee,
                        onEditClick = { onEditEmployeeClick(employee) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EmployeesScreenPreview() {
    val dummyEmployees = listOf(
        Usuario(1, "Juan Pérez", "juan", "", null, null, "ADMINISTRADOR", "EMP01", true, false, System.currentTimeMillis(), 0, null, null),
        Usuario(2, "María López", "maria", "", null, null, "CAJERO", "EMP01", true, false, System.currentTimeMillis(), 0, null, null),
        Usuario(3, "Pedro Gómez", "pedro", "", null, null, "VENDEDOR", "EMP01", true, false, System.currentTimeMillis(), 0, System.currentTimeMillis() + 3600000, null)
    )
    MultiPOSTheme {
        EmployeesScreen(
            employees = dummyEmployees,
            searchQuery = "",
            isLoading = false,
            onSearchChange = {},
            onAddEmployeeClick = {},
            onEditEmployeeClick = {}
        )
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmployeeCard(
    employee: Usuario,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBlocked = employee.bloqueadoHasta != null && employee.bloqueadoHasta!! > System.currentTimeMillis()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked) 
                MaterialTheme.colorScheme.errorContainer 
            else if (employee.requiereCambioClave)
                MaterialTheme.colorScheme.warningContainer
            else 
                MaterialTheme.colorScheme.surface
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = employee.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isBlocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = { },
                            label = { Text("Bloqueado", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
                Text(
                    text = "@${employee.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRole(employee.rol),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar empleado",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatRole(role: String): String {
    return role.lowercase().replaceFirstChar { it.uppercase() }
}
