package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = OnDarkPrimary,
    primaryContainer = Color(0xFF4A2810),
    onPrimaryContainer = AmberTertiary,
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E231E),
    onSecondaryContainer = Color(0xFFFDF8F5),
    tertiary = AmberTertiary,
    onTertiary = Color(0xFF120E0C),
    background = DarkBackground,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    error = CrimsonRed,
    onError = Color.White,
    outline = Color(0xFF4D3B33),
    outlineVariant = Color(0xFF382A24)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDE8D0),
    onPrimaryContainer = Color(0xFF5A2A06),
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE6DC),
    onSecondaryContainer = Color(0xFF231812),
    tertiary = AmberPrimary,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = OnLightSurface,
    surface = LightSurface,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceVariant,
    error = CrimsonRed,
    onError = Color.White,
    outline = Color(0xFFD4C5B9),
    outlineVariant = Color(0xFFEFE6DC)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to rich, warm restaurant & bar ambiance
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
