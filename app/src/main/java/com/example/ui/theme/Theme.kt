package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ArenaDarkColorScheme = darkColorScheme(
    primary = CrimsonFlame,
    onPrimary = TextPrimary,
    primaryContainer = CrimsonDark,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = CyanDark,
    onSecondaryContainer = TextPrimary,
    tertiary = SolarGold,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder
)

@Composable
fun ArenaChampionsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ArenaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
