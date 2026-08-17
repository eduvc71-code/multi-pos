package com.multipos.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.multipos.app.R
import com.multipos.app.adapters.SaleAdapter
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.databinding.FragmentHistoryBinding
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import kotlinx.coroutines.launch
import java.util.Calendar
import com.multipos.app.util.Money

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val adapter = SaleAdapter()
        adapter.onItemClick = { sale ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.homeContainer, SaleDetailFragment.newInstance(sale.id))
                .addToBackStack(null)
                .commit()
        }
        binding.recyclerSales.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSales.adapter = adapter
        val db = DatabaseProvider.get(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            if (!ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.VIEW_HISTORY)) {
                return@launch
            }
            launch {
                db.ventaDao().getAll(ActiveCompanyStore.get(requireContext())).collect { adapter.submitList(it) }
            }
            val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            binding.txtTodayTotal.text = getString(
                R.string.today_total_format,
                Money.format(db.ventaDao().totalSince(start, ActiveCompanyStore.get(requireContext())))
            )
        }
        return binding.root
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
