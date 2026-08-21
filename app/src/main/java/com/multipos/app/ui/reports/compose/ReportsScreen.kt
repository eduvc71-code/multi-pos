package com.multipos.app.ui.reports.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multipos.app.data.ReportData
import com.multipos.app.data.ReportRow
import com.multipos.app.data.ReportSummary
import androidx.compose.ui.res.stringResource
import com.multipos.app.R
import com.multipos.app.ui.components.MultiPOSCard
import com.multipos.app.ui.components.MultiPOSButton
import com.multipos.app.ui.theme.MultiPOSTheme
import com.multipos.app.util.Money

@Composable
fun ReportsScreen(
    reportType: String,
    reportData: ReportData?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onGenerateReport: () -> Unit,
    onExportCsv: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Executive
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.reports_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            if (reportData != null) {
                IconButton(onClick = onExportCsv) {
                    Icon(Icons.Default.Assessment, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Control Card
        MultiPOSCard(elevation = 2.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.reports_selected_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = reportType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                MultiPOSButton(
                    text = stringResource(R.string.reports_generate_button),
                    onClick = onGenerateReport,
                    showLoading = isLoading
                )
            }
        }

        if (reportData != null) {
            // Hero Metrics
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ReportMetricSmall(label = stringResource(R.string.report_sales_label), value = Money.format(reportData.totalVentas), modifier = Modifier.weight(1f))
                ReportMetricSmall(label = stringResource(R.string.report_profit_label), value = Money.format(reportData.totalGanancia), modifier = Modifier.weight(1f))
            }

            // Results Container
            MultiPOSCard(modifier = Modifier.weight(1f), elevation = 1.dp) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(reportData.rows, key = { it.id }) { row ->
                        ReportRowPremium(row = row)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        } else if (!isLoading) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.reports_start_query), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ReportMetricSmall(label: String, value: String, modifier: Modifier) {
    MultiPOSCard(modifier = modifier, elevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ReportRowPremium(row: ReportRow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.reports_row_quantity_prefix, row.cantidad), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = Money.formatPlain(row.total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ReportsScreenPreview() {
    val dummyRows = listOf(
        ReportRow(1, "2026-08-16", "Ventas", "Venta Hoy", 1250500, "Resumen", 15, 1250500)
    )
    val dummyData = ReportData(
        rows = dummyRows,
        summary = ReportSummary(emptyMap()),
        totalVentas = 1250500L,
        totalGanancia = 350000L,
        totalVentasCount = 15,
        rentabilidad = 28.0
    )
    MultiPOSTheme {
        ReportsScreen(
            reportType = "Ventas por Día",
            reportData = dummyData,
            isLoading = false,
            modifier = Modifier,
            onGenerateReport = {},
            onExportCsv = {}
        )
    }
}
