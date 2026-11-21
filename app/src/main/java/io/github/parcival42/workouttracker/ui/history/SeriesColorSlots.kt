package io.github.parcival42.workouttracker.ui.history

import io.github.parcival42.workouttracker.data.Device

/** Number of history colors (`SeriesColors`), also the most devices shown at once. */
const val SERIES_COLOR_COUNT = 8

/** The device's own history color, or null for automatic (also for an index outside the palette). */
fun Device.fixedColorSlot(): Int? = color?.takeIf { it in 0 until SERIES_COLOR_COUNT }

/** Color slot a selected device is drawn with: its own color wins over the stored automatic slot. */
fun shownSlot(device: Device?, storedSlot: Int): Int = device?.fixedColorSlot() ?: storedSlot

/**
 * Selection (deviceId -> stored slot) after adding [deviceId]. A device with its own color keeps
 * it, even when another device with its own color has the same one (the user's choice); a shown
 * automatic device on that color moves to a free one. An automatic device gets the first slot no
 * shown device uses. When the selection is full, devices hidden from
 * [visibleIds] (plan filter) make room; if it is still full, nothing changes.
 */
fun addToSelection(
    selection: Map<Long, Int>,
    deviceId: Long,
    fixedSlots: Map<Long, Int>,
    visibleIds: Set<Long>,
): Map<Long, Int> {
    val candidates = if (selection.size < SERIES_COLOR_COUNT) selection else selection.filterKeys { it in visibleIds }
    if (candidates.size >= SERIES_COLOR_COUNT) return selection
    val shown = candidates.map { (id, slot) -> fixedSlots[id] ?: slot }.toSet()
    val fixed = fixedSlots[deviceId]
    // With fewer than SERIES_COLOR_COUNT candidates a free slot always exists.
    if (fixed == null) return candidates + (deviceId to firstFreeSlot(shown))
    // An automatic device on the new device's own color moves to a free one: two equal
    // colors are only shown when the user chose them.
    val result = candidates.toMutableMap()
    result[deviceId] = fixed
    candidates.forEach { (id, slot) ->
        if (id !in fixedSlots && slot == fixed) {
            result[id] = firstFreeSlot(result.entries.filter { it.key != id }.map { fixedSlots[it.key] ?: it.value })
        }
    }
    return result
}

private fun firstFreeSlot(used: Collection<Int>): Int = (0 until SERIES_COLOR_COUNT).first { it !in used }
