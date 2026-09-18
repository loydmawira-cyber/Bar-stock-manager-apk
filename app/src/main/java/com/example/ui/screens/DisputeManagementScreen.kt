package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Dispute
import com.example.data.model.DisputeStatus
import com.example.data.model.Item
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.DisputeBadge
import com.example.ui.components.optionalLabel
import com.example.ui.components.requiredLabel
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen

/** True only for tot-enabled items that have a bottle size; only these are measured in ml. */
fun Item.isMlTracked(): Boolean = totEnabled && bottleVolumeMl > 0

/**
 * Tot-enabled items ([isMlTracked] = true): "3 btl + 250 ml", or "3 btl" when there is no loose ml.
 * Every other item: "3" + [plainSuffix] - ml is never shown.
 */
fun disputeCountLabel(qty: Int, looseMl: Int, isMlTracked: Boolean, plainSuffix: String = ""): String = when {
    isMlTracked && looseMl > 0 -> "$qty btl + $looseMl ml"
    isMlTracked -> "$qty btl"
    else -> "$qty$plainSuffix"
}

/** Total variance in ml between the counted and expected stock (bottle-measured items only). */
private fun disputeVarianceMl(dispute: Dispute, bottleVolumeMl: Int): Int =
    (dispute.reportedQty * bottleVolumeMl + dispute.reportedLooseMl) -
        (dispute.expectedQty * bottleVolumeMl + dispute.expectedLooseMl)

@Composable
fun DisputeManagementScreen(
    currentUser: User,
    disputes: List<Dispute>,
    items: List<Item> = emptyList(),
    onResolveDispute: (disputeId: Long, notes: String, adjustedStockQty: Int?, adjustedLooseMl: Int?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Open Disputes, 1: Resolved Disputes
    var resolvingDispute by remember { mutableStateOf<Dispute?>(null) }
    var resolutionNotesText by remember { mutableStateOf("") }
    var adjustedStockText by remember { mutableStateOf("") }
    var adjustedLooseMlText by remember { mutableStateOf("") }

    val filteredDisputes = disputes.filter {
        if (selectedTab == 0) it.status == DisputeStatus.OPEN else it.status == DisputeStatus.RESOLVED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = AmberPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AmberPrimary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            val openCount = disputes.count { it.status == DisputeStatus.OPEN }
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Open Disputes ($openCount)", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_open_disputes")
            )
            val resolvedCount = disputes.count { it.status == DisputeStatus.RESOLVED }
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Resolved ($resolvedCount)", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_resolved_disputes")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredDisputes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedTab == 0) "No open stock disputes" else "No resolved dispute history",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "All shifts and handover counts are aligned.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDisputes) { dispute ->
                    val disputeItem = items.firstOrNull { it.id == dispute.itemId }
                    val hasVolume = disputeItem?.isMlTracked() == true
                    val bottleVolumeMl = if (hasVolume) disputeItem?.bottleVolumeMl ?: 0 else 0
                    val varianceMl = if (hasVolume) disputeVarianceMl(dispute, bottleVolumeMl) else 0
                    val isDiscrepancyNegative = if (hasVolume) varianceMl < 0 else dispute.discrepancy < 0
                    val varianceText = when {
                        hasVolume && varianceMl == 0 -> "Matched"
                        hasVolume -> {
                            val abs = kotlin.math.abs(varianceMl)
                            val label = if (abs >= bottleVolumeMl) disputeCountLabel(abs / bottleVolumeMl, abs % bottleVolumeMl, true) else "$abs ml"
                            if (varianceMl > 0) "+$label Extra" else "-$label Shortage"
                        }
                        dispute.discrepancy > 0 -> "+${dispute.discrepancy} Extra"
                        dispute.discrepancy == 0 -> "Matched"
                        else -> "${dispute.discrepancy} Shortage"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (dispute.status == DisputeStatus.OPEN) CrimsonRed.copy(alpha = 0.5f) else EmeraldGreen.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Store, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dispute.counterName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                DisputeBadge(status = dispute.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = dispute.itemName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quantity comparison bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("System Expected", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(disputeCountLabel(dispute.expectedQty, dispute.expectedLooseMl, hasVolume, " units"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberPrimary)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Physical Count", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(disputeCountLabel(dispute.reportedQty, dispute.reportedLooseMl, hasVolume, " units"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Variance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = varianceText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDiscrepancyNegative) CrimsonRed else EmeraldGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Attendants involved
                            Text(
                                text = "Raised by: ${dispute.raisedByAttendantName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (dispute.involvesPreviousAttendantName != null) {
                                Text(
                                    text = "Previous Attendant on Counter: ${dispute.involvesPreviousAttendantName}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                            Text(
                                text = "Flagged at: ${formatDateTime(dispute.createdAt)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Resolution details if resolved
                            if (dispute.status == DisputeStatus.RESOLVED) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFF064E3B).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Resolved by ${dispute.resolvedByAdminName ?: "Admin"}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = EmeraldGreen
                                        )
                                        Text(
                                            text = "Notes: ${dispute.resolutionNotes}",
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        if (dispute.adjustedStockQty != null) {
                                            Text(
                                                text = "Official stock adjusted to ${disputeCountLabel(dispute.adjustedStockQty, dispute.adjustedLooseMl ?: 0, hasVolume, " units")}.",
                                                fontSize = 11.sp,
                                                color = AmberPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Admin Resolve Button
                            if ((currentUser.role == UserRole.OWNER || currentUser.role == UserRole.MANAGER) && dispute.status == DisputeStatus.OPEN) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        resolvingDispute = dispute
                                        resolutionNotesText = ""
                                        adjustedStockText = "${dispute.reportedQty}"
                                        adjustedLooseMlText = "${dispute.reportedLooseMl}"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("resolve_dispute_button_${dispute.id}")
                                ) {
                                    Icon(Icons.Filled.Gavel, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Investigate & Resolve Dispute", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Resolve Dispute Dialog
    resolvingDispute?.let { dispute ->
        val stockQtyVal = adjustedStockText.toIntOrNull()
        val isStockQtyValid = stockQtyVal != null && stockQtyVal >= 0
        val dialogItem = items.firstOrNull { it.id == dispute.itemId }
        val dialogVolumeMl = dialogItem?.takeIf { it.isMlTracked() }?.bottleVolumeMl ?: 0
        val isMidShiftDispute = dispute.resolutionNotes.startsWith("Mid-shift adjustment")
        val showLooseMl = dialogVolumeMl > 0 && !isMidShiftDispute
        val looseMlVal = adjustedLooseMlText.ifBlank { "0" }.toIntOrNull()
        val isLooseMlValid = !showLooseMl || (looseMlVal != null && looseMlVal in 0 until dialogVolumeMl)
        val isFormValid = isStockQtyValid && isLooseMlValid

        AlertDialog(
            onDismissRequest = { resolvingDispute = null },
            title = {
                Text("Resolve Dispute: ${dispute.itemName}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Handover discrepancy between ${dispute.raisedByAttendantName} and ${dispute.involvesPreviousAttendantName ?: "Previous Attendant"}.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = resolutionNotesText,
                        onValueChange = { resolutionNotesText = it },
                        label = optionalLabel("Investigation Notes / Outcome"),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resolution_notes_input"),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )

                    if (showLooseMl) {
                        Text(
                            text = "Expected ${disputeCountLabel(dispute.expectedQty, dispute.expectedLooseMl, showLooseMl, " units")}, " +
                                "counted ${disputeCountLabel(dispute.reportedQty, dispute.reportedLooseMl, showLooseMl, " units")}.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = adjustedStockText,
                        onValueChange = { adjustedStockText = it },
                        label = requiredLabel(if (showLooseMl) "Official Counter Stock (bottles)" else "Official Counter Stock Quantity"),
                        isError = adjustedStockText.isNotEmpty() && !isStockQtyValid,
                        supportingText = if (adjustedStockText.isNotEmpty() && !isStockQtyValid) {
                            { Text("Quantity must be >= 0", color = CrimsonRed) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )

                    if (showLooseMl) {
                        OutlinedTextField(
                            value = adjustedLooseMlText,
                            onValueChange = { adjustedLooseMlText = it.filter { c -> c.isDigit() } },
                            label = optionalLabel("Official Loose ml"),
                            isError = !isLooseMlValid,
                            supportingText = {
                                if (!isLooseMlValid) Text("Must be 0 to ${dialogVolumeMl - 1} ml (bottle = $dialogVolumeMl ml)", color = CrimsonRed)
                                else Text("Open bottle remainder, 0 to ${dialogVolumeMl - 1} ml")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("resolution_loose_ml_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isFormValid) {
                            onResolveDispute(dispute.id, resolutionNotesText.trim(), stockQtyVal, if (showLooseMl) looseMlVal else null)
                            resolvingDispute = null
                        }
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Confirm Resolution", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resolvingDispute = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
