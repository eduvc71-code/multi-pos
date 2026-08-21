package com.multipos.app.ui.login.compose

import android.app.Activity
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.multipos.app.viewmodel.LoginViewModel

@Composable
fun LoginScreenWrapper(
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit
) {
    val viewModel: LoginViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            viewModel.consumeLoginSuccess()
            onLoginSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val message = when {
                error == "CREDENTIALS_REQUIRED" -> "Credenciales requeridas"
                error.startsWith("BLOCKED") -> {
                    val mins = error.split("|")[1]
                    "Usuario bloqueado por $mins minutos"
                }
                error == "INVALID_CREDENTIALS" -> "Usuario o contraseña incorrectos"
                else -> "Error al iniciar sesión: $error"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LoginScreen(
        modifier = modifier,
        username = uiState.username,
        password = uiState.password,
        isLoading = uiState.isLoading,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::login,
        onExitClick = { (context as? Activity)?.finishAffinity() }
    )
}
