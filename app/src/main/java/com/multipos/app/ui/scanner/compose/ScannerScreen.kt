package com.multipos.app.ui.scanner.compose

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ScannerScreen(
    title: String,
    statusText: String,
    manualEntryAllowed: Boolean,
    torchEnabled: Boolean,
    onCloseClick: () -> Unit,
    onTorchToggle: () -> Unit,
    onManualEntry: (String) -> Unit,
    onPreviewCreated: (PreviewView) -> Unit
) {
    var manualCode by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    onPreviewCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Frame
        Box(
            modifier = Modifier
                .size(width = 280.dp, height = 220.dp)
                .align(Alignment.Center)
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        )

        // Top Bar
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
                Text(text = title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onTorchToggle) {
                    Icon(
                        Icons.Default.FlashOn, 
                        contentDescription = "Flash", 
                        tint = if (torchEnabled) Color.Yellow else Color.White
                    )
                }
            }
        }

        // Bottom Controls
        Surface(
            color = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = statusText, color = Color.White, style = MaterialTheme.typography.bodySmall)
                
                if (manualEntryAllowed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualCode,
                            onValueChange = { manualCode = it },
                            placeholder = { Text("Ingresar código manual", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )
                        Button(
                            onClick = { onManualEntry(manualCode) },
                            enabled = manualCode.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("USAR")
                        }
                    }
                }
            }
        }
    }
}
