package io.github.parcival42.workouttracker.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Device::class, TrainingPlan::class, PlanEntry::class, SetLog::class, CardioLog::class, UserSettings::class,
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun planDao(): PlanDao
    abstract fun setLogDao(): SetLogDao
    abstract fun cardioLogDao(): CardioLogDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        // Training history is the product: never use a destructive migration fallback.
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "workout_tracker.db").build()
    }
}
