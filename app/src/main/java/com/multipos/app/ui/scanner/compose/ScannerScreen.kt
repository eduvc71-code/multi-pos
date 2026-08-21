package com.multipos.app.ui.scanner.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.multipos.app.R
import com.multipos.app.ui.components.CameraScanner
import com.multipos.app.ui.theme.MultiPOSTheme

@Composable
fun ScannerScreen(
    title: String,
    onResult: (String, Int) -> Unit,
    onClose: () -> Unit,
    manualEntryAllowed: Boolean = true
) {
    var torchEnabled by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CameraScanner(
            modifier = Modifier.fillMaxSize(),
            torchEnabled = torchEnabled,
            onResult = onResult
        )

        // Frame
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center)
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
        )

        // Top Controls
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
                Text(text = title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { torchEnabled = !torchEnabled }) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null,
                        tint = if (torchEnabled) Color.Yellow else Color.White
                    )
                }
            }
        }

        // Bottom Controls
        if (manualEntryAllowed) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.scanner_manual_code), color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                    Button(
                        onClick = { onResult(manualCode, -1) },
                        enabled = manualCode.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.scanner_manual_use))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ScannerScreenPreview() {
    MultiPOSTheme {
        ScannerScreen(
            title = "Escanear Producto",
            onResult = { _, _ -> },
            onClose = {},
            manualEntryAllowed = true
        )
    }
}
