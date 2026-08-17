package com.multipos.app.ui.clients

import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.withTransaction
import com.google.android.material.materialswitch.MaterialSwitch
import com.multipos.app.adapters.ClientAdapter
import com.multipos.app.data.ActiveCompanyStore
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.CredentialRepository
import com.multipos.app.data.CreditRepository
import com.multipos.app.data.DatabaseProvider
import com.multipos.app.data.RegisterAbonoRequest
import com.multipos.app.data.UserSessionStore
import com.multipos.app.data.entities.Abono
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.CredencialCliente
import com.multipos.app.databinding.FragmentClientsBinding
import com.multipos.app.security.ActiveCompanyAccess
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.QrCredentialService
import com.multipos.app.util.Money
import com.multipos.app.util.ReceiptPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientsFragment : Fragment() {

    class InvalidCreditLimitException(message: String) : Exception(message)
    class ClientNotFoundException : Exception()

    private var _binding: FragmentClientsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClientsBinding.inflate(inflater, container, false)
        val db = DatabaseProvider.get(requireContext())
        val companyId = ActiveCompanyStore.get(requireContext())
        binding.btnAddClient.isEnabled = false
        binding.btnAddPayment.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            if (!ActiveCompanyAccess.allows(requireContext(), db, CompanyPermission.MANAGE_CLIENT_CREDIT)) {
                Toast.makeText(requireContext(), "No tienes permiso para administrar créditos", Toast.LENGTH_LONG).show()
                return@launch
            }
            val adapter = ClientAdapter(
                onClick = { showClientDialog(it, db) },
                onStatement = { openStatement(it.id) }
            )
            binding.recyclerClients.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerClients.adapter = adapter
            binding.btnAddClient.isEnabled = true
            binding.btnAddPayment.isEnabled = true
            binding.btnAddClient.setOnClickListener { showClientDialog(null, db) }
            binding.btnAddPayment.setOnClickListener { showPaymentDialog(db) }
            db.clienteDao().getAll(companyId).collect { adapter.submitList(it) }
        }
        return binding.root
    }

    private fun openStatement(clientId: Int) {
        parentFragmentManager.beginTransaction()
            .replace(com.multipos.app.R.id.homeContainer, EstadoCuentaFragment.newInstance(clientId, ActiveCompanyStore.get(requireContext())))
            .addToBackStack(null)
            .commit()
    }

    private fun showPaymentDialog(db: AppDatabase) {
        viewLifecycleOwner.lifecycleScope.launch {
            val companyId = ActiveCompanyStore.get(requireContext())
            val clients = db.clienteDao().getAllOnce(companyId).filter { it.creditoActual > 0 }
            if (clients.isEmpty()) {
                Toast.makeText(requireContext(), "No hay clientes con saldo pendiente", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val clientSpinner = Spinner(requireContext()).apply {
                adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, clients.map { "${it.nombre} · Saldo ${Money.format(it.creditoActual)}" })
            }
            val paymentSpinner = Spinner(requireContext()).apply {
                adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf(Abono.MEDIO_EFECTIVO, Abono.MEDIO_TARJETA, Abono.MEDIO_TRANSFERENCIA))
            }
            val paymentCheckbox = android.widget.CheckBox(requireContext()).apply {
                text = requireContext().getString(com.multipos.app.R.string.confirm_external_payment)
                isChecked = false
            }
            paymentSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    paymentCheckbox.visibility = if (position == 0) View.GONE else View.VISIBLE
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
            paymentCheckbox.visibility = View.GONE
            val amount = EditText(requireContext()).apply {
                hint = "Monto del abono"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setPadding(24, 12, 24, 12)
            }
            val form = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; addView(clientSpinner); addView(paymentSpinner); addView(paymentCheckbox); addView(amount) }
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Registrar abono")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val value = Money.parseMinorUnits(amount.text.toString())
                    if (value == null || value <= 0) {
                        Toast.makeText(requireContext(), "Indica un monto válido", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val client = clients[clientSpinner.selectedItemPosition]
                    val medioPago = when (paymentSpinner.selectedItemPosition) {
                        1 -> Abono.MEDIO_TARJETA
                        2 -> Abono.MEDIO_TRANSFERENCIA
                        else -> Abono.MEDIO_EFECTIVO
                    }
                    val externalConfirmed = paymentCheckbox.isChecked
                    if (medioPago != Abono.MEDIO_EFECTIVO && !externalConfirmed) {
                        Toast.makeText(requireContext(), "Confirma el cobro externo para continuar", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val result = CreditRepository(db).registerAbono(
                                RegisterAbonoRequest(
                                    companyId = companyId,
                                    clientId = client.id,
                                    userId = UserSessionStore.userId(requireContext()),
                                    monto = value,
                                    medioPago = medioPago,
                                    nota = "Abono de crédito",
                                    externalPaymentConfirmed = externalConfirmed
                                )
                            )
                            val companyName = db.empresaDao().getById(companyId)?.nombre ?: "MultiPOS"
                            val receipt = withContext(Dispatchers.IO) {
                                ReceiptPdfGenerator.createPayment(requireContext(), companyName, client.nombre, value, result.saldoAnterior, result.saldoNuevo)
                            }
                            dialog.dismiss()
                            Toast.makeText(requireContext(), "Abono registrado", Toast.LENGTH_SHORT).show()
                            ReceiptPdfGenerator.share(requireContext(), receipt, "Comprobante de abono")
                        } catch (_: Exception) {
                            Toast.makeText(requireContext(), "No se pudo registrar el abono", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            dialog.show()
        }
    }

    private fun showClientDialog(existing: Cliente?, db: AppDatabase) {
        val context = requireContext()
        val inputs = listOf("Nombre", "Documento", "Teléfono", "Límite de crédito").map { EditText(context).apply { hint = it; setPadding(24, 12, 24, 12) } }
        inputs[3].inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        existing?.let {
            inputs[0].setText(it.nombre); inputs[1].setText(it.documento); inputs[2].setText(it.telefono); inputs[3].setText(Money.toInput(it.limiteCredito))
        }
        val creditSwitch = MaterialSwitch(context).apply {
            text = context.getString(com.multipos.app.R.string.authorize_credit_qr)
            isChecked = existing?.creditoHabilitado == true && existing.estadoCredito == Cliente.ESTADO_ACTIVO
        }
        val credentialButton = Button(context).apply { text = context.getString(com.multipos.app.R.string.view_qr_credential); visibility = if (creditSwitch.isChecked) View.VISIBLE else View.GONE }
        val regenerateButton = Button(context).apply { text = context.getString(com.multipos.app.R.string.replace_qr_credential); visibility = if (creditSwitch.isChecked) View.VISIBLE else View.GONE }
        creditSwitch.setOnCheckedChangeListener { _, checked ->
            credentialButton.visibility = if (checked && existing != null) View.VISIBLE else View.GONE
            regenerateButton.visibility = if (checked && existing != null) View.VISIBLE else View.GONE
        }
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            inputs.forEach(::addView)
            addView(creditSwitch); addView(credentialButton); addView(regenerateButton)
        }
        val builder = AlertDialog.Builder(context).setTitle(if (existing == null) "Nuevo cliente" else "Editar cliente")
            .setView(form).setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null)
        if (existing != null) builder.setNeutralButton("Eliminar", null)
        val dialog = builder.create()
        credentialButton.setOnClickListener { existing?.let { shareCredential(db, it, replace = false) } }
        regenerateButton.setOnClickListener {
            existing?.let { client ->
                AlertDialog.Builder(context).setTitle("Reemplazar credencial")
                    .setMessage("La credencial anterior quedará invalidada inmediatamente.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Reemplazar") { _, _ -> shareCredential(db, client, replace = true) }
                    .show()
            }
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val validationResult = ClientInputValidator.validate(
                    inputs[0].text.toString(),
                    inputs[1].text.toString()
                )

                val (name, document) = when (validationResult) {
                    is ClientInputValidator.ValidationResult.Success -> validationResult.normalizedName to validationResult.normalizedDocument
                    is ClientInputValidator.ValidationResult.EmptyName -> {
                        Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    is ClientInputValidator.ValidationResult.EmptyDocument -> {
                        Toast.makeText(context, "El documento es obligatorio", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                val limit = Money.parseMinorUnits(inputs[3].text.toString())
                if (limit == null || (creditSwitch.isChecked && limit <= 0)) {
                    Toast.makeText(context, "Asigna un límite mayor a cero para habilitar crédito", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (existing != null && limit < existing.creditoActual) {
                    Toast.makeText(context, "El límite no puede ser menor que el saldo actual (${Money.format(existing.creditoActual)})", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val companyId = existing?.empresaId ?: ActiveCompanyStore.get(context)
                        val approvingUser = UserSessionStore.userId(context)
                        val now = System.currentTimeMillis()
                        var savedClient: Cliente? = null
                        var issueCredentialAfterTransaction = false
                        db.withTransaction {
                            val enabled = creditSwitch.isChecked
                            val state = when {
                                enabled -> Cliente.ESTADO_ACTIVO
                                existing?.estadoCredito == Cliente.ESTADO_ACTIVO -> Cliente.ESTADO_SUSPENDIDO
                                else -> Cliente.ESTADO_NO_SOLICITADO
                            }
                            if (existing == null) {
                                val client = Cliente(
                                    nombre = name,
                                    documento = document,
                                    telefono = inputs[2].text.toString().trim(),
                                    limiteCredito = limit,
                                    creditoHabilitado = enabled,
                                    estadoCredito = state,
                                    fechaAprobacion = if (enabled) now else null,
                                    usuarioAproboId = if (enabled) approvingUser else null,
                                    empresaId = companyId
                                )
                                val clientId = db.clienteDao().insert(client).toInt()
                                savedClient = client.copy(id = clientId)
                            } else {
                                val updatedRows = db.clienteDao().updateWithCreditCheck(
                                    id = existing.id,
                                    empresaId = companyId,
                                    nombre = name,
                                    documento = document,
                                    telefono = inputs[2].text.toString().trim(),
                                    limiteCredito = limit,
                                    creditoHabilitado = enabled,
                                    estadoCredito = state,
                                    fechaAprobacion = if (enabled) (existing.fechaAprobacion ?: now) else existing.fechaAprobacion,
                                    usuarioAproboId = if (enabled) (existing.usuarioAproboId ?: approvingUser) else existing.usuarioAproboId
                                )
                                if (updatedRows == 0) {
                                    if (db.clienteDao().getById(existing.id, companyId) == null) {
                                        throw ClientNotFoundException()
                                    } else {
                                        throw InvalidCreditLimitException("El límite de crédito no puede ser inferior al saldo actual.")
                                    }
                                }
                                savedClient = existing.copy(
                                    nombre = name,
                                    documento = document,
                                    telefono = inputs[2].text.toString().trim(),
                                    limiteCredito = limit,
                                    creditoHabilitado = enabled,
                                    estadoCredito = state
                                )
                            }

                            val clientId = savedClient!!.id
                            val activeCredential = db.credencialClienteDao().getActiveForClient(clientId, companyId)
                            if (enabled && activeCredential == null) {
                                issueCredentialAfterTransaction = true
                            } else if (!enabled && activeCredential != null) {
                                db.credencialClienteDao().revokeActive(clientId, companyId, CredencialCliente.ESTADO_REVOCADA, now)
                            }
                        }
                        dialog.dismiss()
                        Toast.makeText(context, "Cliente guardado", Toast.LENGTH_SHORT).show()
                        if (issueCredentialAfterTransaction) {
                            showPinAndIssueCredential(db, savedClient!!.id, companyId, approvingUser)
                        }
                    } catch (e: Exception) {
                        handleSaveClientError(e)
                    }
                }
            }
            if (existing != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                if (existing.creditoActual > 0) {
                    Toast.makeText(context, "No puedes eliminar un cliente con saldo pendiente", Toast.LENGTH_LONG).show()
                } else AlertDialog.Builder(context).setTitle("Eliminar cliente").setMessage("Esta acción no se puede deshacer.")
                    .setNegativeButton("Cancelar", null).setPositiveButton("Eliminar") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val archived = db.withTransaction {
                                val rows = db.clienteDao().archive(existing.id, existing.empresaId)
                                if (rows == 1) {
                                    db.credencialClienteDao().revokeActive(
                                        existing.id,
                                        existing.empresaId,
                                        CredencialCliente.ESTADO_REVOCADA,
                                        System.currentTimeMillis()
                                    )
                                }
                                rows
                            }
                            if (archived == 1) {
                                dialog.dismiss()
                            } else {
                                Toast.makeText(context, "No puedes eliminar un cliente con saldo pendiente", Toast.LENGTH_LONG).show()
                            }
                        }
                    }.show()
            }
        }
        dialog.show()
    }

    private fun handleSaveClientError(e: Exception) {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is SQLiteConstraintException) {
                val message = cause.message.orEmpty()
                if (message.contains("index_clientes_empresaId_documento") || message.contains("clientes.empresaId, clientes.documento")) {
                    Toast.makeText(context, "El documento ya existe para otro cliente.", Toast.LENGTH_LONG).show()
                    return
                }
            }
            if (cause is InvalidCreditLimitException) {
                Toast.makeText(context, cause.message, Toast.LENGTH_LONG).show()
                return
            }
            if (cause is ClientNotFoundException) {
                Toast.makeText(context, "El cliente ya no existe.", Toast.LENGTH_LONG).show()
                return
            }
            cause = cause.cause
        }
        Toast.makeText(context, "No se pudo guardar el cliente.", Toast.LENGTH_LONG).show()
    }

    private fun shareCredential(db: AppDatabase, client: Cliente, replace: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val companyId = client.empresaId
            val credential = db.credencialClienteDao().getActiveForClient(client.id, companyId)
            if (replace) {
                if (credential == null) {
                    Toast.makeText(requireContext(), "El cliente no tiene una credencial activa", Toast.LENGTH_LONG).show()
                    return@launch
                }
                showPinForReplace(db, client, credential)
            } else {
                if (credential == null) Toast.makeText(requireContext(), "El cliente no tiene una credencial activa", Toast.LENGTH_LONG).show()
                else generateAndShareCredential(db, client, credential)
            }
        }
    }

    private fun showPinForReplace(db: AppDatabase, client: Cliente, credential: CredencialCliente) {
        val pinInput = android.widget.EditText(requireContext()).apply {
            hint = "PIN de 4 dígitos"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(24, 12, 24, 12)
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Reemplazar credencial")
            .setMessage("Ingresa el PIN de 4 dígitos para la nueva credencial. La credencial anterior quedará invalidada inmediatamente.")
            .setView(pinInput)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Reemplazar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = pinInput.text.toString().trim()
                if (pin.length != 4 || !pin.all { it.isDigit() }) {
                    Toast.makeText(requireContext(), "El PIN debe ser de exactamente 4 dígitos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                viewLifecycleOwner.lifecycleScope.launch {
                    val credRepo = CredentialRepository(db)
                    try {
                        val newCredential = credRepo.issueCredential(
                            companyId = client.empresaId,
                            clientId = client.id,
                            pin = pin,
                            issuedBy = UserSessionStore.userId(requireContext())
                        )
                        generateAndShareCredential(db, client, newCredential)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "No se pudo reemplazar la credencial", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showPinAndIssueCredential(db: AppDatabase, clientId: Int, companyId: String, approvingUser: Int) {
        val pinInput = android.widget.EditText(requireContext()).apply {
            hint = "PIN de 4 dígitos"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(24, 12, 24, 12)
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Emitir credencial")
            .setMessage("La credencial vence en 365 días. Ingresa un PIN de 4 dígitos que el cliente usará para autorizar compras a crédito.")
            .setView(pinInput)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Emitir", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = pinInput.text.toString().trim()
                if (pin.length != 4 || !pin.all { it.isDigit() }) {
                    Toast.makeText(requireContext(), "El PIN debe ser de exactamente 4 dígitos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                val pinChars = pin.toCharArray()
                viewLifecycleOwner.lifecycleScope.launch {
                    val credRepo = CredentialRepository(db)
                    try {
                        val client = db.clienteDao().getById(clientId, companyId) ?: return@launch
                        val newCredential = credRepo.issueCredential(
                            companyId = companyId,
                            clientId = clientId,
                            pin = pin,
                            issuedBy = approvingUser
                        )
                        pinChars.fill('\u0000')
                        generateAndShareCredential(db, checkNotNull(client), newCredential)
                    } catch (e: Exception) {
                        pinChars.fill('\u0000')
                        Toast.makeText(requireContext(), "No se pudo emitir la credencial", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private suspend fun generateAndShareCredential(db: AppDatabase, client: Cliente, credential: CredencialCliente) {
        try {
            val company = db.empresaDao().getById(client.empresaId)?.nombre ?: "MultiPOS"
            val payload = QrCredentialService.buildPayload(client.empresaId, credential.credentialId)
            val document = maskDocument(client.documento)
            val file = withContext(Dispatchers.IO) { ReceiptPdfGenerator.createCreditCredential(requireContext(), company, client.nombre, document, payload) }
            ReceiptPdfGenerator.share(requireContext(), file, "Credencial privada de crédito")
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "La autorización se guardó, pero no se pudo generar la credencial", Toast.LENGTH_LONG).show()
        }
    }

    private fun maskDocument(document: String): String = if (document.length <= 4) "****" else "*".repeat(document.length - 4) + document.takeLast(4)

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
