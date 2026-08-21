package com.multipos.app.ui.home.compose

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.multipos.app.R
import com.multipos.app.viewmodel.HomeViewModel

@Composable
fun HomeScreenWrapper(onLogout: () -> Unit) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()

    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) {
            onLogout()
        }
    }

    HomeScreen(
        userName = uiState.userName,
        companyColor = uiState.companyColor,
        onLogoutClick = { viewModel.logout() },
        onMenuItemClick = { menu -> 
            if (viewModel.canNavigateTo(menu)) {
                viewModel.setSelectedMenu(menu)
                navController.navigate(menu) {
                    popUpTo("DASHBOARD") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                Toast.makeText(context, R.string.home_no_permission_toast, Toast.LENGTH_SHORT).show()
            }
        },
        selectedMenu = uiState.selectedMenu
    ) {
        NavHost(
            navController = navController,
            startDestination = "DASHBOARD",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("DASHBOARD") {
                DashboardScreenWrapper(
                    companyId = uiState.companyId,
                    companyName = uiState.activeCompanyName,
                    onLogoutClick = { viewModel.logout() }
                )
            }
            composable("POS") {
                POSScreenWrapper(
                    companyId = uiState.companyId,
                    userId = uiState.userId
                )
            }
            composable("INVENTORY") {
                InventoryScreenWrapper(
                    companyId = uiState.companyId
                )
            }
            composable("HISTORY") {
                HistoryScreenWrapper(
                    companyId = uiState.companyId
                )
            }
            composable("CLIENTS") {
                ClientsScreenWrapper(
                    companyId = uiState.companyId
                )
            }
            composable("EMPLOYEES") {
                EmployeesScreenWrapper(
                    companyId = uiState.companyId
                )
            }
            composable("CASH") {
                CashScreenWrapper(
                    companyId = uiState.companyId,
                    userId = uiState.userId
                )
            }
            composable("REPORTS") {
                ReportsScreenWrapper(
                    companyId = uiState.companyId
                )
            }
        }
    }
}
