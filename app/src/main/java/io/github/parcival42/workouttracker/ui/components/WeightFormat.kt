package io.github.parcival42.workouttracker.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

private const val GRAMS_PER_KG = 1000

fun formatKg(weightGrams: Int): String {
    val format = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return format.format(weightGrams.toDouble() / GRAMS_PER_KG)
}

/** Parses user input such as "52,5" or "52.5" (kg) into grams; null if invalid or negative. */
fun parseKgToGrams(input: String): Int? {
    val normalized = input.trim().replace(',', '.')
    val kg = normalized.toBigDecimalOrNull() ?: return null
    if (kg < BigDecimal.ZERO || kg > BigDecimal(1000)) return null
    return kg.multiply(BigDecimal(GRAMS_PER_KG)).setScale(0, RoundingMode.HALF_UP).toInt()
}
