package org.umn.utsmap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BudgetGreenDark, onPrimary = Color.White,
    primaryContainer = BudgetMint, onPrimaryContainer = Color(0xFF064D26),
    secondary = Color(0xFF42745A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7F2EB), onSecondaryContainer = Color(0xFF163C28),
    tertiary = Color(0xFF906900), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF1CC), onTertiaryContainer = Color(0xFF493400),
    background = Color.White, onBackground = BudgetInk,
    surface = Color.White, onSurface = BudgetInk,
    surfaceVariant = Color(0xFFF3F8F5), onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFF94A3B8), outlineVariant = Color(0xFFE2E8E5),
    error = Color(0xFFFF6B6B), onError = Color.White,
    errorContainer = Color(0xFFFFE9E7), onErrorContainer = Color(0xFF751B1B),
)
private val DarkColors = darkColorScheme(
    primary = BudgetGreen, onPrimary = Color(0xFF002D13),
    primaryContainer = Color(0xFF123D27), onPrimaryContainer = Color(0xFFA5F0BF),
    secondary = Color(0xFFA5CEB3), onSecondary = Color(0xFF103822),
    secondaryContainer = Color(0xFF264D36), onSecondaryContainer = Color(0xFFD3EEDA),
    tertiary = Color(0xFFF4C44F), onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF554211), onTertiaryContainer = Color(0xFFFFE7A2),
    background = BudgetNight, onBackground = Color(0xFFF2F7F4),
    surface = Color(0xFF11241B), onSurface = Color(0xFFF2F7F4),
    surfaceVariant = Color(0xFF192E23), onSurfaceVariant = Color(0xFFA6B8AC),
    outline = Color(0xFF809789), outlineVariant = Color(0xFF294033),
    error = Color(0xFFFF8B86), onError = Color(0xFF650E15),
    errorContainer = Color(0xFF482024), onErrorContainer = Color(0xFFFFDAD6),
)

/** Warna merek tetap konsisten, tidak mengikuti dynamic color wallpaper. */
@Composable
fun BudgetLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BudgetLensTypography,
        shapes = BudgetLensShapes,
        content = content,
    )
}
