package com.multipos.app.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.multipos.app.adapters.ClientAdapter
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.entities.Cliente
import com.multipos.app.databinding.FragmentClientsBinding
import kotlinx.coroutines.launch

class ClientsFragment : Fragment() {
    private var _binding: FragmentClientsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClientsBinding.inflate(inflater, container, false)
        val db = DatabaseProvider.get(requireContext()); val adapter = ClientAdapter { showDialog(it, db) }
        binding.recyclerClients.layoutManager = LinearLayoutManager(requireContext()); binding.recyclerClients.adapter = adapter
        binding.btnAddClient.setOnClickListener { showDialog(null, db) }
        viewLifecycleOwner.lifecycleScope.launch { db.clienteDao().getAll().collect { adapter.submitList(it) } }
        return binding.root
    }
    private fun showDialog(existing: Cliente?, db: com.multipos.app.data.AppDatabase) {
        val inputs = listOf("Nombre", "Documento", "Teléfono", "Límite de crédito").map { EditText(requireContext()).apply { hint = it; setPadding(24, 12, 24, 12) } }
        existing?.let { inputs[0].setText(it.nombre); inputs[1].setText(it.documento); inputs[2].setText(it.telefono); inputs[3].setText(it.limiteCredito.toString()) }
        val form = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; inputs.forEach(::addView) }
        val builder = AlertDialog.Builder(requireContext()).setTitle(if (existing == null) "Nuevo cliente" else "Editar cliente").setView(form).setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null)
        if (existing != null) builder.setNeutralButton("Eliminar", null)
        val dialog = builder.create(); dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = inputs[0].text.toString().trim(); val doc = inputs[1].text.toString().trim(); val limit = inputs[3].text.toString().toDoubleOrNull()
                if (name.isBlank() || doc.isBlank() || limit == null || limit < 0) { Toast.makeText(requireContext(), "Completa los datos correctamente", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                viewLifecycleOwner.lifecycleScope.launch { db.clienteDao().let { dao -> val c = Cliente(existing?.id ?: 0, name, doc, inputs[2].text.toString().trim(), limiteCredito = limit, creditoActual = existing?.creditoActual ?: 0.0); if (existing == null) dao.insert(c) else dao.update(c) }; dialog.dismiss() }
            }
            if (existing != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { viewLifecycleOwner.lifecycleScope.launch { db.clienteDao().delete(existing); dialog.dismiss() } }
        }; dialog.show()
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
