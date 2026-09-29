package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueGlow,
    onPrimary = TextInverse,
    primaryContainer = DarkCardSurfaceVariant,
    onPrimaryContainer = TextPrimary,
    secondary = SecondarySlate,
    onSecondary = TextPrimary,
    background = DarkAppBackground,
    onBackground = TextPrimary,
    surface = DarkCardSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCardSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = StatusDanger,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme as requested by user
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
