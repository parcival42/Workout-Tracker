package io.github.parcival42.workouttracker.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

// Distances are stored in meters and shown in km, exactly (up to meter precision).

const val METERS_PER_KM = 1000
private const val MAX_DISTANCE_KM = 1000
const val MAX_DISTANCE_METERS = MAX_DISTANCE_KM * METERS_PER_KM

fun formatKm(meters: Int): String {
    val format = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 3
        // "1.000" would parse back as one km in German.
        isGroupingUsed = false
    }
    return format.format(meters.toDouble() / METERS_PER_KM)
}

/** Speed in meters per hour as km/h with one decimal, e.g. "8.5". */
fun formatKmh(metersPerHour: Int): String {
    val format = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    return format.format(metersPerHour.toDouble() / METERS_PER_KM)
}

/** Parses user input such as "2,4" or "2.4" (km) into meters; null if invalid, zero or negative. */
fun parseKmToMeters(input: String): Int? {
    val normalized = input.trim().replace(',', '.')
    val km = normalized.toBigDecimalOrNull() ?: return null
    if (km > BigDecimal(MAX_DISTANCE_KM)) return null
    val meters = km.multiply(BigDecimal(METERS_PER_KM)).setScale(0, RoundingMode.HALF_UP).toInt()
    return meters.takeIf { it > 0 }
}
