package io.github.parcival42.workouttracker

import android.app.Application
import io.github.parcival42.workouttracker.data.AppDatabase

class WorkoutTrackerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
}
