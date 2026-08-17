package com.multipos.app.ui.history

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
import com.multipos.app.ui.history.compose.HistoryScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.viewmodel.HistoryViewModel
import com.multipos.app.viewmodel.HistoryViewModelFactory

class HistoryFragment : Fragment() {
    private lateinit var viewModel: HistoryViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        val factory = HistoryViewModelFactory(db.ventaDao(), companyId)
        viewModel = ViewModelProvider(this, factory)[HistoryViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    HistoryScreen(
                        sales = state.filteredSales,
                        searchQuery = state.searchQuery,
                        isLoading = state.isLoading,
                        onSearchChange = { viewModel.onSearchQueryChange(it) },
                        totalToday = state.totalToday,
                        onSaleClick = { sale ->
                            // Navegar al detalle (tradicional por ahora o implementar SaleDetailFragment con Compose)
                            parentFragmentManager.beginTransaction()
                                .replace(com.multipos.app.R.id.homeContainer, SaleDetailFragment.newInstance(sale.id))
                                .addToBackStack(null)
                                .commit()
                        }
                    )
                }
            }
        }
    }
}
