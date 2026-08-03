package com.multipos.app.ui.inventory 
 
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
import com.multipos.app.adapters.ProductAdapter
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.databinding.FragmentInventoryBinding
import kotlinx.coroutines.launch
import com.multipos.app.R 
import com.multipos.app.data.entities.Producto
 
class InventoryFragment : Fragment() { 
    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { 
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        val db = DatabaseProvider.get(requireContext())
        val adapter = ProductAdapter { showProductDialog(it, db) }
        binding.recyclerInventory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerInventory.adapter = adapter
        binding.btnAddProduct.setOnClickListener { showProductDialog(null, db) }
        viewLifecycleOwner.lifecycleScope.launch {
            DatabaseProvider.get(requireContext()).productoDao().getAll().collect { adapter.submitList(it) }
        }
        return binding.root
    } 
    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    private fun showProductDialog(existing: Producto?, db: com.multipos.app.data.AppDatabase) {
        val fields = listOf("Nombre", "Código", "Precio de venta", "Costo unitario", "Stock")
        val inputs = fields.map { EditText(requireContext()).apply { hint = it; setPadding(24, 12, 24, 12) } }
        existing?.let {
            inputs[0].setText(it.nombre); inputs[1].setText(it.codigo)
            inputs[2].setText(it.precioVenta.toString()); inputs[3].setText(it.costoUnitario.toString()); inputs[4].setText(it.stock.toString())
        }
        val form = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; inputs.forEach(::addView) }
        val builder = AlertDialog.Builder(requireContext()).setTitle(if (existing == null) "Nuevo producto" else "Editar producto")
            .setView(form).setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null)
        if (existing != null) builder.setNeutralButton("Eliminar", null)
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = inputs[0].text.toString().trim(); val code = inputs[1].text.toString().trim()
                val price = inputs[2].text.toString().toDoubleOrNull(); val cost = inputs[3].text.toString().toDoubleOrNull(); val stock = inputs[4].text.toString().toIntOrNull()
                if (name.isBlank() || code.isBlank() || price == null || cost == null || stock == null || price < 0 || cost < 0 || stock < 0) {
                    Toast.makeText(requireContext(), "Completa los datos correctamente", Toast.LENGTH_SHORT).show(); return@setOnClickListener
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val product = Producto(existing?.id ?: 0, name, code, price, cost, stock, existing?.stockMinimo ?: 5, existing?.categoria ?: "General", existing?.fotoUrl ?: "")
                    if (existing == null) db.productoDao().insert(product) else db.productoDao().update(product)
                    dialog.dismiss(); Toast.makeText(requireContext(), "Producto guardado", Toast.LENGTH_SHORT).show()
                }
            }
            if (existing != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch { db.productoDao().delete(existing); dialog.dismiss(); Toast.makeText(requireContext(), "Producto eliminado", Toast.LENGTH_SHORT).show() }
            }
        }
        dialog.show()
    }
}
