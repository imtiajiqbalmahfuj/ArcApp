package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    secondary = SophisticatedSecondary,
    tertiary = SophisticatedTertiary,
    background = SophisticatedBg,
    surface = SophisticatedSurface,
    surfaceVariant = SophisticatedSurfaceVariant,
    onBackground = SophisticatedOnBg,
    onSurface = SophisticatedOnSurface,
    onSurfaceVariant = SophisticatedOnSurfaceVariant,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    outline = SophisticatedBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for Sophisticated Dark aesthetic
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve theme intent
    content: @Composable () -> Unit,
) {
    // We enforce our custom designed scheme to keep the premium dark look everywhere
    MaterialTheme(
        colorScheme = SophisticatedDarkColorScheme,
        typography = Typography,
        content = content
    )
}
