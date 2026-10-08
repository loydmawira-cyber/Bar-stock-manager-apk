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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BarProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.util.PinManager
import com.example.data.util.PinResult
import com.example.ui.components.PinDots
import com.example.ui.components.PinPad
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen

/**
 * Quick sign-in with a 4-digit PIN. Shown instead of the email/password form when at least one
 * account on this device has a PIN. Several people can share one phone, so each picks their name first.
 */
@Composable
fun PinLoginScreen(
    barProfile: BarProfile,
    pinUsers: List<User>,
    onLoginWithPin: (userId: Long, pin: String, onResult: (PinResult) -> Unit) -> Unit,
    onUsePassword: () -> Unit
) {
    var selectedUserId by remember { mutableStateOf<Long?>(if (pinUsers.size == 1) pinUsers.first().id else null) }
    var entry by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    // If the selected person lost their PIN (too many wrong tries / removed), go back to the picker.
    LaunchedEffect(pinUsers) {
        val sel = selectedUserId
        if (sel != null && pinUsers.none { it.id == sel }) {
            selectedUserId = if (pinUsers.size == 1) pinUsers.first().id else null
            entry = ""
        }
    }

    val selectedUser = pinUsers.firstOrNull { it.id == selectedUserId }

    fun submit(pin: String) {
        val user = selectedUser ?: return
        isChecking = true
        onLoginWithPin(user.id, pin) { result ->
            isChecking = false
            when (result) {
                is PinResult.Success -> Unit
                is PinResult.Wrong -> {
                    entry = ""
                    errorMessage = if (result.attemptsLeft == 1) "Wrong PIN. 1 attempt left."
                    else "Wrong PIN. ${result.attemptsLeft} attempts left."
                }
                is PinResult.LockedOut -> {
                    entry = ""
                    errorMessage = null
                }
                is PinResult.NotSet -> {
                    entry = ""
                    errorMessage = null
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(AmberPrimary.copy(alpha = 0.25f), AmberPrimary.copy(alpha = 0.08f))
                        )
                    )
                    .border(1.5.dp, AmberPrimary.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = barProfile.barName.ifBlank { "Welcome back" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            if (selectedUser == null) {
                Text(
                    text = "Who is signing in?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                pinUsers.forEach { user ->
                    PinUserCard(user = user, onClick = {
                        selectedUserId = user.id
                        entry = ""
                        errorMessage = null
                    })
                }
            } else {
                Text(
                    text = "Hi ${selectedUser.name.substringBefore(' ')}, enter your PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                PinDots(filled = entry.length, isError = errorMessage != null)

                Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                    when {
                        isChecking -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AmberPrimary
                        )
                        errorMessage != null -> Text(
                            errorMessage!!,
                            color = CrimsonRed,
                            fontSize = 13.sp,
                            modifier = Modifier.testTag("pin_error")
                        )
                    }
                }

                PinPad(
                    enabled = !isChecking,
                    onDigit = { d ->
                        if (!isChecking && entry.length < PinManager.PIN_LENGTH) {
                            errorMessage = null
                            entry += d
                            if (entry.length == PinManager.PIN_LENGTH) submit(entry)
                        }
                    },
                    onBackspace = { if (!isChecking && entry.isNotEmpty()) entry = entry.dropLast(1) }
                )

                if (pinUsers.size > 1) {
                    TextButton(onClick = {
                        selectedUserId = null
                        entry = ""
                        errorMessage = null
                    }) { Text("Switch user", color = AmberPrimary) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onUsePassword,
                enabled = !isChecking,
                modifier = Modifier.testTag("pin_use_password")
            ) {
                Text("Use email & password instead", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PinUserCard(user: User, onClick: () -> Unit) {
    val isAdmin = user.role == UserRole.OWNER || user.role == UserRole.MANAGER
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .testTag("pin_user_card_${user.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isAdmin) AmberPrimary.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAdmin) Icons.Outlined.Shield else Icons.Filled.Person,
                    contentDescription = null,
                    tint = if (isAdmin) AmberPrimary else EmeraldGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(user.role.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
