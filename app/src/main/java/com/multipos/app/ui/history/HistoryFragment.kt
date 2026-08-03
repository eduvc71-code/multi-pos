package com.multipos.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.multipos.app.adapters.SaleAdapter
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val adapter = SaleAdapter()
        binding.recyclerSales.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSales.adapter = adapter
        val db = DatabaseProvider.get(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            db.ventaDao().getAll().collect { adapter.submitList(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            binding.txtTodayTotal.text = "Total de hoy: ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(db.ventaDao().totalSince(start))}"
        }
        return binding.root
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
