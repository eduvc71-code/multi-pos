package com.multipos.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.ui.theme.premiumBorder

/**
 * Card personalizada MultiPOS - Estilo Fintech Executive (Plano 3D)
 */
@Composable
fun MultiPOSCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.premiumBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(content = content)
    }
}

@Preview(showBackground = true)
@Composable
fun MultiPOSCardPreview() {
    MultiPOSTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            MultiPOSCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Contenido dentro de la tarjeta Premium",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
