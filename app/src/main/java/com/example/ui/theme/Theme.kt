package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AegisCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = AegisCyanContainer,
    onPrimaryContainer = AegisCyanOnContainer,
    secondary = AegisIndigo,
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = AegisIndigoContainer,
    onSecondaryContainer = AegisIndigoOnContainer,
    tertiary = AegisEmerald,
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = AegisEmeraldContainer,
    onTertiaryContainer = AegisEmeraldOnContainer,
    error = AegisRose,
    onError = Color.White,
    errorContainer = AegisRoseContainer,
    onErrorContainer = AegisRoseOnContainer,
    background = AegisBackground,
    onBackground = AegisTextPrimary,
    surface = AegisSurface,
    onSurface = AegisTextPrimary,
    surfaceVariant = AegisSurfaceCard,
    onSurfaceVariant = AegisTextSecondary,
    outline = AegisOutline
)

// For personal security assistant aesthetic, both dark and light retain clean deep cyber styling
private val LightColorScheme = DarkColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent sleek dark Aegis look
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
