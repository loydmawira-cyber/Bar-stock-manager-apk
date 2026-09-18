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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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

/**
 * Whether an item's current counted values still match what the system expects.
 * Used to decide the "disputed" indicator color for an item that has been verified.
 */
private fun OpeningVerificationState.hasDiscrepancy(): Boolean =
    !isApproved || countedQty != systemQty || countedLooseMl != systemLooseMl

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
    if (items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No items to verify for this counter.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onConfirmAndStartShift) {
                Text("Start Shift")
            }
        }
        return
    }

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    // Keep the pointer valid if the underlying item list ever changes size.
    LaunchedEffect(items.size) {
        if (currentIndex > items.lastIndex) currentIndex = items.lastIndex
        if (currentIndex < 0) currentIndex = 0
    }

    // True right after the attendant confirms the count on the LAST item.
    // There's no next item to advance to, so this just closes that item's
    // count-entry panel and shows a "review complete" message instead of
    // silently doing nothing. It resets whenever they move to a different
    // item (e.g. via Previous), so re-visiting the last item shows its
    // panel again if they want to re-check it.
    var reviewComplete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(currentIndex) { reviewComplete = false }

    val verifiedCount = items.count { it.verified }
    val disputedCount = items.count { it.verified && it.hasDiscrepancy() }
    val allVerified = verifiedCount == items.size
    val currentItem = items[currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
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
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("cancel_shift_verification_button")) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = "Verify each item one by one before taking physical responsibility.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Item ${currentIndex + 1} of ${items.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$verifiedCount/${items.size} verified" + if (disputedCount > 0) " · $disputedCount disputed" else "",
                        fontSize = 12.sp,
                        color = if (disputedCount > 0) CrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { verifiedCount.toFloat() / items.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (disputedCount > 0) OrangeAccent else EmeraldGreen,
                    trackColor = DarkSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Item navigator: tap any dot to jump straight to that item.
        // Color shows its status at a glance: pending / verified OK / disputed.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items.size) { index ->
                val it = items[index]
                val isCurrent = index == currentIndex
                val dotColor = when {
                    !it.verified -> DarkSurfaceVariant
                    it.hasDiscrepancy() -> CrimsonRed
                    else -> EmeraldGreen
                }
                val dotTextColor = if (!it.verified) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(
                            width = if (isCurrent) 2.dp else 0.dp,
                            color = AmberPrimary,
                            shape = CircleShape
                        )
                        .testTag("verification_step_dot_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    if (it.verified && !it.hasDiscrepancy()) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else if (it.verified) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text("${index + 1}", fontSize = 12.sp, color = dotTextColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Current item card — the one item the attendant is verifying right now.
        Box(modifier = Modifier.weight(1f)) {
            val hasDiscrepancy = currentItem.hasDiscrepancy()
            val diff = currentItem.countedQty - currentItem.systemQty

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (currentItem.verified && hasDiscrepancy) Color(0xFF2A1515) else DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (currentItem.verified && hasDiscrepancy) CrimsonRed.copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadge(category = currentItem.category)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${formatCurrency(currentItem.unitPrice)} / ${currentItem.unitType}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentItem.itemName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(color = DarkSurfaceVariant, shape = RoundedCornerShape(8.dp)) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Actual balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (currentItem.mlTracked) "${currentItem.systemQty} bottles + ${currentItem.systemLooseMl} ml" else "${currentItem.systemQty} units",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!currentItem.verified) {
                        Text(
                            text = "Does the physical stock on the shelf match the balance above?",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onUpdateApproval(currentItem.itemId, true)
                                onUpdateCountedQty(currentItem.itemId, currentItem.systemQty)
                                onUpdateCountedLooseMl(currentItem.itemId, currentItem.systemLooseMl)
                                if (currentIndex < items.lastIndex) currentIndex += 1
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentItem.verified && !hasDiscrepancy) EmeraldGreen else DarkSurfaceVariant,
                                contentColor = if (currentItem.verified && !hasDiscrepancy) Color.Black else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("verify_ok_button_${currentItem.itemId}")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verified OK", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { onUpdateApproval(currentItem.itemId, false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentItem.verified && hasDiscrepancy) CrimsonRed else DarkSurfaceVariant,
                                contentColor = if (currentItem.verified && hasDiscrepancy) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("verify_dispute_button_${currentItem.itemId}")
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dispute", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Once the count is confirmed on the last item, close this item's
                    // entry panel and let the attendant decide when to finish, instead
                    // of jumping straight into starting the shift.
                    AnimatedVisibility(visible = reviewComplete && currentIndex == items.lastIndex) {
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Count confirmed. Review any item above, then tap \"Confirm & Start Shift\" below when ready.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Once disputed, capture the real physical count and confirm to move on.
                    AnimatedVisibility(visible = currentItem.verified && hasDiscrepancy && !reviewComplete) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
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
                                    onClick = { onUpdateCountedQty(currentItem.itemId, maxOf(0, currentItem.countedQty - 1)) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceVariant)
                                ) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                OutlinedTextField(
                                    value = "${currentItem.countedQty}",
                                    onValueChange = { str ->
                                        val count = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                        onUpdateCountedQty(currentItem.itemId, count)
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .width(80.dp)
                                        .testTag("counted_qty_input_${currentItem.itemId}"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CrimsonRed,
                                        unfocusedBorderColor = CrimsonRed.copy(alpha = 0.5f)
                                    )
                                )

                                if (currentItem.mlTracked) {
                                    OutlinedTextField(
                                        value = "${currentItem.countedLooseMl}",
                                        onValueChange = { str ->
                                            val looseMl = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                            onUpdateCountedLooseMl(currentItem.itemId, looseMl)
                                        },
                                        label = { Text("ml", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .width(90.dp)
                                            .testTag("counted_loose_ml_input_${currentItem.itemId}"),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CrimsonRed,
                                            unfocusedBorderColor = CrimsonRed.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                IconButton(
                                    onClick = { onUpdateCountedQty(currentItem.itemId, currentItem.countedQty + 1) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceVariant)
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(color = CrimsonRed, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = if (currentItem.countedLooseMl != currentItem.systemLooseMl) {
                                        "TOT remainder differs — dispute will be logged"
                                    } else if (diff > 0) "+$diff Extra — dispute will be logged" else "$diff Shortage — dispute will be logged",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (currentIndex < items.lastIndex) {
                                        currentIndex += 1
                                    } else {
                                        reviewComplete = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_discrepancy_and_continue_button")
                            ) {
                                Text(
                                    text = if (currentIndex < items.lastIndex) "Confirm Count & Next Item" else "Confirm Count",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Manual Previous / Next — lets the attendant review or revisit items
        // without changing their verification status.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { if (currentIndex > 0) currentIndex -= 1 },
                enabled = currentIndex > 0,
                modifier = Modifier.testTag("verification_previous_button")
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous")
            }
            OutlinedButton(
                onClick = { if (currentIndex < items.lastIndex) currentIndex += 1 },
                enabled = currentIndex < items.lastIndex,
                modifier = Modifier.testTag("verification_next_button")
            ) {
                Text("Next")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!allVerified) {
            Text(
                text = "Verify all ${items.size} items to continue (${items.size - verifiedCount} remaining)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Final confirmation — only meaningful once every item has been verified one by one.
        Button(
            onClick = onConfirmAndStartShift,
            enabled = allVerified,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (disputedCount > 0) OrangeAccent else EmeraldGreen,
                disabledContainerColor = DarkSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_opening_and_start_shift_button")
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (allVerified) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (!allVerified) "Verify All Items First"
                    else if (disputedCount > 0) "Confirm & Start Shift ($disputedCount Disputes)"
                    else "All Confirmed OK · Start Shift",
                color = if (allVerified) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
