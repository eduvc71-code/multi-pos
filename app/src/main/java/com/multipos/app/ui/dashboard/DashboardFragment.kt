package com.multipos.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.ui.dashboard.compose.DashboardScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.viewmodel.DashboardViewModel
import com.multipos.app.viewmodel.DashboardViewModelFactory

class DashboardFragment : Fragment() {
    private lateinit var viewModel: DashboardViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        val factory = DashboardViewModelFactory(db, companyId)
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    DashboardScreen(
                        totalSalesToday = state.totalSalesToday,
                        totalProducts = state.totalProducts,
                        lowStockCount = state.lowStockCount
                    )
                }
            }
        }
    }
}
