package com.healthexport.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// ── Static colour schemes (Claude-inspired warm palette) ──────────────────

private val LightColorScheme = lightColorScheme(
    primary              = Primary40,
    onPrimary            = androidx.compose.ui.graphics.Color.White,
    primaryContainer     = Primary90,
    onPrimaryContainer   = Primary10,

    secondary            = Secondary40,
    onSecondary          = androidx.compose.ui.graphics.Color.White,
    secondaryContainer   = Secondary90,
    onSecondaryContainer = Secondary10,

    tertiary             = Tertiary40,
    onTertiary           = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer    = Tertiary90,
    onTertiaryContainer  = Tertiary10,

    error                = Error40,
    onError              = androidx.compose.ui.graphics.Color.White,
    errorContainer       = Error90,
    onErrorContainer     = Error10,

    background           = Neutral99,
    onBackground         = Neutral10,
    surface              = Neutral99,
    onSurface            = Neutral10,
    surfaceVariant       = Neutral95,
    onSurfaceVariant     = Neutral20,
)

private val DarkColorScheme = darkColorScheme(
    primary              = PrimaryDark80,
    onPrimary            = Primary10,
    primaryContainer     = PrimaryDark40,
    onPrimaryContainer   = Primary90,

    secondary            = Secondary80,
    onSecondary          = Secondary10,
    secondaryContainer   = Secondary40,
    onSecondaryContainer = Secondary90,

    tertiary             = Tertiary80,
    onTertiary           = Tertiary10,
    tertiaryContainer    = Tertiary40,
    onTertiaryContainer  = Tertiary90,

    error                = Error80,
    onError              = Error10,
    errorContainer       = Error40,
    onErrorContainer     = Error90,

    background           = Neutral10,
    onBackground         = Neutral90,
    surface              = Neutral10,
    onSurface            = Neutral90,
    surfaceVariant       = Neutral20,
    onSurfaceVariant     = Neutral80,
)

// Neutral90 is not declared above — add it here for dark scheme completeness.
private val Neutral80 = androidx.compose.ui.graphics.Color(0xFFD4C8C4)
private val Neutral90 = androidx.compose.ui.graphics.Color(0xFFE8DDD7)

// ── Public theme composable ────────────────────────────────────────────────

/**
 * Main app theme.
 *
 * On Android 12+ (API 31) Material You dynamic colours are available.
 * We prefer our curated palette ([dynamicColor] = false by default) so the
 * Claude-inspired look is consistent across devices.
 */
@Composable
fun HealthExportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = HealthExportTypography,
        content     = content,
    )
}
