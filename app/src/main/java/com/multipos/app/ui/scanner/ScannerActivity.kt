package com.multipos.app.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.multipos.app.R
import com.multipos.app.databinding.ActivityScannerBinding
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannerBinding
    private val executor = Executors.newSingleThreadExecutor()
    private val analyzing = AtomicBoolean(false)
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var barcodeScanner: BarcodeScanner? = null
    private var torchEnabled = false
    private var manualEntryAllowed = true

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            resetCameraState()
            binding.txtScannerStatus.setText(
                if (manualEntryAllowed) R.string.scanner_permission_denied_manual else R.string.scanner_permission_denied
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtScannerTitle.text = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.scanner_title)
        manualEntryAllowed = intent.getBooleanExtra(EXTRA_ALLOW_MANUAL, true)
        binding.manualEntryContainer.visibility = if (manualEntryAllowed) View.VISIBLE else View.GONE
        binding.btnCloseScanner.setOnClickListener { finish() }
        binding.btnTorch.setOnClickListener {
            val activeCamera = camera ?: return@setOnClickListener
            if (!activeCamera.cameraInfo.hasFlashUnit()) return@setOnClickListener

            val requestedState = !torchEnabled
            val torchFuture = runCatching {
                activeCamera.cameraControl.enableTorch(requestedState)
            }.getOrElse { return@setOnClickListener }
            torchFuture.addListener({
                if (isDestroyed) return@addListener
                runCatching { torchFuture.get() }
                    .onSuccess {
                        torchEnabled = requestedState
                        binding.btnTorch.setText(if (torchEnabled) R.string.scanner_torch_off else R.string.scanner_torch)
                    }
            }, ContextCompat.getMainExecutor(this))
        }
        binding.btnUseManualCode.setOnClickListener {
            val code = binding.etManualCode.text?.toString().orEmpty().trim()
            if (code.isNotEmpty()) returnResult(code, "MANUAL")
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
    private fun startCamera() {
        val providerFuture = runCatching { ProcessCameraProvider.getInstance(this) }.getOrElse {
            showCameraStartError()
            return
        }
        providerFuture.addListener({
            if (isFinishing || isDestroyed) return@addListener

            val provider = runCatching { providerFuture.get() }.getOrElse {
                showCameraStartError()
                return@addListener
            }
            cameraProvider = provider
            val hasBackCamera = runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }
                .getOrDefault(false)
            if (!hasBackCamera) {
                resetCameraState()
                showCameraStartError()
                return@addListener
            }

            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_QR_CODE
            ).build()
            val scanner = BarcodeScanning.getClient(options)
            barcodeScanner = scanner
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null || !analyzing.compareAndSet(false, true)) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                try {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            barcodes.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf(String::isNotEmpty) }?.let { code ->
                                val format = barcodes.firstOrNull { it.rawValue?.trim() == code }?.format?.toString() ?: "UNKNOWN"
                                returnResult(code, format)
                            }
                        }
                        .addOnCompleteListener {
                            analyzing.set(false)
                            imageProxy.close()
                        }
                } catch (_: RuntimeException) {
                    analyzing.set(false)
                    imageProxy.close()
                }
            }
            val boundCamera = runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }.getOrElse {
                analysis.clearAnalyzer()
                resetCameraState()
                showCameraStartError()
                return@addListener
            }
            camera = boundCamera
            binding.txtScannerStatus.setText(R.string.scanner_instruction)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showCameraStartError() {
        binding.txtScannerStatus.setText(
            if (manualEntryAllowed) R.string.scanner_start_error_manual else R.string.scanner_start_error
        )
    }

    private fun resetCameraState() {
        analyzing.set(false)
        torchEnabled = false
        camera = null
        runCatching { cameraProvider?.unbindAll() }
        cameraProvider = null
        barcodeScanner?.close()
        barcodeScanner = null
        binding.btnTorch.setText(R.string.scanner_torch)
    }

    private fun returnResult(code: String, format: String) {
        if (isFinishing) return
        setResult(RESULT_OK, Intent().putExtra(EXTRA_SCAN_RESULT, code).putExtra(EXTRA_SCAN_FORMAT, format))
        finish()
    }

    override fun onDestroy() {
        resetCameraState()
        executor.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TITLE = "scanner_title"
        const val EXTRA_SCAN_RESULT = "scan_result"
        const val EXTRA_SCAN_FORMAT = "scan_format"
        const val EXTRA_ALLOW_MANUAL = "allow_manual"
    }
}
