package com.qtpie.simplepuzzle.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LightColorScheme = lightColorScheme(
    primary = PurpleBlue,
    onPrimary = Color.White,
    primaryContainer = PurpleBlue.copy(alpha = 0.2f),
    onPrimaryContainer = PurpleBlue,

    secondary = CoralPink,
    onSecondary = Color.White,
    secondaryContainer = CoralPink.copy(alpha = 0.2f),
    onSecondaryContainer = CoralPink,

    tertiary = WarmYellow,
    onTertiary = Color.Black,

    background = BackgroundLight,
    onBackground = TextOnLight,

    surface = CardBackground,
    onSurface = TextPrimary,

    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,

    error = Error,
    onError = Color.White,
)

// Dark Color Scheme (for future dark mode)
val DarkColorScheme = darkColorScheme(
    primary = PurpleBlue,
    onPrimary = Color.White,
    primaryContainer = PurpleBlue.copy(alpha = 0.3f),
    onPrimaryContainer = PurpleBlue,

    secondary = CoralPink,
    onSecondary = Color.White,
    secondaryContainer = CoralPink.copy(alpha = 0.3f),
    onSecondaryContainer = CoralPink,

    tertiary = WarmYellow,
    onTertiary = Color.Black,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,

    error = Error,
    onError = Color.White,
)

@Composable
fun SimplePuzzleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}