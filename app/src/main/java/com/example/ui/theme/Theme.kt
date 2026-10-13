package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CodePilotDarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = Color(0xFF032239),
    primaryContainer = Color(0xFF073A5C),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = SecondaryViolet,
    onSecondary = Color(0xFF2E0854),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = AccentGreen,
    onTertiary = Color(0xFF063725),
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceCard,
    error = AccentRed,
    onError = Color(0xFF450A0A)
)

private val CodePilotLightColorScheme = lightColorScheme(
    primary = PrimaryCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAE6FD),
    onPrimaryContainer = Color(0xFF003554),
    secondary = SecondaryViolet,
    onSecondary = Color.White,
    tertiary = AccentGreen,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark-first developer experience
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CodePilotDarkColorScheme else CodePilotLightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
