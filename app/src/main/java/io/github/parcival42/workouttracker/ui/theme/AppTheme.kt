package io.github.parcival42.workouttracker.ui.theme

import android.content.Context
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.edit
import io.github.parcival42.workouttracker.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Color themes the user can pick in Settings; stored by name. [windowTheme] is the matching
 * window style (themes.xml) for the window background before the first frame.
 */
enum class AppTheme(@StringRes val labelRes: Int, val isDark: Boolean, @StyleRes val windowTheme: Int) {
    // Dark and Light: wallpaper colors (dynamic color) from Android 12, otherwise a static purple scheme.
    DARK(R.string.theme_dark, isDark = true, windowTheme = R.style.Theme_WorkoutTracker),
    LIGHT(R.string.theme_light, isDark = false, windowTheme = R.style.Theme_WorkoutTracker_Light),
    WARM(R.string.theme_warm, isDark = true, windowTheme = R.style.Theme_WorkoutTracker_Warm);

    companion object {
        val DEFAULT = DARK
    }
}

/**
 * The chosen theme, kept in SharedPreferences like a device setting: it is read synchronously at
 * startup (no flash of the wrong theme) and is deliberately not part of the backup.
 */
class ThemeSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        AppTheme.entries.firstOrNull { it.name == preferences.getString(KEY_THEME, null) } ?: AppTheme.DEFAULT
    )
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    fun setTheme(theme: AppTheme) {
        preferences.edit { putString(KEY_THEME, theme.name) }
        _theme.value = theme
    }

    private companion object {
        const val PREFERENCES_NAME = "appearance"
        const val KEY_THEME = "theme"
    }
}
