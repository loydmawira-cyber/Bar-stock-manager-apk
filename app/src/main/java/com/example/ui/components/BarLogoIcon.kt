package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberPrimary

data class BarIconPreset(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

val BAR_ICON_PRESETS = listOf(
    BarIconPreset("cocktail", "Cocktail Lounge", Icons.Filled.LocalBar, AmberPrimary),
    BarIconPreset("beer", "Taproom & Brewery", Icons.Filled.SportsBar, Color(0xFFF59E0B)),
    BarIconPreset("wine", "Wine & Bistro", Icons.Filled.WineBar, Color(0xFFEC4899)),
    BarIconPreset("nightlife", "Nightclub / VIP", Icons.Filled.Nightlife, Color(0xFF8B5CF6)),
    BarIconPreset("celebration", "Party & Events", Icons.Filled.Celebration, Color(0xFF10B981)),
    BarIconPreset("lounge", "Cafe & Bar", Icons.Filled.LocalCafe, Color(0xFF06B6D4))
)

@Composable
fun BarLogoIcon(
    iconType: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color = AmberPrimary,
    backgroundColor: Color = AmberPrimary.copy(alpha = 0.15f),
    borderColor: Color = AmberPrimary.copy(alpha = 0.4f),
    shape: androidx.compose.ui.graphics.Shape = CircleShape
) {
    val preset = BAR_ICON_PRESETS.firstOrNull { it.id == iconType } ?: BAR_ICON_PRESETS.first()

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor)
            .border(1.5.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = preset.icon,
            contentDescription = preset.name,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
