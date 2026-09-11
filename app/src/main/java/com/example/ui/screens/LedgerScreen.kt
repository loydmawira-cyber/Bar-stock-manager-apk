package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Reconciliation
import com.example.data.model.ReconciliationType
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.MetricStatCard
import com.example.ui.components.ReconciliationBadge
import com.example.ui.components.LocalCurrencySymbol
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen

@Composable
fun LedgerScreen(
    currentUser: User,
    reconciliations: List<Reconciliation>,
    attendants: List<User>,
    selectedAttendantId: Long?,
    onSelectAttendantFilter: (Long?) -> Unit
) {
    val currency = LocalCurrencySymbol.current
    var showFilterDropdown by remember { mutableStateOf(false) }

    // If attendant is viewing, restrict to their own records automatically
    val effectiveFilterId = if (currentUser.role == UserRole.ATTENDANT) currentUser.id else selectedAttendantId

    val filteredList = reconciliations.filter {
        effectiveFilterId == null || it.attendantId == effectiveFilterId
    }

    val totalExpected = filteredList.sumOf { it.expectedTotal }
    val totalSubmitted = filteredList.sumOf { it.submittedTotal }
    val totalLosses = filteredList.filter { it.variance < 0.0 }.sumOf { Math.abs(it.variance) }
    val totalExtras = filteredList.filter { it.variance > 0.0 }.sumOf { it.variance }
    val netVariance = totalSubmitted - totalExpected

    val selectedAttendantName = attendants.firstOrNull { it.id == effectiveFilterId }?.name ?: "All Attendants"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Filter Header Bar (only if Admin)
        if (currentUser.role == UserRole.ADMIN) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MONTHLY LOSSES & EXTRAS LEDGER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary,
                        letterSpacing = 1.sp
                    )

                    Box {
                        OutlinedButton(
                            onClick = { showFilterDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("filter_attendant_button")
                        ) {
                            Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(selectedAttendantName, fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = showFilterDropdown,
                            onDismissRequest = { showFilterDropdown = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Attendants", fontWeight = if (selectedAttendantId == null) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onSelectAttendantFilter(null)
                                    showFilterDropdown = false
                                }
                            )

                            attendants.forEach { att ->
                                DropdownMenuItem(
                                    text = { Text(att.name, fontWeight = if (selectedAttendantId == att.id) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        onSelectAttendantFilter(att.id)
                                        showFilterDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Total Shortage / Losses",
                    value = formatCurrency(totalLosses),
                    icon = Icons.Filled.ArrowDownward,
                    iconColor = if (totalLosses > 0.0) CrimsonRed else EmeraldGreen,
                    subtitle = "Cash deficits",
                    modifier = Modifier.weight(1f)
                )

                MetricStatCard(
                    title = "Total Surplus / Extras",
                    value = formatCurrency(totalExtras),
                    icon = Icons.Filled.ArrowUpward,
                    iconColor = EmeraldGreen,
                    subtitle = "Cash overages",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Expected Sales", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalExpected), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Cash Received", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalSubmitted), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (netVariance < 0.0) "-${formatCurrency(Math.abs(netVariance))}" else "+${formatCurrency(netVariance)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (netVariance < 0.0) CrimsonRed else EmeraldGreen
                        )
                    }
                }
            }
        }

        // Itemized Reconciliations List
        item {
            Text(
                text = "SHIFT-BY-SHIFT SETTLEMENT HISTORY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No closed shift reconciliations found for this period.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredList) { recon ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Shift #${recon.shiftId} · ${recon.attendantName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatDateTime(recon.date),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            ReconciliationBadge(type = recon.type, variance = recon.variance)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Expected: ${formatCurrency(recon.expectedTotal)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Handover Cash: ${formatCurrency(recon.submittedTotal)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        if (recon.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Note: ${recon.notes}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
