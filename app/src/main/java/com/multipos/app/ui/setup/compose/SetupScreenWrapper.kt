package com.multipos.app.ui.setup.compose

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.multipos.app.viewmodel.SetupViewModel

@Composable
fun SetupScreenWrapper(
    modifier: Modifier = Modifier,
    onSetupSuccess: () -> Unit
) {
    val viewModel: SetupViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.setupSuccess) {
        if (uiState.setupSuccess) {
            onSetupSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val message = when (error) {
                "BUSINESS_NAME_SHORT" -> "El nombre del negocio es muy corto"
                "OWNER_NAME_SHORT" -> "El nombre del dueño es muy corto"
                "USERNAME_INVALID" -> "Nombre de usuario inválido (4-30 caracteres, a-z, 0-9, . _ -)"
                "PASSWORD_SHORT" -> "La contraseña debe tener al menos 8 caracteres"
                "PASSWORD_MISMATCH" -> "Las contraseñas no coinciden"
                "COMPANY_EXISTS" -> "Ya existe una empresa configurada"
                else -> "Error: $error"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    SetupScreen(
        modifier = modifier,
        businessName = uiState.businessName,
        onBusinessNameChange = viewModel::updateBusinessName,
        businessNit = uiState.businessNit,
        onBusinessNitChange = viewModel::updateBusinessNit,
        ownerName = uiState.ownerName,
        onOwnerNameChange = viewModel::updateOwnerName,
        username = uiState.username,
        onUsernameChange = viewModel::updateUsername,
        pass = uiState.pass,
        onPassChange = viewModel::updatePass,
        confirmPass = uiState.confirmPass,
        onConfirmPassChange = viewModel::updateConfirmPass,
        isLoading = uiState.isLoading,
        onCreateClick = viewModel::createWorkspace
    )
}
