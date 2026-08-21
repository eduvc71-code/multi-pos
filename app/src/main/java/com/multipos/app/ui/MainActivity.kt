package com.multipos.app.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.multipos.app.ui.home.compose.HomeScreenWrapper
import com.multipos.app.ui.login.compose.LoginScreenWrapper
import com.multipos.app.ui.setup.compose.SetupScreenWrapper
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")
        enableEdgeToEdge()
        setContent {
            MultiPOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootNavigation()
                }
            }
        }
    }
}

@Composable
fun RootNavigation() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val destination by mainViewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            Log.d("Navigation", "Ruta actual: ${entry.destination.route}")
        }
    }

    LaunchedEffect(destination) {
        destination?.let {
            Log.d("Navigation", "Redirigiendo a: $it (desde MainViewModel)")
            navController.navigate(it) {
                popUpTo("AUTH_CHECK") { inclusive = true }
            }
            mainViewModel.resetDestination()
        }
    }

    NavHost(navController = navController, startDestination = "AUTH_CHECK") {
        composable("AUTH_CHECK") {
            AuthCheckScreen(onCheck = { mainViewModel.checkAuth() })
        }
        composable("LOGIN") {
            LoginScreenWrapper(onLoginSuccess = { 
                Log.d("Navigation", "Login exitoso, navegando a HOME")
                navController.navigate("HOME") { 
                    popUpTo("LOGIN") { inclusive = true }
                    launchSingleTop = true
                } 
            })
        }
        composable("SETUP") {
            SetupScreenWrapper(onSetupSuccess = { 
                Log.d("Navigation", "Setup exitoso, navegando a HOME")
                navController.navigate("HOME") { 
                    popUpTo("SETUP") { inclusive = true }
                } 
            })
        }
        composable("HOME") {
            HomeScreenWrapper(onLogout = { 
                Log.d("Navigation", "Logout detectado, navegando a LOGIN")
                navController.navigate("LOGIN") { 
                    popUpTo("HOME") { inclusive = true }
                } 
            })
        }
    }
}

@Composable
fun AuthCheckScreen(onCheck: () -> Unit) {
    LaunchedEffect(Unit) {
        onCheck()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
