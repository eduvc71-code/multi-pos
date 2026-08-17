package com.multipos.app.ui.pos 
 
import android.app.Activity
import android.content.Intent
import android.os.Bundle 
import android.view.LayoutInflater 
import android.view.View 
import android.view.ViewGroup 
import android.widget.AdapterView
import android.widget.Toast
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment 
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.multipos.app.adapters.ProductAdapter
import com.multipos.app.adapters.CartAdapter
import com.multipos.app.adapters.CartLine
import com.multipos.app.util.ReceiptPdfGenerator
import com.multipos.app.util.Money
import com.multipos.app.R
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.RegisterSaleRequest
import com.multipos.app.data.SaleLineSnapshot
import com.multipos.app.data.SaleRegistrationException
import com.multipos.app.data.SaleRepository
import com.multipos.app.data.CashRepository
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.Producto
import com.multipos.app.databinding.FragmentPosBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.multipos.app.ui.scanner.ScannerActivity
import com.multipos.app.security.QrCredentialService
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import androidx.appcompat.app.AlertDialog
 
class PosFragment : Fragment() { 
    private var _binding: FragmentPosBinding? = null
    private val binding get() = _binding!!
    private val cart = mutableMapOf<Int, Int>()
    private val prices = mutableMapOf<Int, Long>()
    private val products = mutableMapOf<Int, Producto>()
    private var catalog = emptyList<Producto>()
    private lateinit var cartAdapter: CartAdapter
    private var selectedCreditClient: Cliente? = null
    private var scannedCredentialId: String? = null
    private var scannedPin: String? = null
    private var scannerTarget = ScannerTarget.PRODUCT
    private val saleSubmissionGuard = SaleSubmissionGuard()
    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val code = result.data?.getStringExtra(ScannerActivity.EXTRA_SCAN_RESULT).orEmpty()
            if (code.isNotBlank() && scannerTarget == ScannerTarget.PRODUCT) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val product = DatabaseProvider.get(requireContext()).productoDao().getByCode(ActiveCompanyStore.get(requireContext()), code)
                    if (product == null) Toast.makeText(requireContext(), "No encontramos un producto con este código", Toast.LENGTH_LONG).show()
                    else addToCart(product)
                }
            } else if (code.isNotBlank()) {
                identifyCreditClient(code)
            }
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { 
        _binding = FragmentPosBinding.inflate(inflater, container, false)
        val adapter = ProductAdapter { product -> addToCart(product) }
        cartAdapter = CartAdapter({ changeQuantity(it, -1) }, { changeQuantity(it, 1) })
        binding.recyclerProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerProducts.adapter = adapter
        binding.recyclerCart.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCart.adapter = cartAdapter
        binding.btnCharge.isEnabled = false
        binding.btnScanProduct.isEnabled = false
        binding.btnScanClientQr.isEnabled = false
        binding.spinnerPayment.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("EFECTIVO", "TARJETA", "TRANSFERENCIA", "CREDITO"))
        binding.spinnerClient.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("No aplica"))
        binding.spinnerPayment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isCredit = binding.spinnerPayment.selectedItem?.toString() == "CREDITO"
                binding.btnScanClientQr.visibility = if (isCredit) View.VISIBLE else View.GONE
                binding.txtCreditClient.visibility = if (isCredit) View.VISIBLE else View.GONE
                if (!isCredit) clearCreditClient()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val database = DatabaseProvider.get(requireContext())
            if (!ActiveCompanyAccess.allows(requireContext(), database, CompanyPermission.SELL)) {
                Toast.makeText(requireContext(), "No tienes permiso para registrar ventas", Toast.LENGTH_LONG).show()
                return@launch
            }
            binding.btnCharge.isEnabled = true
            binding.btnScanProduct.isEnabled = true
            binding.btnScanClientQr.isEnabled = true
            database.productoDao().getAll(ActiveCompanyStore.get(requireContext())).collect {
                catalog = it
                prices.clear(); prices.putAll(it.associate { product -> product.id to product.precioVenta })
                products.clear(); products.putAll(it.associateBy { product -> product.id })
                adapter.submitList(filterProducts(binding.etProductSearch.text.toString())); updateCart()
            }
        }
        binding.etProductSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { adapter.submitList(filterProducts(s?.toString().orEmpty())) }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        val totalsWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateTotal() }
            override fun afterTextChanged(s: Editable?) = Unit
        }
        binding.etDiscount.addTextChangedListener(totalsWatcher)
        binding.etTax.addTextChangedListener(totalsWatcher)
        binding.btnCharge.setOnClickListener { chargeSale() }
        binding.btnScanProduct.setOnClickListener {
            scannerTarget = ScannerTarget.PRODUCT
            scannerLauncher.launch(Intent(requireContext(), ScannerActivity::class.java).putExtra(ScannerActivity.EXTRA_TITLE, "Escanear producto"))
        }
        binding.btnScanClientQr.setOnClickListener {
            scannerTarget = ScannerTarget.CLIENT_CREDENTIAL
            scannerLauncher.launch(Intent(requireContext(), ScannerActivity::class.java)
                .putExtra(ScannerActivity.EXTRA_TITLE, "Credencial privada de crédito")
                .putExtra(ScannerActivity.EXTRA_ALLOW_MANUAL, false))
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            val keys = savedInstanceState.getIntArray("cart_keys") ?: intArrayOf()
            val values = savedInstanceState.getIntArray("cart_values") ?: intArrayOf()
            cart.clear()
            for (i in keys.indices) {
                if (i < values.size) {
                    cart[keys[i]] = values[i]
                }
            }
            scannedCredentialId = savedInstanceState.getString("scanned_credential_id")
            scannedPin = savedInstanceState.getString("scanned_pin")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("cart_keys", cart.keys.toIntArray())
        outState.putIntArray("cart_values", cart.values.toIntArray())
        outState.putString("scanned_credential_id", scannedCredentialId)
        outState.putString("scanned_pin", scannedPin)
    }
    private fun filterProducts(query: String): List<Producto> { val q = query.trim().lowercase(); return if (q.isBlank()) catalog else catalog.filter { it.nombre.lowercase().contains(q) || it.codigo.lowercase().contains(q) || it.codigoBarras?.lowercase()?.contains(q) == true } }
    private fun addToCart(product: Producto) {
        if (saleSubmissionGuard.isInProgress) return
        if (product.stock > (cart[product.id] ?: 0)) {
            cart[product.id] = (cart[product.id] ?: 0) + 1
            updateCart()
        } else Toast.makeText(requireContext(), R.string.stock_error, Toast.LENGTH_SHORT).show()
    }
    private fun changeQuantity(product: Producto, delta: Int) {
        if (saleSubmissionGuard.isInProgress) return
        val next = (cart[product.id] ?: 0) + delta
        if (next <= 0) cart.remove(product.id) else if (next <= product.stock) cart[product.id] = next
        updateCart()
    }
    private fun updateCart() { cartAdapter.submitList(cart.mapNotNull { (id, quantity) -> products[id]?.let { CartLine(it, quantity) } }); updateTotal() }
    private fun updateTotal() {
        val subtotal = cart.entries.sumOf { (id, quantity) -> quantity * (prices[id] ?: 0) }
        val discount = binding.etDiscount.text.toString().takeIf(String::isNotBlank)
            ?.let(Money::parseMinorUnits) ?: 0
        val taxBasisPoints = Money.parsePercentageBasisPoints(binding.etTax.text.toString()) ?: 0
        val taxable = (subtotal - discount).coerceAtLeast(0)
        val total = runCatching {
            Math.addExact(taxable, Money.calculateTax(taxable, taxBasisPoints))
        }.getOrDefault(0)
        binding.txtCartTotal.text = getString(R.string.cart_total, Money.format(total))
    }
    private fun chargeSale() {
        if (cart.isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_cart, Toast.LENGTH_SHORT).show()
            return
        }
        val saleLines = cart.mapNotNull { (id, quantity) ->
            products[id]?.let { CartLine(it, quantity) }
        }
        if (saleLines.size != cart.size) {
            Toast.makeText(requireContext(), R.string.product_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        val db = DatabaseProvider.get(requireContext())
        val saleRepository = SaleRepository(db)
        val companyId = ActiveCompanyStore.get(requireContext())
        val subtotal = saleLines.sumOf { it.quantity * it.product.precioVenta }
        val discountText = binding.etDiscount.text.toString()
        val discount = if (discountText.isBlank()) 0 else Money.parseMinorUnits(discountText)
        if (discount == null) {
            Toast.makeText(requireContext(), "Ingresa un descuento monetario válido", Toast.LENGTH_SHORT).show()
            return
        }
        val taxBasisPoints = Money.parsePercentageBasisPoints(binding.etTax.text.toString())
        if (taxBasisPoints == null) {
            Toast.makeText(requireContext(), "Ingresa un impuesto entre 0 y 100 con hasta dos decimales", Toast.LENGTH_SHORT).show()
            return
        }
        if (discount > subtotal) { Toast.makeText(requireContext(), "El descuento no puede superar el subtotal", Toast.LENGTH_SHORT).show(); return }
        val taxable = subtotal - discount
        val tax = Money.calculateTax(taxable, taxBasisPoints)
        val total = runCatching { Math.addExact(taxable, tax) }.getOrElse {
            Toast.makeText(requireContext(), "El total de la venta es demasiado alto", Toast.LENGTH_SHORT).show()
            return
        }
        val payment = binding.spinnerPayment.selectedItem.toString()
        val client = selectedCreditClient
        val credentialId = scannedCredentialId
        val pin = scannedPin
        if (payment == "CREDITO" && (client == null || credentialId == null || pin == null)) { Toast.makeText(requireContext(), "Escanea la credencial QR del cliente y confirma el PIN para autorizar el crédito", Toast.LENGTH_LONG).show(); return }
        if (!saleSubmissionGuard.tryStart()) return
        setCharging(true)
        viewLifecycleOwner.lifecycleScope.launch {
            var errorMessage: String? = null
            try {
                if (!ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.SELL)) {
                    error("Permiso revocado")
                }
                saleRepository.register(
                    RegisterSaleRequest(
                        paymentType = payment,
                        total = total,
                        subtotal = subtotal,
                        discount = discount,
                        tax = tax,
                        clientId = client?.id,
                        credentialId = credentialId,
                        pin = pin,
                        userId = UserSessionStore.userId(requireContext()),
                        companyId = companyId,
                        lines = saleLines.map { line ->
                            SaleLineSnapshot(
                                productId = line.product.id,
                                quantity = line.quantity,
                                unitPrice = line.product.precioVenta
                            )
                        }
                    )
                )
            } catch (error: Exception) {
                errorMessage = when {
                    error is SaleRegistrationException.CreditLimitExceeded -> "El cliente no tiene crédito disponible"
                    error is SaleRegistrationException.InvalidCredential -> "La credencial fue revocada o reemplazada"
                    error is SaleRegistrationException.CredentialBlocked -> "Credencial bloqueada"
                    error is SaleRegistrationException.CredentialExpired -> "Credencial vencida"
                    error is SaleRegistrationException.IncorrectPin -> "PIN incorrecto"
                    error is SaleRegistrationException.CreditInactive -> "El crédito del cliente no está activo"
                    error is SaleRegistrationException.NoActiveCashSession -> {
                        if (ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.MANAGE_CASH)) {
                            promptOpenCash()
                            null
                        } else {
                            "No hay caja abierta. Solicita a un responsable que abra la caja"
                        }
                    }
                    error.message?.contains("Permiso") == true -> "Ya no tienes permiso para registrar ventas"
                    else -> getString(R.string.stock_error)
                }
            } finally {
                saleSubmissionGuard.finish()
                setCharging(false)
            }
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                return@launch
            }

            cart.clear(); binding.etDiscount.text.clear(); binding.etTax.text.clear(); clearCreditClient(); updateCart()
            Toast.makeText(requireContext(), R.string.sale_saved, Toast.LENGTH_SHORT).show()
            try {
                val companyName = db.empresaDao().getById(companyId)?.nombre ?: "MultiPOS"
                val receipt = withContext(Dispatchers.IO) { ReceiptPdfGenerator.createSale(requireContext(), companyName, saleLines, subtotal, discount, tax, total, payment) }
                ReceiptPdfGenerator.share(requireContext(), receipt, "Comprobante de venta")
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "La venta se guardó, pero no se pudo generar el comprobante", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun setCharging(charging: Boolean) {
        _binding?.btnCharge?.apply {
            isEnabled = !charging
            text = getString(if (charging) R.string.sale_processing else R.string.charge_sale)
        }
    }
    private fun promptOpenCash() {
        val context = requireContext()
        val amountInput = android.widget.EditText(context).apply {
            hint = getString(R.string.cash_hint_apertura)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(amountInput)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Abrir caja")
            .setMessage("No hay caja abierta. Abre la caja y se reintentará la venta.")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Abrir y reintentar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val monto = Money.parseMinorUnits(amountInput.text.toString())
                if (monto == null || monto < 0) {
                    Toast.makeText(context, R.string.cash_error_monto_invalid, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                confirmButton.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val db = DatabaseProvider.get(requireContext())
                        CashRepository(db).openSession(
                            companyId = ActiveCompanyStore.get(requireContext()),
                            userId = UserSessionStore.userId(requireContext()),
                            montoApertura = monto
                        )
                        dialog.dismiss()
                        chargeSale()
                    } catch (error: Exception) {
                        confirmButton.isEnabled = true
                        Toast.makeText(context, error.message ?: "No se pudo abrir la caja", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }
    private fun identifyCreditClient(rawCode: String) {
        val payload = QrCredentialService.parsePayload(rawCode)
        val companyId = ActiveCompanyStore.get(requireContext())
        if (payload == null) {
            Toast.makeText(requireContext(), "La credencial no es válida", Toast.LENGTH_LONG).show()
            return
        }
        if (payload.companyId != companyId) {
            Toast.makeText(requireContext(), "La credencial pertenece a otra empresa", Toast.LENGTH_LONG).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val db = DatabaseProvider.get(requireContext())
            val credential = db.credencialClienteDao().getForCredentialId(companyId, payload.credentialId)
            val client = credential?.let { db.clienteDao().getByIdIncludingInactive(it.clienteId, companyId) }
            if (credential == null || client == null || !client.activo || !client.creditoHabilitado || client.estadoCredito != Cliente.ESTADO_ACTIVO) {
                Toast.makeText(requireContext(), "La credencial está revocada o el crédito no está activo", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (credential.pinHash == null) {
                Toast.makeText(requireContext(), "La credencial requiere un PIN. Emita una credencial nueva desde Clientes.", Toast.LENGTH_LONG).show()
                return@launch
            }
            val now = System.currentTimeMillis()
            if (credential.fechaVencimiento != null && now > credential.fechaVencimiento) {
                Toast.makeText(requireContext(), "Credencial vencida", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (credential.bloqueadaHasta != null && now < credential.bloqueadaHasta) {
                val remainingMin = ((credential.bloqueadaHasta - now) / 60000) + 1
                Toast.makeText(requireContext(), "Credencial bloqueada. Intenta en $remainingMin minutos", Toast.LENGTH_LONG).show()
                return@launch
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Confirmar cliente")
                .setMessage("${client.nombre}\nDocumento: ${maskDocument(client.documento)}\nLímite: ${Money.format(client.limiteCredito)}\nSaldo: ${Money.format(client.creditoActual)}\nDisponible: ${Money.format(client.creditoDisponible)}")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Usar para esta venta") { _, _ ->
                    showPinDialog(client, payload.credentialId)
                }.show()
        }
    }

    private fun showPinDialog(client: Cliente, credentialId: String) {
        val pinInput = android.widget.EditText(requireContext()).apply {
            hint = "PIN de 4 dígitos"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(24, 12, 24, 12)
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Ingrese el PIN de la credencial")
            .setMessage("Debe ingresar el PIN de 4 dígitos asociado a la credencial del cliente.")
            .setView(pinInput)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = pinInput.text.toString().trim()
                if (pin.length != 4 || !pin.all { it.isDigit() }) {
                    Toast.makeText(requireContext(), "El PIN debe ser de exactamente 4 dígitos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                selectedCreditClient = client
                scannedCredentialId = credentialId
                scannedPin = pin
                binding.txtCreditClient.text = getString(R.string.credit_client_format, client.nombre, Money.format(client.creditoDisponible))
                dialog.dismiss()
                val pinChars = pin.toCharArray()
                pinChars.fill('\u0000')
            }
        }
        dialog.show()
    }

    private fun clearCreditClient() {
        selectedCreditClient = null
        scannedCredentialId = null
        scannedPin = null
        if (_binding != null) binding.txtCreditClient.text = getString(R.string.client_not_identified)
    }

    private fun maskDocument(document: String): String = if (document.length <= 4) "****" else "*".repeat(document.length - 4) + document.takeLast(4)

    private enum class ScannerTarget { PRODUCT, CLIENT_CREDENTIAL }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
