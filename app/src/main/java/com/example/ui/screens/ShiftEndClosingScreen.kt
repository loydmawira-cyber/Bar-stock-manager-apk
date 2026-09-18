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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.components.CategoryBadge
import com.example.ui.components.LocalCurrencySymbol
import com.example.ui.components.optionalLabel
import com.example.ui.components.requiredLabel
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.ClosingCountState
import java.util.Locale

@Composable
fun ShiftEndClosingScreen(
    closingItems: List<ClosingCountState>,
    paymentMethods: List<String>,
    submittedPaymentInputs: Map<String, String>,
    submittedCashInput: String,
    submittedMpesaInput: String,
    submittedCardInput: String,
    submittedBankInput: String,
    closingNotesInput: String,
    onUpdateClosingQty: (itemId: Long, count: Int, looseMl: Int) -> Unit,
    onUpdatePayment: (method: String, value: String) -> Unit,
    onUpdateSubmittedCash: (String) -> Unit,
    onUpdateSubmittedMpesa: (String) -> Unit,
    onUpdateSubmittedCard: (String) -> Unit,
    onUpdateSubmittedBank: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSubmitClosing: () -> Unit
) {
    val currency = LocalCurrencySymbol.current
    val totalExpected = closingItems.sumOf { it.expectedAmount }
    val totalUnitsSold = closingItems.sumOf { it.unitsSold }

    val submittedTotal = paymentMethods.sumOf { submittedPaymentInputs[it]?.toDoubleOrNull() ?: 0.0 }
    val variance = submittedTotal - totalExpected

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Closing Header & Sales Math Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2433)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmberPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SHIFT CLOSING & SALES MATH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberPrimary,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Calculate, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto Computed",
                                fontSize = 11.sp,
                                color = AmberPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Units Sold",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$totalUnitsSold units",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Expected Sales Cash",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(totalExpected),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldGreen
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Item by Item Closing Counts
        item {
            Text(
                text = "ENTER REMAINING STOCK ON COUNTER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Text(
                text = "Formula: units_sold = (opening + adjustments) - closing",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(closingItems) { item ->
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

                        // Opening / Adjustment context
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = if (item.totEnabled) {
                                        if (item.openingLooseMl > 0) "Opening: ${item.effectiveOpening} bottles + ${item.openingLooseMl} ml"
                                        else "Opening: ${item.effectiveOpening} bottles"
                                    } else {
                                        "Opening: ${item.openingQty}${if (item.adjustmentQty != 0) " (+${item.adjustmentQty})" else ""}"
                                    },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (item.totEnabled) {
                                        "Base: ${item.effectiveOpening * item.bottleVolumeMl + item.openingLooseMl} ml"
                                    } else {
                                        "Base: ${item.effectiveOpening}"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Closing Count Input & Units Sold Output
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Closing input stepper
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (item.totEnabled) "Closing bottles:" else "Closing Count:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            IconButton(
                                onClick = { onUpdateClosingQty(item.itemId, maxOf(0, item.closingQty - 1), item.closingLooseMl) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedTextField(
                                value = "${item.closingQty}",
                                onValueChange = { str ->
                                    val count = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    onUpdateClosingQty(item.itemId, count, item.closingLooseMl)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .width(70.dp)
                                    .testTag("closing_qty_input_${item.itemId}"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { onUpdateClosingQty(item.itemId, item.closingQty + 1, item.closingLooseMl) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                            ) {
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            if (item.totEnabled) {
                                OutlinedTextField(
                                    value = "${item.closingLooseMl}",
                                    onValueChange = { value ->
                                        val ml = value.filter { it.isDigit() }.toIntOrNull() ?: 0
                                        onUpdateClosingQty(item.itemId, item.closingQty, ml)
                                    },
                                    label = { Text("ml") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(78.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                                )
                            }
                        }

                        // Computed Sold & Amount
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (item.totEnabled) "Balance: ${item.closingQty} bottles ${item.closingLooseMl} ml" else "Sold: ${item.unitsSold}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary
                            )
                            Text(
                                text = formatCurrency(item.expectedAmount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }
            }
        }

        // Cash Handover & Reconciliation Input Card
        item {
            Spacer(modifier = Modifier.height(6.dp))

            val cashVal = submittedCashInput.toDoubleOrNull()
            val isCashValid = paymentMethods.isNotEmpty() && paymentMethods.all { (submittedPaymentInputs[it]?.toDoubleOrNull() ?: -1.0) >= 0.0 }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SALES SUBMISSION BY PAYMENT METHOD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enter bar sales collected through each payment method. No external account balance is required.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    paymentMethods.forEach { method ->
                        OutlinedTextField(
                            value = submittedPaymentInputs[method] ?: "0",
                            onValueChange = { onUpdatePayment(method, it) },
                            label = { Text("$method sales ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("payment_${method.lowercase().replace(" ", "_")}"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total submitted sales: ${formatCurrency(submittedTotal)}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = closingNotesInput,
                        onValueChange = onUpdateNotes,
                        label = optionalLabel("Shift Handover Notes"),
                        leadingIcon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Realtime Variance Preview
                    if (isCashValid) {
                        Spacer(modifier = Modifier.height(12.dp))

                        val (varBg, varColor, varTitle) = when {
                            variance < -0.01 -> Triple(
                                CrimsonRed.copy(alpha = 0.15f),
                                CrimsonRed,
                                "LOSS / SHORTAGE: ${String.format(Locale.US, "-%s%.2f", currency, Math.abs(variance))}"
                            )
                            variance > 0.01 -> Triple(
                                EmeraldGreen.copy(alpha = 0.15f),
                                EmeraldGreen,
                                "EXTRA / SURPLUS: +${String.format(Locale.US, "%s%.2f", currency, Math.abs(variance))}"
                            )
                            else -> Triple(
                                Color(0xFF064E3B).copy(alpha = 0.3f),
                                EmeraldGreen,
                                "BALANCED RECONCILIATION (${currency}0.00 Variance)"
                            )
                        }

                        Surface(
                            color = varBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = varTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = varColor
                                )
                                Text(
                                    text = if (variance < -0.01) "This shortage will be posted to your attendant loss ledger."
                                    else if (variance > 0.01) "This surplus will be posted to your attendant extra ledger."
                                    else "Clean handover! No shortage or surplus detected.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Final Submit Button
        item {
            val isCashValid = paymentMethods.isNotEmpty() && paymentMethods.all { (submittedPaymentInputs[it]?.toDoubleOrNull() ?: -1.0) >= 0.0 }
            Button(
                onClick = onSubmitClosing,
                enabled = isCashValid,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_shift_closing_button")
            ) {
                Icon(Icons.Filled.Receipt, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit Closing & Finalize Reconciliation",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
