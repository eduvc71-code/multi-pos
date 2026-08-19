package com.multipos.app.ui.employees.compose

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
import com.multipos.app.data.entities.Usuario
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.components.MultiPOSSearchField
import com.multipos.app.ui.theme.MultiPOSTheme

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
                        text = "Equipo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    IconButton(onClick = onAddEmployeeClick) {
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
                placeholder = "Buscar colaborador..."
            )
            
            // Container tipo lista única
            MultiPOSCard(modifier = Modifier.weight(1f), elevation = 1.dp) {
                if (employees.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay colaboradores", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        items(employees, key = { it.id }) { employee ->
                            EmployeeListItemPremium(
                                employee = employee,
                                onEditClick = { onEditEmployeeClick(employee) }
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
fun EmployeeListItemPremium(
    employee: Usuario,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isBlocked = employee.bloqueadoHasta != null && employee.bloqueadoHasta!! > System.currentTimeMillis()
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = employee.nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = employee.rol,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                if (isBlocked) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• BLOQUEADO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        IconButton(onClick = onEditClick) {
            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EmployeesScreenPreview() {
    val dummyEmployees = listOf(
        Usuario(1, "Juan Pérez", "admin", "", null, null, "PROPIETARIO", "EMP01", true, false, System.currentTimeMillis(), 0, null, null),
        Usuario(2, "María López", "maria", "", null, null, "VENDEDOR", "EMP01", true, false, System.currentTimeMillis(), 0, null, null)
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
