package com.multipos.app.ui.employees.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multipos.app.R
import com.multipos.app.data.entities.Usuario
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.components.MultiPOSSearchField
import com.multipos.app.ui.theme.MultiPOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(
    employees: List<Usuario>,
    searchQuery: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onSearchChange: (String) -> Unit,
    onAddEmployeeClick: () -> Unit,
    onEditEmployeeClick: (Usuario) -> Unit,
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.employees_title),
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
                placeholder = stringResource(R.string.employees_search_placeholder)
            )
            
            // Container tipo lista única
            MultiPOSCard(modifier = Modifier.weight(1f), elevation = 1.dp) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (employees.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.employees_no_collaborators), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        val isBlocked = employee.bloqueado
        
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
        Usuario(id = 1, nombre = "Juan Pérez", usuario = "admin", password = "", rol = "PROPIETARIO", empresaId = "EMP01", activo = true, fechaCreacion = System.currentTimeMillis()),
        Usuario(id = 2, nombre = "María López", usuario = "maria", password = "", rol = "VENDEDOR", empresaId = "EMP01", activo = true, fechaCreacion = System.currentTimeMillis())
    )
    MultiPOSTheme {
        EmployeesScreen(
            employees = dummyEmployees,
            searchQuery = "",
            isLoading = false,
            modifier = Modifier,
            onSearchChange = {},
            onAddEmployeeClick = {},
            onEditEmployeeClick = {}
        )
    }
}
