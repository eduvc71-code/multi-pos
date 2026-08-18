package com.multipos.app.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.multipos.app.R
import com.multipos.app.ui.scanner.compose.ScannerScreen
import com.multipos.app.ui.theme.MultiPOSTheme
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScannerActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val analyzing = AtomicBoolean(false)
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var barcodeScanner: BarcodeScanner? = null
    
    private var torchEnabled by mutableStateOf(false)
    private var statusText by mutableStateOf("Iniciando cámara...")
    private var manualEntryAllowed = true
    private var previewView: PreviewView? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            statusText = if (manualEntryAllowed) "Sin permiso de cámara. Use entrada manual." else "Sin permiso de cámara."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Escanear Código"
        manualEntryAllowed = intent.getBooleanExtra(EXTRA_ALLOW_MANUAL, true)

        setContent {
            MultiPOSTheme {
                ScannerScreen(
                    title = title,
                    statusText = statusText,
                    manualEntryAllowed = manualEntryAllowed,
                    torchEnabled = torchEnabled,
                    onCloseClick = { finish() },
                    onTorchToggle = { toggleTorch() },
                    onManualEntry = { returnResult(it, "MANUAL") },
                    onPreviewCreated = { 
                        previewView = it
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            startCamera()
                        } else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }

    private fun toggleTorch() {
        val activeCamera = camera ?: return
        if (!activeCamera.cameraInfo.hasFlashUnit()) return

        val newState = !torchEnabled
        activeCamera.cameraControl.enableTorch(newState).addListener({
            torchEnabled = newState
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
    private fun startCamera() {
        val view = previewView ?: return
        val providerFuture = ProcessCameraProvider.getInstance(this)
        
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            ).build()
            
            val scanner = BarcodeScanning.getClient(options)
            barcodeScanner = scanner
            
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            
            analysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null || !analyzing.compareAndSet(false, true)) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.rawValue?.let { code ->
                            returnResult(code, barcodes.first().format.toString())
                        }
                    }
                    .addOnCompleteListener {
                        analyzing.set(false)
                        imageProxy.close()
                    }
            }

            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                statusText = "Coloque el código en el recuadro"
            } catch (e: Exception) {
                statusText = "Error al iniciar cámara"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun returnResult(code: String, format: String) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_SCAN_RESULT, code).putExtra(EXTRA_SCAN_FORMAT, format))
        finish()
    }

    override fun onDestroy() {
        executor.shutdown()
        barcodeScanner?.close()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TITLE = "scanner_title"
        const val EXTRA_SCAN_RESULT = "scan_result"
        const val EXTRA_SCAN_FORMAT = "scan_format"
        const val EXTRA_ALLOW_MANUAL = "allow_manual"
    }
}
