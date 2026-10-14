package io.github.parcival42.workouttracker.ui.components

// Device weights form a grid: minimum + k * step. Stepping snaps onto that grid.

fun stepWeightUp(currentGrams: Int, minGrams: Int, stepGrams: Int): Int {
    if (currentGrams < minGrams) return minGrams
    val k = Math.floorDiv(currentGrams - minGrams, stepGrams) + 1
    return minGrams + k * stepGrams
}

fun stepWeightDown(currentGrams: Int, minGrams: Int, stepGrams: Int): Int {
    if (currentGrams <= minGrams) return minGrams
    val k = Math.ceilDiv(currentGrams - minGrams, stepGrams) - 1
    return minGrams + k * stepGrams
}
