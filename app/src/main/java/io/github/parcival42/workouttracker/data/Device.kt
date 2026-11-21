package io.github.parcival42.workouttracker.data

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
    // Lightest setting on the machine; weights are min + k * step. Column keeps its original name.
    @ColumnInfo(name = "start_weight_grams", defaultValue = "0") val minWeightGrams: Int = 0,
    @ColumnInfo(name = "weight_step_grams", defaultValue = "2500") val weightStepGrams: Int = DEFAULT_WEIGHT_STEP_GRAMS,
    // Weight fields are unused for cardio devices. Fixed once the device has logged training.
    @ColumnInfo(defaultValue = "STRENGTH") val type: DeviceType = DeviceType.STRENGTH,
    // Only used for bodyweight devices; fixed like the type once the device has logged training.
    @ColumnInfo(defaultValue = "REPS") val measure: BodyweightMeasure = BodyweightMeasure.REPS,
    // Only used for cardio devices: sessions can carry a distance. Changeable at any time.
    @ColumnInfo(name = "tracks_distance", defaultValue = "0") val tracksDistance: Boolean = false,
    // Icon key (see DeviceIcons); starts as the type's icon. An unknown key shows the type icon.
    @ColumnInfo(defaultValue = "strength") val icon: String = type.defaultIcon,
    // History color as a palette index (see SeriesColors); null = picked automatically.
    // An index outside the palette counts as automatic.
    val color: Int? = null,
) {
    companion object {
        const val DEFAULT_WEIGHT_STEP_GRAMS = 2_500
    }
}
