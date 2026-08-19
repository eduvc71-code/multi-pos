package com.multipos.app.ui.reports

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
import com.multipos.app.ui.reports.compose.ReportsScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.viewmodel.ReportsViewModel
import com.multipos.app.viewmodel.ReportsViewModelFactory

class ReportesFragment : Fragment() {
    private lateinit var viewModel: ReportsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        val userId = com.multipos.app.data.UserSessionStore.userId(requireContext())
        val factory = ReportsViewModelFactory(db, companyId, userId)
        viewModel = ViewModelProvider(this, factory)[ReportsViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    ReportsScreen(
                        reportType = state.reportType.name,
                        reportData = state.reportData,
                        isLoading = state.isLoading,
                        onGenerateReport = { viewModel.generateReport() },
                        onExportCsv = { viewModel.exportCsv() }
                    )
                }
            }
        }
    }
}
