package com.multipos.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MultiPOSSecondary,
    onPrimary = Color.Black,
    primaryContainer = MultiPOSSecondaryVariant,
    onPrimaryContainer = Color.White,
    
    secondary = MultiPOSSecondary,
    onSecondary = Color.Black,
    secondaryContainer = MultiPOSDarkSurfaceVariant,
    onSecondaryContainer = MultiPOSDarkTextPrimary,
    
    tertiary = MultiPOSInfo,
    onTertiary = Color.Black,
    
    background = MultiPOSDarkBackground,
    onBackground = MultiPOSDarkTextPrimary,
    
    surface = MultiPOSDarkSurface,
    onSurface = MultiPOSDarkTextPrimary,
    surfaceVariant = MultiPOSDarkSurfaceVariant,
    onSurfaceVariant = MultiPOSDarkTextSecondary,
    
    error = MultiPOSError,
    onError = Color.White,
    
    outline = MultiPOSDarkBorder,
    outlineVariant = MultiPOSDarkDivider
)

private val LightColorScheme = lightColorScheme(
    primary = MultiPOSPrimary,
    onPrimary = Color.White,
    primaryContainer = MultiPOSPrimaryVariant,
    onPrimaryContainer = Color.White,
    
    secondary = MultiPOSSecondary,
    onSecondary = Color.Black,
    secondaryContainer = MultiPOSSurfaceVariant,
    onSecondaryContainer = MultiPOSTextSecondary,
    
    tertiary = MultiPOSInfo,
    onTertiary = Color.White,
    
    background = MultiPOSBackground,
    onBackground = MultiPOSTextPrimary,
    
    surface = MultiPOSSurface,
    onSurface = MultiPOSTextPrimary,
    surfaceVariant = MultiPOSSurfaceVariant,
    onSurfaceVariant = MultiPOSTextSecondary,
    
    error = MultiPOSError,
    onError = Color.White,
    
    outline = MultiPOSBorder,
    outlineVariant = MultiPOSDivider
)

@Composable
fun MultiPOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Se puede activar para Material You
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MultiPOSTypography,
        content = content
    )
}

// Extensiones para colores personalizados MultiPOS
val ColorScheme.success: Color get() = MultiPOSSuccess
val ColorScheme.warning: Color get() = MultiPOSWarning
val ColorScheme.onBackgroundVariant: Color get() = if (isLight) MultiPOSTextSecondary else MultiPOSDarkTextSecondary
val ColorScheme.warningContainer: Color get() = MultiPOSWarning.copy(alpha = 0.1f)

private val ColorScheme.isLight: Boolean
    get() = this.primary == MultiPOSPrimary

