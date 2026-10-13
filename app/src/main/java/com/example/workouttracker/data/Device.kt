package com.example.workouttracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "image_uri") val imageUri: String? = null,
    // Devices are archived instead of deleted so their training history survives.
    val archived: Boolean = false,
    // Used as the prefilled weight until the device has a logged set.
    @ColumnInfo(name = "start_weight_grams", defaultValue = "0") val startWeightGrams: Int = 0,
    @ColumnInfo(name = "weight_step_grams", defaultValue = "2500") val weightStepGrams: Int = DEFAULT_WEIGHT_STEP_GRAMS,
) {
    companion object {
        const val DEFAULT_WEIGHT_STEP_GRAMS = 2_500
    }
}
