package com.multipos.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.databinding.FragmentDashboardBinding
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import kotlinx.coroutines.launch
import java.util.Calendar
import com.multipos.app.util.Money

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val db = DatabaseProvider.get(requireContext()); val companyId = ActiveCompanyStore.get(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            if (!ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.VIEW_DASHBOARD)) {
                return@launch
            }
            val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            binding.txtDashboardSales.text = Money.format(db.ventaDao().totalSince(start, companyId))
            binding.txtDashboardProducts.text = db.productoDao().count(companyId).toString()
            binding.txtDashboardLowStock.text = db.productoDao().lowStockCount(companyId).toString()
        }
        return binding.root
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
