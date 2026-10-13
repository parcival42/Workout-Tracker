package com.example.workouttracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_plan")
data class TrainingPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // At most one plan is the default; it starts automatically in the Training tab.
    @ColumnInfo(name = "is_default", defaultValue = "0") val isDefault: Boolean = false,
)
