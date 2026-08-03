package com.multipos.app.ui.pos 
 
import android.os.Bundle 
import android.view.LayoutInflater 
import android.view.View 
import android.view.ViewGroup 
import android.widget.Toast
import androidx.fragment.app.Fragment 
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.withTransaction
import com.multipos.app.adapters.ProductAdapter
import com.multipos.app.R
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.entities.DetalleVenta
import com.multipos.app.data.entities.Producto
import com.multipos.app.data.entities.Venta
import com.multipos.app.databinding.FragmentPosBinding
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
 
class PosFragment : Fragment() { 
    private var _binding: FragmentPosBinding? = null
    private val binding get() = _binding!!
    private val cart = mutableMapOf<Int, Int>()
    private val prices = mutableMapOf<Int, Double>()
    private val products = mutableMapOf<Int, Producto>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { 
        _binding = FragmentPosBinding.inflate(inflater, container, false)
        val adapter = ProductAdapter { product ->
            if (product.stock > (cart[product.id] ?: 0)) cart[product.id] = (cart[product.id] ?: 0) + 1
            updateTotal()
        }
        binding.recyclerProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerProducts.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            DatabaseProvider.get(requireContext()).productoDao().getAll().collect {
                prices.clear(); prices.putAll(it.associate { product -> product.id to product.precioVenta })
                products.clear(); products.putAll(it.associateBy { product -> product.id })
                adapter.submitList(it); updateTotal()
            }
        }
        binding.btnCharge.setOnClickListener { chargeSale() }
        return binding.root
    } 
    private fun updateTotal() {
        val total = cart.entries.sumOf { (id, quantity) -> quantity * (prices[id] ?: 0.0) }
        binding.txtCartTotal.text = getString(com.multipos.app.R.string.cart_total, NumberFormat.getCurrencyInstance(Locale.getDefault()).format(total))
    }
    private fun chargeSale() {
        if (cart.isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_cart, Toast.LENGTH_SHORT).show()
            return
        }
        val db = DatabaseProvider.get(requireContext())
        val total = cart.entries.sumOf { (id, quantity) -> quantity * (prices[id] ?: 0.0) }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.withTransaction {
                    val ventaId = db.ventaDao().insert(Venta(tipoPago = "EFECTIVO", total = total, idUsuario = 1)).toInt()
                    val detalles = cart.map { (id, quantity) ->
                        val product = products[id] ?: error("Producto no disponible")
                        if (db.productoDao().decreaseStock(id, quantity) == 0) error("Stock insuficiente")
                        DetalleVenta(idVenta = ventaId, idProducto = id, cantidad = quantity, precioUnitario = product.precioVenta, subtotal = quantity * product.precioVenta)
                    }
                    db.ventaDao().insertDetalles(detalles)
                }
                cart.clear(); updateTotal()
                Toast.makeText(requireContext(), R.string.sale_saved, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), R.string.stock_error, Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
