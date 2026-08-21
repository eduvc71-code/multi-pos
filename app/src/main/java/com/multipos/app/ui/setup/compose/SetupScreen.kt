package com.multipos.app.ui.setup.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multipos.app.R
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.components.MultiPOSTextField
import com.multipos.app.ui.theme.MultiPOSTheme

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
    modifier: Modifier = Modifier,
    onCreateClick: () -> Unit,
) {
    var passVisible by remember { mutableStateOf(value = false) }
    var confirmPassVisible by remember { mutableStateOf(value = false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header Executive
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Card Única Premium
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                
                Text(text = stringResource(R.string.setup_business_section), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                
                MultiPOSTextField(
                    value = businessName,
                    onValueChange = onBusinessNameChange,
                    label = stringResource(R.string.setup_business_name_label),
                    leadingIcon = { Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.primary) }
                )
                
                MultiPOSTextField(
                    value = businessNit,
                    onValueChange = onBusinessNitChange,
                    label = stringResource(R.string.setup_business_nit_label),
                    leadingIcon = { Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.primary) }
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Text(text = stringResource(R.string.setup_owner_section), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                
                MultiPOSTextField(
                    value = ownerName,
                    onValueChange = onOwnerNameChange,
                    label = stringResource(R.string.setup_owner_name_label),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) }
                )
                
                MultiPOSTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = stringResource(R.string.setup_username_label),
                    leadingIcon = { Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary) }
                )
                
                MultiPOSTextField(
                    value = pass,
                    onValueChange = onPassChange,
                    label = stringResource(R.string.setup_password_label),
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passVisible = !passVisible }) {
                            Icon(if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    }
                )

                MultiPOSTextField(
                    value = confirmPass,
                    onValueChange = onConfirmPassChange,
                    label = stringResource(R.string.setup_confirm_password_label),
                    leadingIcon = { Icon(Icons.Default.LockReset, null, tint = MaterialTheme.colorScheme.primary) },
                    visualTransformation = if (confirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                            Icon(if (confirmPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                MultiPOSButton(
                    text = stringResource(R.string.setup_create_button),
                    onClick = onCreateClick,
                    showLoading = isLoading
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SetupScreenPreview() {
    MultiPOSTheme {
        SetupScreen(
            businessName = "Mi Negocio",
            onBusinessNameChange = {},
            businessNit = "12345678-9",
            onBusinessNitChange = {},
            ownerName = "Juan Pérez",
            onOwnerNameChange = {},
            username = "admin",
            onUsernameChange = {},
            pass = "password123",
            onPassChange = {},
            confirmPass = "password123",
            onConfirmPassChange = {},
            isLoading = false,
            onCreateClick = {}
        )
    }
}
