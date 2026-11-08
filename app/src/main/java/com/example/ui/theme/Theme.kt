package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EdgeloomDarkColorScheme = darkColorScheme(
    primary = EdgeloomPrimaryLime,
    onPrimary = EdgeloomBackground,
    primaryContainer = EdgeloomPrimaryGlow,
    onPrimaryContainer = EdgeloomPrimaryLime,
    secondary = EdgeloomSignalEmber,
    onSecondary = EdgeloomBackground,
    secondaryContainer = EdgeloomSignalGlow,
    onSecondaryContainer = EdgeloomSignalEmber,
    tertiary = EdgeloomOnlineGreen,
    onTertiary = EdgeloomBackground,
    background = EdgeloomBackground,
    onBackground = EdgeloomTextPrimary,
    surface = EdgeloomSurface,
    onSurface = EdgeloomTextPrimary,
    surfaceVariant = EdgeloomCard,
    onSurfaceVariant = EdgeloomTextMuted,
    outline = EdgeloomBorder,
    error = EdgeloomErrorRed,
    onError = EdgeloomBackground
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = EdgeloomDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = EdgeloomBackground.toArgb()
                window.navigationBarColor = EdgeloomSurface.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
