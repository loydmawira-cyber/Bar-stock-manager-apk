package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonthlyReport
import com.example.ui.components.LocalCurrencySymbol
import com.example.ui.components.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonthlyReportsScreen(
    reports: List<MonthlyReport>,
    loading: Boolean,
    onLoad: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currency = LocalCurrencySymbol.current
    LaunchedEffect(Unit) { onLoad() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Column {
                Text("Monthly Reports", style = MaterialTheme.typography.titleLarge)
                Text("Owner only • downloadable PDF and CSV reports", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (loading) {
            Text("Loading reports…", modifier = Modifier.padding(20.dp))
        } else if (reports.isEmpty()) {
            Text("No generated monthly reports are available yet.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(reports) { report ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(report.period, style = MaterialTheme.typography.titleMedium)
                                Text(report.status, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("Generated: ${if (report.generatedAt > 0) SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.generatedAt)) else "Pending"}", fontSize = 12.sp)
                            Text("Variance: ${formatCurrency(report.variance)}  •  Salary deduction review: ${formatCurrency(report.salaryDeductionReview)}", fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (report.pdfUrl.isNotBlank()) {
                                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(report.pdfUrl))) }) {
                                        Icon(Icons.Filled.Download, contentDescription = null)
                                        Text(" PDF")
                                    }
                                }
                                if (report.csvUrl.isNotBlank()) {
                                    OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(report.csvUrl))) }) {
                                        Icon(Icons.Filled.Download, contentDescription = null)
                                        Text(" CSV")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
