package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemProfitability
import com.example.data.model.PnlTimeframe
import com.example.ui.components.CategoryBadge
import com.example.ui.components.MetricStatCard
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.BarStockViewModel
import java.util.Locale

@Composable
fun ProfitAndLossScreen(
    viewModel: BarStockViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedTimeframe by viewModel.selectedPnlTimeframe.collectAsState()
    val pnlReport by viewModel.pnlReport.collectAsState()

    LaunchedEffect(selectedTimeframe) {
        viewModel.loadProfitAndLossReport(selectedTimeframe)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Timeframe Selector Tabs
            TabRow(
                selectedTabIndex = selectedTimeframe.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                PnlTimeframe.values().forEach { tf ->
                    val label = when (tf) {
                        PnlTimeframe.THIS_WEEK -> "This Week"
                        PnlTimeframe.THIS_MONTH -> "This Month"
                        PnlTimeframe.THIS_YEAR -> "This Year"
                        PnlTimeframe.ALL_TIME -> "All Time"
                    }
                    Tab(
                        selected = selectedTimeframe == tf,
                        onClick = { viewModel.loadProfitAndLossReport(tf) },
                        text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val report = pnlReport
            if (report == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmberPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Loss Alert Banner
                    if (report.lossMakingItems.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = CrimsonRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Loss Alert (${report.lossMakingItems.size} items selling below cost)",
                                            fontWeight = FontWeight.Bold,
                                            color = CrimsonRed
                                        )
                                        Text(
                                            text = "Weighted cost exceeds selling price for: ${report.lossMakingItems.joinToString { it.itemName }}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Estimated Cost Alert Banner
                    if (report.itemsWithEstimatedCost.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AmberPrimary.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Estimated Cost (${report.itemsWithEstimatedCost.size} items have no purchase receipt yet)",
                                            fontWeight = FontWeight.Bold,
                                            color = AmberPrimary
                                        )
                                        Text(
                                            text = "Using the manual case price as a guess, not an actual purchase, for: ${report.itemsWithEstimatedCost.joinToString { it.itemName }}. Record a purchase receipt (or opening stock) for accurate profit.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Executive Financial Summary
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Executive Financial Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricStatCard(
                                    title = "Sales Revenue",
                                    value = formatCurrency(report.salesRevenue),
                                    icon = Icons.Default.TrendingUp,
                                    iconColor = EmeraldGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatCard(
                                    title = "COGS (Weighted Cost)",
                                    value = formatCurrency(report.costOfGoodsSold),
                                    icon = Icons.Default.AttachMoney,
                                    iconColor = AmberPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricStatCard(
                                    title = "Gross Profit",
                                    value = formatCurrency(report.grossProfit),
                                    icon = Icons.Default.PieChart,
                                    iconColor = if (report.grossProfit >= 0) EmeraldGreen else CrimsonRed,
                                    subtitle = "Margin: ${String.format(Locale.US, "%.1f%%", report.grossMarginPct)}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatCard(
                                    title = "Operating Expenses",
                                    value = formatCurrency(report.operatingExpenses),
                                    icon = Icons.Default.TrendingDown,
                                    iconColor = CrimsonRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Net Profit Card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (report.netProfit >= 0) Color(0xFF064E3B) else Color(0xFF450A0A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "NET PROFIT / (LOSS)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = formatCurrency(report.netProfit),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (report.netProfit >= 0) EmeraldGreen else CrimsonRed
                                    )
                                    Text(
                                        text = "Gross Profit (${formatCurrency(report.grossProfit)}) - Expenses (${formatCurrency(report.operatingExpenses)})",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // Stock Movement & Valuation Summary
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory, contentDescription = null, tint = AmberPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Stock Valuation & Movements",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Divider(modifier = Modifier.padding(vertical = 12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Opening Stock", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${report.openingStockQty} units", fontWeight = FontWeight.Bold)
                                        Text(formatCurrency(report.openingStockValue), fontSize = 12.sp, color = AmberPrimary)
                                    }
                                    Column {
                                        Text("Closing Stock", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${report.closingStockQty} units", fontWeight = FontWeight.Bold)
                                        Text(formatCurrency(report.closingStockValue), fontSize = 12.sp, color = EmeraldGreen)
                                    }
                                }
                            }
                        }
                    }

                    // Item Level Profitability breakdown
                    item {
                        Text(
                            text = "Item Profitability Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (report.itemProfitabilities.isEmpty()) {
                        item {
                            Text(
                                text = "No item sales or stock records found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(report.itemProfitabilities, key = { it.itemId }) { item ->
                            ItemProfitabilityCard(item = item)
                        }
                    }
                }
            }
        }
    }

@Composable
fun ItemProfitabilityCard(item: ItemProfitability) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isLossMaking) CrimsonRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CategoryBadge(category = item.category)
                    if (item.costIsEstimated) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = AmberPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Est. cost",
                                color = AmberPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (item.isLossMaking) {
                    Surface(
                        color = CrimsonRed,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LOSS MAKING",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Avg Buying Cost", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(item.weightedAvgCost), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column {
                    Text("Selling Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(item.sellingPrice), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column {
                    Text("Profit / Unit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(item.profitPerUnit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (item.profitPerUnit >= 0) EmeraldGreen else CrimsonRed
                    )
                }
                Column {
                    Text("Margin %", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        String.format(Locale.US, "%.1f%%", item.profitMarginPct),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (item.profitMarginPct >= 0) EmeraldGreen else CrimsonRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Units Sold: ${item.unitsSold}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total Gross Profit: ${formatCurrency(item.grossProfit)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.grossProfit >= 0) EmeraldGreen else CrimsonRed
                )
            }
        }
    }
}
