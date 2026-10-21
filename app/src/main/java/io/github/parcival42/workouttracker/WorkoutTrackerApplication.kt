package io.github.parcival42.workouttracker

import android.app.Application
import io.github.parcival42.workouttracker.data.AppDatabase
import io.github.parcival42.workouttracker.ui.theme.ThemeSettings

class WorkoutTrackerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val themeSettings: ThemeSettings by lazy { ThemeSettings(this) }
}
