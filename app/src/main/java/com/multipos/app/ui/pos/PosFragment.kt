package com.multipos.app.ui.pos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.multipos.app.R
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.ui.pos.compose.POSScreen
import com.multipos.app.ui.scanner.ScannerActivity
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.viewmodel.PosViewModel
import com.multipos.app.viewmodel.PosViewModelFactory

class PosFragment : Fragment() {
    private lateinit var viewModel: PosViewModel
    private var scanConsumer: ((String, String) -> Unit)? = null

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val code = result.data?.getStringExtra(ScannerActivity.EXTRA_SCAN_RESULT).orEmpty()
            val format = result.data?.getStringExtra(ScannerActivity.EXTRA_SCAN_FORMAT).orEmpty()
            if (code.isNotBlank()) scanConsumer?.invoke(code, format)
        }
        scanConsumer = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        val factory = PosViewModelFactory(db.productoDao(), companyId)
        viewModel = ViewModelProvider(this, factory)[PosViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    POSScreen(
                        products = state.filteredProducts,
                        cartLines = state.cart,
                        selectedClient = state.selectedClient,
                        paymentMethod = state.paymentMethod,
                        total = state.total,
                        searchQuery = state.searchQuery,
                        isLoading = state.isLoading,
                        onSearchChange = { viewModel.onSearchQueryChange(it) },
                        onAddToCart = { viewModel.addToCart(it) },
                        onIncreaseQuantity = { viewModel.updateQuantity(it, 1) },
                        onDecreaseQuantity = { viewModel.updateQuantity(it, -1) },
                        onRemoveFromCart = { viewModel.removeFromCart(it) },
                        onClearCart = { viewModel.clearCart() },
                        onPaymentMethodSelected = { viewModel.setPaymentMethod(it) },
                        onClientSelected = { viewModel.setClient(it) },
                        onChargeClick = { 
                            if (state.cart.isEmpty()) {
                                Toast.makeText(context, "El carrito está vacío", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Procesando pago...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onScanProduct = { 
                            scanConsumer = { code, _ ->
                                viewModel.onSearchQueryChange(code)
                                // Intentar agregar automáticamente si hay un match exacto
                                val exactMatch = viewModel.uiState.value.products.find { 
                                    it.codigo == code || it.codigoBarras == code 
                                }
                                if (exactMatch != null) {
                                    viewModel.addToCart(exactMatch)
                                    Toast.makeText(context, "Agregado: ${exactMatch.nombre}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            openScanner("Escanear producto")
                        },
                        onScanClientQr = { 
                            scanConsumer = { code, _ ->
                                viewModel.setClient(code)
                                Toast.makeText(context, "Cliente: $code", Toast.LENGTH_SHORT).show()
                            }
                            openScanner("Escanear QR de cliente")
                        }
                    )
                }
            }
        }
    }

    private fun openScanner(title: String) {
        scannerLauncher.launch(Intent(requireContext(), ScannerActivity::class.java).putExtra(ScannerActivity.EXTRA_TITLE, title))
    }
}
