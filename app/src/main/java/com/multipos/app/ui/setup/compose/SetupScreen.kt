package com.multipos.app.ui.setup.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.components.MultiPOSTextField
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.ui.theme.onBackgroundVariant

@Composable
fun SetupScreen(
    businessName: String,
    onBusinessNameChange: (String) -> Unit,
    businessNit: String,
    onBusinessNitChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    pass: String,
    onPassChange: (String) -> Unit,
    confirmPass: String,
    onConfirmPassChange: (String) -> Unit,
    isLoading: Boolean,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passVisible by remember { mutableStateOf(false) }
    var confirmPassVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Configura MultiPOS",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Registra tu negocio y crea la cuenta segura del propietario.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackgroundVariant
            )
        }

        // Formulario
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                
                // Sección 1: Datos del negocio
                SectionHeader(number = "1", title = "Datos del negocio")
                
                MultiPOSTextField(
                    value = businessName,
                    onValueChange = onBusinessNameChange,
                    label = "Nombre del negocio",
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) }
                )
                
                MultiPOSTextField(
                    value = businessNit,
                    onValueChange = onBusinessNitChange,
                    label = "NIT (opcional)",
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Sección 2: Propietario
                SectionHeader(number = "2", title = "Propietario y acceso")
                
                MultiPOSTextField(
                    value = ownerName,
                    onValueChange = onOwnerNameChange,
                    label = "Nombre del propietario",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                
                MultiPOSTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = "Usuario",
                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) }
                )
                
                MultiPOSTextField(
                    value = pass,
                    onValueChange = onPassChange,
                    label = "Crear contraseña",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passVisible = !passVisible }) {
                            Icon(if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    }
                )
                
                MultiPOSTextField(
                    value = confirmPass,
                    onValueChange = onConfirmPassChange,
                    label = "Confirmar contraseña",
                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                    visualTransformation = if (confirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                            Icon(if (confirmPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                MultiPOSButton(
                    text = "CREAR ESPACIO DE TRABAJO",
                    onClick = onCreateClick,
                    enabled = !isLoading,
                    showLoading = isLoading
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SectionHeader(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = number, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
