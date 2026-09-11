package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DisputeStatus
import com.example.data.model.ItemCategory
import com.example.data.model.ReconciliationType
import com.example.data.model.ShiftStatus
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OnDarkSurfaceVariant
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SkyBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CategoryBadge(category: ItemCategory, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (category) {
        ItemCategory.BEER -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "Beer")
        ItemCategory.SPIRIT -> Triple(Color(0xFFEDE9FE), Color(0xFF5B21B6), "Spirit")
        ItemCategory.TOT -> Triple(Color(0xFFFCE7F3), Color(0xFF9D174D), "Tot / Shot")
        ItemCategory.WINE -> Triple(Color(0xFFFFE4E6), Color(0xFF9F1239), "Wine")
        ItemCategory.SOFT_DRINK -> Triple(Color(0xFFE0F2FE), Color(0xFF075985), "Soft Drink")
        ItemCategory.OTHER -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "Other")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(contentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ShiftStatusBadge(status: ShiftStatus) {
    when (status) {
        ShiftStatus.ACTIVE -> StatusBadge(
            text = "Active Shift",
            containerColor = Color(0xFF064E3B),
            contentColor = Color(0xFF34D399)
        )
        ShiftStatus.CLOSED -> StatusBadge(
            text = "Closed",
            containerColor = DarkSurfaceVariant,
            contentColor = OnDarkSurfaceVariant
        )
        ShiftStatus.DISPUTED -> StatusBadge(
            text = "Disputed",
            containerColor = CrimsonRed.copy(alpha = 0.2f),
            contentColor = CrimsonRed
        )
    }
}

@Composable
fun ReconciliationBadge(type: ReconciliationType, variance: Double = 0.0) {
    val currency = LocalCurrencySymbol.current
    when (type) {
        ReconciliationType.LOSS -> StatusBadge(
            text = "LOSS (${String.format(Locale.US, "-%s%.2f", currency, Math.abs(variance))})",
            containerColor = CrimsonRed.copy(alpha = 0.2f),
            contentColor = CrimsonRed
        )
        ReconciliationType.EXTRA -> StatusBadge(
            text = "EXTRA (+${String.format(Locale.US, "%s%.2f", currency, Math.abs(variance))})",
            containerColor = EmeraldGreen.copy(alpha = 0.2f),
            contentColor = EmeraldGreen
        )
        ReconciliationType.BALANCED -> StatusBadge(
            text = "BALANCED (${currency}0.00)",
            containerColor = DarkSurface,
            contentColor = AmberPrimary
        )
        ReconciliationType.NONE -> StatusBadge(
            text = "Pending Handover",
            containerColor = DarkSurfaceVariant,
            contentColor = OnDarkSurfaceVariant
        )
    }
}


val LocalCurrencySymbol = staticCompositionLocalOf { "$" }

@Composable
fun formatCurrency(amount: Double): String {
    val symbol = LocalCurrencySymbol.current
    return String.format(Locale.US, "%s%,.2f", symbol, amount)
}

fun formatCurrencyRaw(amount: Double, symbol: String = "$"): String {
    return String.format(Locale.US, "%s%,.2f", symbol, amount)
}

@Composable
fun DisputeBadge(status: DisputeStatus) {
    when (status) {
        DisputeStatus.OPEN -> StatusBadge(
            text = "Open Dispute",
            containerColor = Color(0xFF450A0A),
            contentColor = CrimsonRed
        )
        DisputeStatus.RESOLVED -> StatusBadge(
            text = "Resolved",
            containerColor = Color(0xFF064E3B),
            contentColor = EmeraldGreen
        )
    }
}

@Composable
fun UserStatusBadge(status: UserStatus) {
    when (status) {
        UserStatus.APPROVED -> StatusBadge(
            text = "Approved",
            containerColor = Color(0xFF064E3B),
            contentColor = EmeraldGreen
        )
        UserStatus.PENDING -> StatusBadge(
            text = "Pending Approval",
            containerColor = Color(0xFF451A03),
            contentColor = AmberPrimary
        )
        UserStatus.REVOKED -> StatusBadge(
            text = "Revoked",
            containerColor = Color(0xFF450A0A),
            contentColor = CrimsonRed
        )
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color = AmberPrimary,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = iconColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


fun formatTimeOnly(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
