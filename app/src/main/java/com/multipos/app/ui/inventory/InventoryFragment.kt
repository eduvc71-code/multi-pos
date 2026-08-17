package com.multipos.app.ui.inventory

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.multipos.app.R
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.InventoryMovementException
import com.multipos.app.data.InventoryMovementRepository
import com.multipos.app.data.InventoryMovementRequest
import com.multipos.app.data.entities.MovimientoInventario
import com.multipos.app.data.entities.Producto
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import com.multipos.app.ui.inventory.compose.InventoryScreen
import com.multipos.app.ui.scanner.ScannerActivity
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money
import com.multipos.app.viewmodel.InventoryViewModel
import com.multipos.app.viewmodel.InventoryViewModelFactory
import kotlinx.coroutines.launch

class InventoryFragment : Fragment() {
    private lateinit var viewModel: InventoryViewModel
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
        val factory = InventoryViewModelFactory(db.productoDao(), companyId)
        viewModel = ViewModelProvider(this, factory)[InventoryViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MultiPOSTheme {
                    val state by viewModel.uiState.collectAsState()
                    InventoryScreen(
                        products = state.filteredProducts,
                        searchQuery = state.searchQuery,
                        isLoading = state.isLoading,
                        onSearchChange = { viewModel.onSearchQueryChange(it) },
                        onAddProductClick = {
                            viewLifecycleOwner.lifecycleScope.launch {
                                if (ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.MANAGE_INVENTORY)) {
                                    showProductDialog(null, db)
                                }
                            }
                        },
                        onEditProductClick = { product ->
                            showProductDialog(product, db)
                        },
                        onDeleteProductClick = { product ->
                            confirmDeleteProduct(product, db)
                        },
                        onMovementsClick = {
                            viewLifecycleOwner.lifecycleScope.launch {
                                if (ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.MANAGE_INVENTORY)) {
                                    showMovementDialog(db, companyId, viewModel.uiState.value.products)
                                }
                            }
                        },
                        onScanClick = {
                            scanConsumer = { code, _ ->
                                viewModel.onSearchQueryChange(code)
                            }
                            openScanner("Buscar producto")
                        }
                    )
                }
            }
        }
    }

    private fun confirmDeleteProduct(product: Producto, db: AppDatabase) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar producto")
            .setMessage("Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    if (!ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.MANAGE_INVENTORY)) {
                        Toast.makeText(requireContext(), "No tienes permiso para eliminar productos", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    if (db.productoDao().archive(product.id, product.empresaId) == 1) {
                        Toast.makeText(requireContext(), "Producto eliminado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showProductDialog(existing: Producto?, db: AppDatabase, scannedBarcode: String? = null, scannedFormat: String? = null) {
        val fields = listOf("Nombre", "Código interno", "Código de barras (opcional)", "Precio de venta", "Costo unitario", "Stock")
        val inputs = fields.map { EditText(requireContext()).apply { hint = it; setPadding(24, 12, 24, 12) } }
        existing?.let {
            inputs[0].setText(it.nombre)
            inputs[1].setText(it.codigo)
            inputs[2].setText(it.codigoBarras.orEmpty())
            inputs[3].setText(Money.toInput(it.precioVenta))
            inputs[4].setText(Money.toInput(it.costoUnitario))
            inputs[5].setText(it.stock.toString())
        }
        if (existing == null && scannedBarcode != null) inputs[2].setText(scannedBarcode)
        val scanButton = Button(requireContext()).apply { text = getString(R.string.scan_code_camera) }
        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            inputs.forEach(::addView)
            addView(scanButton)
        }
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) "Nuevo producto" else "Editar producto")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
        if (existing != null) builder.setNeutralButton("Eliminar", null)
        val dialog = builder.create()
        scanButton.setOnClickListener {
            scanConsumer = { code, _ -> inputs[2].setText(code) }
            openScanner("Código del producto")
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = inputs[0].text.toString().trim()
                val code = inputs[1].text.toString().trim()
                val barcode = inputs[2].text.toString().trim().ifEmpty { null }
                val price = Money.parseMinorUnits(inputs[3].text.toString())
                val cost = Money.parseMinorUnits(inputs[4].text.toString())
                val stock = inputs[5].text.toString().toIntOrNull()
                if (name.isBlank() || code.isBlank() || price == null || cost == null || stock == null || stock < 0) {
                    Toast.makeText(requireContext(), "Completa los datos correctamente", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                saveButton.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    if (!ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.MANAGE_INVENTORY)) {
                        saveButton.isEnabled = true
                        Toast.makeText(requireContext(), "Ya no tienes permiso para guardar productos", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    val product = Producto(
                        id = existing?.id ?: 0,
                        nombre = name,
                        codigo = code,
                        precioVenta = price,
                        costoUnitario = cost,
                        stock = stock,
                        stockMinimo = existing?.stockMinimo ?: 5,
                        categoria = existing?.categoria ?: "General",
                        fotoUrl = existing?.fotoUrl ?: "",
                        codigoBarras = barcode,
                        tipoCodigo = if (barcode != null) (scannedFormat ?: existing?.tipoCodigo) else null,
                        empresaId = existing?.empresaId ?: ActiveCompanyStore.get(requireContext())
                    )
                    try {
                        if (existing == null) db.productoDao().insert(product) else db.productoDao().update(product)
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "Producto guardado", Toast.LENGTH_SHORT).show()
                    } catch (_: SQLiteConstraintException) {
                        Toast.makeText(requireContext(), "El código interno o de barras ya está registrado", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), "No se pudo guardar el producto. Intenta nuevamente.", Toast.LENGTH_LONG).show()
                    } finally {
                        if (dialog.isShowing) saveButton.isEnabled = true
                    }
                }
            }
            if (existing != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                confirmDeleteProduct(existing, db)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun openScanner(title: String) {
        scannerLauncher.launch(Intent(requireContext(), ScannerActivity::class.java).putExtra(ScannerActivity.EXTRA_TITLE, title))
    }

    private fun showMovementDialog(
        db: AppDatabase,
        companyId: String,
        catalog: List<Producto>
    ) {
        if (catalog.isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_products, Toast.LENGTH_SHORT).show()
            return
        }
        val context = requireContext()
        val productSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, catalog.map { it.nombre })
        }
        val typeSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listOf(
                getString(R.string.inventory_movement_tipo_entrada),
                getString(R.string.inventory_movement_tipo_salida),
                getString(R.string.inventory_movement_tipo_ajuste)
            ))
        }
        val quantityInput = EditText(context).apply {
            hint = getString(R.string.inventory_movement_cantidad_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val reasonInput = EditText(context).apply {
            hint = getString(R.string.inventory_movement_motivo_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
        }
        typeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                quantityInput.hint = getString(
                    if (position == 2) R.string.inventory_movement_ajuste_hint
                    else R.string.inventory_movement_cantidad_hint
                )
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(productSpinner)
            addView(typeSpinner)
            addView(quantityInput)
            addView(reasonInput)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.inventory_movement_title)
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val product = catalog[productSpinner.selectedItemPosition]
                val tipo = when (typeSpinner.selectedItemPosition) {
                    0 -> MovimientoInventario.TIPO_ENTRADA_MANUAL
                    1 -> MovimientoInventario.TIPO_SALIDA_MANUAL
                    else -> MovimientoInventario.TIPO_AJUSTE
                }
                val cantidad = quantityInput.text.toString().toIntOrNull()
                val motivo = reasonInput.text.toString().trim()
                if (cantidad == null || cantidad == 0) {
                    Toast.makeText(context, R.string.inventory_movement_cantidad_hint, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (motivo.length !in 5..300) {
                    Toast.makeText(context, R.string.inventory_movement_motivo_hint, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                confirmButton.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        InventoryMovementRepository(db).registerMovement(
                            InventoryMovementRequest(
                                companyId = companyId,
                                productId = product.id,
                                userId = com.multipos.app.data.UserSessionStore.userId(requireContext()),
                                tipo = tipo,
                                cantidad = cantidad,
                                motivo = motivo
                            )
                        )
                        dialog.dismiss()
                        Toast.makeText(context, R.string.inventory_movement_saved, Toast.LENGTH_SHORT).show()
                    } catch (error: InventoryMovementException) {
                        confirmButton.isEnabled = true
                        Toast.makeText(context, error.message ?: getString(R.string.inventory_movement_error), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }
}
