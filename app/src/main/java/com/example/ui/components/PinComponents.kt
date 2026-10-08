package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.PinManager
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurfaceVariant

/** Row of 4 dots that fill as digits are entered. */
@Composable
fun PinDots(filled: Int, isError: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(PinManager.PIN_LENGTH) { index ->
            val color = if (isError) CrimsonRed else AmberPrimary
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .then(
                        if (index < filled) Modifier.background(color)
                        else Modifier.border(1.5.dp, color.copy(alpha = 0.6f), CircleShape)
                    )
            )
        }
    }
}

/** Numeric keypad. Calls [onDigit] for 0-9 and [onBackspace] for the delete key. */
@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    enabled: Boolean = true
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { d -> PinKey(label = d.toString(), enabled = enabled, tag = "pin_key_$d") { onDigit(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(modifier = Modifier.size(72.dp))
            PinKey(label = "0", enabled = enabled, tag = "pin_key_0") { onDigit('0') }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled) { onBackspace() }
                    .testTag("pin_key_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PinKey(label: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(DarkSurfaceVariant.copy(alpha = 0.6f))
            .clickable(enabled = enabled) { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Dialog to create (or change) a 4-digit PIN. The person enters it twice to confirm.
 * [onSetPin] receives the confirmed PIN. [onNotNow]/[onNeverAsk] are only shown for the first-time prompt.
 */
@Composable
fun SetPinDialog(
    userName: String,
    isChange: Boolean,
    onSetPin: (String) -> Unit,
    onDismiss: () -> Unit,
    onNeverAsk: (() -> Unit)? = null
) {
    var firstPin by remember { mutableStateOf("") }
    var entry by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun handleComplete(pin: String) {
        if (!confirming) {
            if (PinManager.isTooWeak(pin)) {
                error = "That PIN is too easy to guess (like 1234 or 0000). Choose another."
                entry = ""
            } else {
                firstPin = pin
                entry = ""
                error = null
                confirming = true
            }
        } else {
            if (pin == firstPin) {
                onSetPin(pin)
            } else {
                error = "PINs didn't match. Start again."
                entry = ""
                firstPin = ""
                confirming = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isChange) "Change your PIN" else "Set a 4-digit PIN",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when {
                        confirming -> "Enter the PIN again to confirm"
                        isChange -> "Choose a new PIN for $userName"
                        else -> "Next time, sign in with a quick PIN instead of your email and password on this phone."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                PinDots(filled = entry.length, isError = error != null)
                Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.Center) {
                    error?.let {
                        Text(it, color = CrimsonRed, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
                PinPad(
                    onDigit = { d ->
                        if (entry.length < PinManager.PIN_LENGTH) {
                            error = null
                            entry += d
                            if (entry.length == PinManager.PIN_LENGTH) handleComplete(entry)
                        }
                    },
                    onBackspace = { if (entry.isNotEmpty()) entry = entry.dropLast(1) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (onNeverAsk != null) "Not now" else "Cancel") }
        },
        dismissButton = onNeverAsk?.let {
            { TextButton(onClick = it) { Text("Don't ask again") } }
        }
    )
}
