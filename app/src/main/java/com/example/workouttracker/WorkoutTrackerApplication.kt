package com.example.workouttracker

import android.app.Application
import com.example.workouttracker.data.AppDatabase

class WorkoutTrackerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
}
