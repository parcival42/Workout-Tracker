package io.github.parcival42.workouttracker.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

private val DarkFallbackColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightFallbackColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val WarmColorScheme = darkColorScheme(
    primary = WarmPrimary,
    onPrimary = WarmOnPrimary,
    primaryContainer = WarmPrimaryContainer,
    onPrimaryContainer = WarmOnPrimaryContainer,
    inversePrimary = WarmInversePrimary,
    secondary = WarmSecondary,
    onSecondary = WarmOnSecondary,
    secondaryContainer = WarmSecondaryContainer,
    onSecondaryContainer = WarmOnSecondaryContainer,
    tertiary = WarmTertiary,
    onTertiary = WarmOnTertiary,
    tertiaryContainer = WarmTertiaryContainer,
    onTertiaryContainer = WarmOnTertiaryContainer,
    background = WarmBackground,
    onBackground = WarmOnSurface,
    surface = WarmBackground,
    onSurface = WarmOnSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = WarmOnSurfaceVariant,
    surfaceTint = WarmPrimary,
    inverseSurface = WarmInverseSurface,
    inverseOnSurface = WarmInverseOnSurface,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    surfaceBright = WarmSurfaceBright,
    surfaceDim = WarmBackground,
    surfaceContainerLowest = WarmSurfaceContainerLowest,
    surfaceContainerLow = WarmSurfaceContainerLow,
    surfaceContainer = WarmSurfaceContainer,
    surfaceContainerHigh = WarmSurfaceContainerHigh,
    surfaceContainerHighest = WarmSurfaceContainerHighest,
)


/** Independent of the system dark mode: the theme alone decides light or dark. */
fun AppTheme.colorScheme(context: Context): ColorScheme = when (this) {
    // Dynamic color needs Android 12 (API 31); older versions use the static schemes.
    AppTheme.DARK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        DarkFallbackColorScheme
    }
    AppTheme.LIGHT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context)
    } else {
        LightFallbackColorScheme
    }
    AppTheme.WARM -> WarmColorScheme
}

/** For the few colors outside the scheme (chart palette and chrome), which have a light and a dark set. */
val ColorScheme.isLight: Boolean get() = background.luminance() > 0.5f

// The theme decides light or dark, regardless of the system setting.
@Composable
fun WorkoutTrackerTheme(
    theme: AppTheme = AppTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = theme.colorScheme(LocalContext.current),
        typography = Typography,
        content = content
    )
}
