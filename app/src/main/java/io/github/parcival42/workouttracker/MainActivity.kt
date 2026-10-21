package io.github.parcival42.workouttracker

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.parcival42.workouttracker.ui.AppNavigation
import io.github.parcival42.workouttracker.ui.theme.AppTheme
import io.github.parcival42.workouttracker.ui.theme.WorkoutTrackerTheme
import io.github.parcival42.workouttracker.ui.theme.colorScheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeSettings = (application as WorkoutTrackerApplication).themeSettings
        applyWindowTheme(themeSettings.theme.value)
        setContent {
            val theme by themeSettings.theme.collectAsStateWithLifecycle()
            // Window and system bars follow the app theme, not the system dark mode.
            LaunchedEffect(theme) { applyWindowTheme(theme) }
            WorkoutTrackerTheme(theme = theme) {
                AppNavigation()
            }
        }
    }

    private fun applyWindowTheme(theme: AppTheme) {
        val style = if (theme.isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        // No dark gap between the starting window and the first Compose frame.
        window.setBackgroundDrawable(theme.colorScheme(this).background.toArgb().toDrawable())
        // The starting window of the next cold start is drawn before any app code runs; from
        // Android 13 the system can be told which window theme to use for it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            splashScreen.setSplashScreenTheme(theme.windowTheme)
        }
    }
}
