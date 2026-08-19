package com.multipos.app.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * SISTEMA DE TEMAS PREMIUM MULTI-EMPRESA
 * Inspirado en el diseño Esmeralda / Grey Premium.
 */

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = EmeraldPrimaryDark,
    onPrimaryContainer = Color.White,
    
    secondary = AccentTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = DeepDarkSurface,
    onSecondaryContainer = TextOnDark,
    
    tertiary = AccentOrange,
    onTertiary = Color.Black,
    
    background = DeepDarkBackground,
    onBackground = TextOnDark,
    
    surface = DeepDarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DeepDarkBorder,
    onSurfaceVariant = TextSecondary,
    
    error = SoftError,
    onError = Color.White,
    
    outline = DeepDarkBorder,
    outlineVariant = DeepDarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimaryLight,
    onPrimaryContainer = EmeraldPrimaryDark,
    
    secondary = AccentTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = PremiumWhiteSurface,
    onSecondaryContainer = TextSecondary,
    
    tertiary = AccentOrange,
    onTertiary = Color.White,
    
    background = PremiumGreyBackground,
    onBackground = TextPrimary,
    
    surface = PremiumWhiteSurface,
    onSurface = TextPrimary,
    surfaceVariant = PremiumGreyBackground,
    onSurfaceVariant = TextSecondary,
    
    error = SoftError,
    onError = Color.White,
    
    outline = PremiumGreyBorder,
    outlineVariant = PremiumGreyBorder
)

/**
 * TEMA PRINCIPAL: MultiBusinessAppTheme
 * Este tema cambia automáticamente el aspecto visual de toda la App a un estilo Esmeralda Premium.
 */
@Composable
fun MultiBusinessAppTheme(
    darkTheme: Boolean = false, // Forzamos modo claro por defecto para mantener la estética del Plan Premium
    // Dynamic color desactivado para forzar la identidad visual corporativa
    dynamicColor: Boolean = false,
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
            // Hacemos las barras del sistema transparentes para un look "Edge-to-Edge" total
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            // Ajustamos el color de los iconos (batería, wifi, etc) según el tema
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MultiPOSTypography,
        content = content
    )
}

/**
 * TEMA LEGACY (Para compatibilidad): MultiPOSTheme
 * Redirige al nuevo tema premium para no romper código existente.
 */
@Composable
fun MultiPOSTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MultiBusinessAppTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

// --- EXTENSIONES DE UTILIDAD PARA EL DESARROLLADOR ---

val ColorScheme.success: Color get() = SoftSuccess
val ColorScheme.warning: Color get() = SoftWarning
val ColorScheme.onBackgroundVariant: Color get() = TextSecondary
val ColorScheme.warningContainer: Color get() = SoftWarning.copy(alpha = 0.12f)
val ColorScheme.premiumBorder: Color get() = if (this.surface == PremiumWhiteSurface) PremiumGreyBorder else DeepDarkBorder
