package com.multipos.app.ui.pos.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.util.Money

@Composable
fun SaleSuccessDialog(folio: String, total: Long, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        MultiPOSCard(elevation = 8.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Text(
                    "¡VENTA EXITOSA!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Folio: $folio", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        Money.format(total),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                HorizontalDivider()
                MultiPOSButton(text = "Nueva Venta", onClick = onDismiss)
            }
        }
    }
}
