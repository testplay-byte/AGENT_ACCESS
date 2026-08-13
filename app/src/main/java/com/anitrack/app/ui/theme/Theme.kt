package com.anitrack.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light Theme Colors - Sky Blue based
private val LightColorScheme = lightColorScheme(
    primary = SkyBlue600,
    onPrimary = Color.White,
    primaryContainer = SkyBlue100,
    onPrimaryContainer = SkyBlue900,
    
    secondary = Aqua500,
    onSecondary = Color.White,
    secondaryContainer = Aqua100,
    onSecondaryContainer = Aqua900,
    
    tertiary = CoralAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7E6),
    onTertiaryContainer = Color(0xFF68100B),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    background = BackgroundLight,
    onBackground = Color(0xFF1C1B1F),
    
    surface = SurfaceLight,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = SkyBlue100,
    onSurfaceVariant = Color(0xFF49454F),
    
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF5EFF7),
    
    scrim = Color.Black.copy(alpha = 0.32f)
)

// Dark Theme Colors - Deep blue based
private val DarkColorScheme = darkColorScheme(
    primary = SkyBlue400,
    onPrimary = SkyBlue900,
    primaryContainer = SkyBlue800,
    onPrimaryContainer = SkyBlue200,
    
    secondary = Aqua400,
    onSecondary = Aqua900,
    secondaryContainer = Aqua800,
    onSecondaryContainer = Aqua200,
    
    tertiary = CoralAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF93300B),
    onTertiaryContainer = Color(0xFFFFE7E6),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    background = BackgroundDark,
    onBackground = Color(0xFFE6E1E5),
    
    surface = SurfaceDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    
    scrim = Color.Black.copy(alpha = 0.32f)
)

@Composable
fun AniTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Disabled to use our custom sky blue theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Capture context and view OUTSIDE of SideEffect (composable context required)
    val context = LocalContext.current
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            // SAFE: Only access window if context is an Activity
            val activity = context as? Activity
            if (activity != null) {
                activity.window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AniTrackTypography,
        content = content
    )
}
