package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = MoriDarkPrimary,
    onPrimary = MoriDarkOnPrimary,
    primaryContainer = MoriDarkPrimaryContainer,
    onPrimaryContainer = MoriDarkOnPrimaryContainer,
    background = MoriDarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = MoriDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = MoriDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = MoriDarkOutline,
    outlineVariant = MoriDarkOutlineVariant,
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFF7F1D1D)
)

private val LightColorScheme = lightColorScheme(
    primary = MoriLightPrimary,
    onPrimary = MoriLightOnPrimary,
    primaryContainer = MoriLightPrimaryContainer,
    onPrimaryContainer = MoriLightOnPrimaryContainer,
    background = MoriLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = MoriLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = MoriLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF64748B),
    outline = MoriLightOutline,
    outlineVariant = MoriLightOutlineVariant,
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2)
)

@Composable
fun MoriTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
