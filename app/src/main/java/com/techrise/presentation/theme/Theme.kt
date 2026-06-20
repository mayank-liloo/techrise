package com.techrise.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SportyPrimary,
    onPrimary = SportyOnPrimary,
    primaryContainer = SportySurface,
    onPrimaryContainer = SportyPrimary,
    secondary = SportySecondary,
    onSecondary = SportyOnSecondary,
    secondaryContainer = SportySurfaceVariant,
    onSecondaryContainer = SportyTextPrimary,
    background = SportyBackground,
    onBackground = SportyTextPrimary,
    surface = SportySurface,
    onSurface = SportyTextPrimary,
    surfaceVariant = SportySurfaceVariant,
    onSurfaceVariant = SportyTextSecondary,
    outline = SportyBorder,
    outlineVariant = SportyBorder.copy(alpha = 0.5f),
    error = StatusPending
)

private val LightColorScheme = lightColorScheme(
    primary = CoolBlue,
    onPrimary = Color.White,
    primaryContainer = LightBorder,
    onPrimaryContainer = CoolBlueVariant,
    secondary = Color(0xFFF59E0B), // Amber Yellow
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = TextPrimaryLight,
    background = CleanSlateBackground,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    outlineVariant = LightBorder.copy(alpha = 0.5f),
    error = StatusPending
)

@Composable
fun SecureCRMTheme(
    darkTheme: Boolean = false, // Light theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
