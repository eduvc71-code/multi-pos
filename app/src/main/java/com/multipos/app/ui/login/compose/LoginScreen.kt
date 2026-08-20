package com.multipos.app.ui.login.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.components.MultiPOSTextField
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.ui.theme.onBackgroundVariant

@Composable
fun LoginScreen(
    username: String,
    password: String,
    isLoading: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onExitClick: () -> Unit, // Nueva prop
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Botón Salir en la esquina superior
        IconButton(
            onClick = onExitClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.login_exit_content_desc), tint = Color.Gray)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Logo Circular con Brillo Radial (Prompt 1)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(80.dp),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = stringResource(R.string.login_logo_content_desc),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.displaySmall, // Un poco más pequeño para que no wrapée
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.sp
            )
            
            Text(
                text = stringResource(R.string.login_subtitle).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Card de login Estilo Executive
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.login_section_title),
                        style = MaterialTheme.typography.titleMedium, // Reducido para mejor balance
                        fontWeight = FontWeight.Black
                    )
                    
                    Text(
                        text = stringResource(R.string.login_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    MultiPOSTextField(
                        value = username,
                        onValueChange = onUsernameChange,
                        label = stringResource(R.string.login_hint_user),
                        leadingIcon = {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                    
                    MultiPOSTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = stringResource(R.string.login_hint_pass),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    MultiPOSButton(
                        text = stringResource(R.string.login_button),
                        onClick = onLoginClick,
                        showLoading = isLoading
                    )
                    
                    TextButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "¿Olvidaste tu contraseña?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = stringResource(R.string.login_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    MultiPOSTheme {
        LoginScreen(
            username = "admin",
            password = "password",
            isLoading = false,
            onUsernameChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onExitClick = {}
        )
    }
}
