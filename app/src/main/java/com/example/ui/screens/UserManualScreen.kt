package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.AmberPrimary

@Composable
fun UserManualScreen(currentRole: UserRole?) {
    val roles = UserManualContent.rolesVisibleTo(currentRole)
    var selectedRole by remember { mutableStateOf(roles.first()) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "User Manual",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (roles.size > 1) {
            Text(
                text = "Pick a guide. You can also share one with your team.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                roles.forEach { role ->
                    FilterChip(
                        selected = role == selectedRole,
                        onClick = { selectedRole = role },
                        label = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.Black
                        ),
                        modifier = Modifier.testTag("manual_chip_${role.name}")
                    )
                }
            }
        }

        Text(
            text = UserManualContent.title(selectedRole),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AmberPrimary
        )
        Text(
            text = UserManualContent.summary(selectedRole),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        UserManualContent.sections(selectedRole).forEachIndexed { index, section ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${index + 1}. ${section.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    section.steps.forEachIndexed { i, step ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                text = "${i + 1}.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AmberPrimary,
                                modifier = Modifier.width(24.dp)
                            )
                            Text(
                                text = step,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    section.tip?.let { tip ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tip: $tip",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberPrimary
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, UserManualContent.title(selectedRole))
                    putExtra(Intent.EXTRA_TEXT, UserManualContent.asPlainText(selectedRole))
                }
                context.startActivity(Intent.createChooser(send, "Share manual"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_share_button"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, AmberPrimary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary)
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share this guide", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
