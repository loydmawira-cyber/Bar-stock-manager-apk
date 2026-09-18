package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Counter
import com.example.ui.components.CategoryBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OrangeAccent
import com.example.ui.viewmodel.OpeningVerificationState

@Composable
fun ShiftStartVerificationScreen(
    counter: Counter?,
    items: List<OpeningVerificationState>,
    onUpdateApproval: (itemId: Long, approved: Boolean) -> Unit,
    onUpdateCountedQty: (itemId: Long, count: Int) -> Unit,
    onUpdateCountedLooseMl: (itemId: Long, looseMl: Int) -> Unit,
    onConfirmAndStartShift: () -> Unit,
    onCancel: () -> Unit
) {
    val disputedCount = items.count { !it.isApproved || it.countedQty != it.systemQty }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "OPENING STOCK HANDOVER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = counter?.name ?: "Bar Counter",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Verify each item before taking physical responsibility. Flag any missing or excess stock.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (disputedCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = CrimsonRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$disputedCount item(s) flagged with discrepancies. Dispute logs will be sent to Admin and previous attendant.",
                                fontSize = 12.sp,
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Item List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                val hasDiscrepancy = !item.isApproved ||
                    item.countedQty != item.systemQty ||
                    item.countedLooseMl != item.systemLooseMl
                val diff = item.countedQty - item.systemQty

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasDiscrepancy) Color(0xFF2A1515) else DarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (hasDiscrepancy) CrimsonRed.copy(alpha = 0.6f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryBadge(category = item.category)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${formatCurrency(item.unitPrice)} / ${item.unitType}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.itemName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // System Expected Quantity Readout
                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Actual balance",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${item.systemQty} bottles + ${item.systemLooseMl} ml",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Verification Actions: Approve vs Disapprove
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = item.isApproved &&
                                        item.countedQty == item.systemQty &&
                                        item.countedLooseMl == item.systemLooseMl,
                                    onClick = {
                                        onUpdateApproval(item.itemId, true)
                                        onUpdateCountedQty(item.itemId, item.systemQty)
                                        onUpdateCountedLooseMl(item.itemId, item.systemLooseMl)
                                    },
                                    label = { Text(
                                        "Approve (${item.systemQty} bottles + ${item.systemLooseMl} ml)",
                                        fontSize = 12.sp
                                    ) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldGreen,
                                        selectedLabelColor = Color.Black,
                                        selectedLeadingIconColor = Color.Black
                                    )
                                )

                                FilterChip(
                                    selected = !item.isApproved ||
                                        item.countedQty != item.systemQty ||
                                        item.countedLooseMl != item.systemLooseMl,
                                    onClick = {
                                        onUpdateApproval(item.itemId, false)
                                    },
                                    label = { Text("Disapprove", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CrimsonRed,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )
                            }

                            // Discrepancy Tag
                            if (hasDiscrepancy) {
                                Surface(
                                    color = CrimsonRed,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (item.countedLooseMl != item.systemLooseMl) {
                                            "TOT remainder differs"
                                        } else if (diff > 0) "+$diff Extra" else "$diff Shortage",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // If disapproved, show actual physical count input
                        AnimatedVisibility(
                            visible = !item.isApproved ||
                                item.countedQty != item.systemQty ||
                                item.countedLooseMl != item.systemLooseMl
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "Enter actual physical count on shelf:",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onUpdateCountedQty(item.itemId, maxOf(0, item.countedQty - 1)) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaceVariant)
                                    ) {
                                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    OutlinedTextField(
                                        value = "${item.countedQty}",
                                        onValueChange = { str ->
                                            val count = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                            onUpdateCountedQty(item.itemId, count)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .width(80.dp)
                                            .testTag("counted_qty_input_${item.itemId}"),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CrimsonRed,
                                            unfocusedBorderColor = CrimsonRed.copy(alpha = 0.5f)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = "${item.countedLooseMl}",
                                        onValueChange = { str ->
                                            val looseMl = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                            onUpdateCountedLooseMl(item.itemId, looseMl)
                                        },
                                        label = { Text("ml", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .width(90.dp)
                                            .testTag("counted_loose_ml_input_${item.itemId}"),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CrimsonRed,
                                            unfocusedBorderColor = CrimsonRed.copy(alpha = 0.5f)
                                        )
                                    )

                                    IconButton(
                                        onClick = { onUpdateCountedQty(item.itemId, item.countedQty + 1) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaceVariant)
                                    ) {
                                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Text(
                                        text = "Dispute will be logged",
                                        fontSize = 11.sp,
                                        color = CrimsonRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm Action Button
        Button(
            onClick = onConfirmAndStartShift,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (disputedCount > 0) OrangeAccent else EmeraldGreen
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_opening_and_start_shift_button")
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (disputedCount > 0) "Confirm & Start Shift ($disputedCount Disputes)" else "All Confirmed OK · Start Shift",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
