package com.multipos.app.ui.clients

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
import com.multipos.app.ui.clients.compose.ClientsScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.viewmodel.ClientsViewModel
import com.multipos.app.viewmodel.ClientsViewModelFactory

class ClientsFragment : Fragment() {
    private lateinit var viewModel: ClientsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        val factory = ClientsViewModelFactory(db.clienteDao(), companyId)
        viewModel = ViewModelProvider(this, factory)[ClientsViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    ClientsScreen(
                        clients = state.filteredClients,
                        searchQuery = state.searchQuery,
                        isLoading = state.isLoading,
                        onSearchChange = { viewModel.onSearchQueryChange(it) },
                        onAddClientClick = { /* Abrir diálogo tradicional de cliente */ },
                        onEditClientClick = { /* Abrir detalle de cliente */ },
                        onViewStatementClick = { /* Abrir estado de cuenta */ }
                    )
                }
            }
        }
    }
}
