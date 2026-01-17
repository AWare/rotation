package app.rotatescreen.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// NGE theme colors
private val NGEPurple = Color(0xFFB565FF)
private val NGEDark = Color(0xFF0D0415)
private val NGEGreen = Color(0xFF00FF41)
private val NGEOrange = Color(0xFFFF8C00)

private val DarkColorScheme = darkColorScheme(
    primary = NGEPurple,
    secondary = NGEGreen,
    tertiary = NGEOrange,
    background = NGEDark
)

private val LightColorScheme = lightColorScheme(
    primary = NGEPurple,
    secondary = NGEGreen,
    tertiary = NGEOrange
)

@Composable
fun RotationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar color to match NGE theme
            window.statusBarColor = NGEDark.toArgb()
            // Set navigation bar color to match
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.navigationBarColor = NGEDark.toArgb()
            }
            // Use light icons (white) for dark background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
