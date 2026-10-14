package com.example.workouttracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApplication
import com.example.workouttracker.data.CardioLogDao
import com.example.workouttracker.data.Device
import com.example.workouttracker.data.DeviceDao
import com.example.workouttracker.data.DeviceType
import com.example.workouttracker.data.SetLogDao
import com.example.workouttracker.ui.components.secondsToMinutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class HistoryRange(val months: Long?) {
    ONE_MONTH(1),
    THREE_MONTHS(3),
    ONE_YEAR(12),
    ALL(null),
}

data class HistoryDevice(val device: Device, val colorSlot: Int?)

/**
 * One logged set or cardio session. [value] is in chart units: grams for strength,
 * whole minutes for cardio. [reps] is null for cardio.
 */
data class HistoryEntry(
    val id: Long,
    val deviceId: Long,
    val performedAt: Long,
    val value: Int,
    val reps: Int?,
)

data class DayEntries(val date: LocalDate, val entries: List<HistoryEntry>)

data class HistorySeries(val deviceId: Long, val deviceName: String, val colorSlot: Int, val days: List<DayEntries>)

data class HistoryUiState(
    /** Strength and cardio are never mixed in one chart: they have different units. */
    val mode: DeviceType = DeviceType.STRENGTH,
    /** Device types that have history; the mode switch is only shown for more than one. */
    val availableModes: List<DeviceType> = emptyList(),
    /** Devices of the current mode. */
    val devices: List<HistoryDevice> = emptyList(),
    val range: HistoryRange = HistoryRange.THREE_MONTHS,
    /** All dates with at least one entry of a selected device, ascending. */
    val dates: List<LocalDate> = emptyList(),
    val series: List<HistorySeries> = emptyList(),
    val canSelectMore: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    deviceDao: DeviceDao,
    private val setLogDao: SetLogDao,
    private val cardioLogDao: CardioLogDao,
) : ViewModel() {

    private val range = MutableStateFlow(HistoryRange.THREE_MONTHS)

    // null until the user picks a mode; then the mode of the most recent training is shown.
    private val mode = MutableStateFlow<DeviceType?>(null)

    // Per mode: deviceId -> color slot. A slot stays with its device while selected, so
    // toggling other devices never repaints the ones already shown.
    private val selections = MutableStateFlow<Map<DeviceType, Map<Long, Int>>>(emptyMap())

    private val entries = range.flatMapLatest { range ->
        val since = range.startMillis()
        combine(setLogDao.observeSince(since), cardioLogDao.observeSince(since)) { sets, sessions ->
            mapOf(
                DeviceType.STRENGTH to sets.map { HistoryEntry(it.id, it.deviceId, it.performedAt, it.weightGrams, it.reps) },
                DeviceType.CARDIO to sessions.map {
                    HistoryEntry(it.id, it.deviceId, it.performedAt, secondsToMinutes(it.durationSeconds), null)
                },
            )
        }
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        deviceDao.observeWithHistory(),
        range,
        mode,
        selections,
        entries,
    ) { devices, range, mode, selections, entriesByType ->
        val availableModes = DeviceType.entries.filter { type -> devices.any { it.type == type } }
        val mostRecentMode = entriesByType
            .mapNotNull { (type, entries) -> entries.lastOrNull()?.let { type to it.performedAt } }
            .maxByOrNull { it.second }?.first
        val effectiveMode = mode?.takeIf { it in availableModes }
            ?: mostRecentMode?.takeIf { it in availableModes }
            ?: availableModes.firstOrNull()
            ?: DeviceType.STRENGTH

        val modeDevices = devices.filter { it.type == effectiveMode }
        val modeEntries = entriesByType[effectiveMode].orEmpty()

        // Default: show the most recently trained device until the user picks something.
        val defaultDeviceId = modeEntries.lastOrNull()?.deviceId ?: modeDevices.firstOrNull()?.id
        val effectiveSelection = selections[effectiveMode]
            ?: defaultDeviceId?.let { mapOf(it to 0) }
            ?: emptyMap()

        val selectedEntriesByDevice = modeEntries
            .filter { it.deviceId in effectiveSelection }
            .groupBy { it.deviceId }
        val zone = ZoneId.systemDefault()
        val series = modeDevices.mapNotNull { device ->
            val slot = effectiveSelection[device.id] ?: return@mapNotNull null
            val days = selectedEntriesByDevice[device.id].orEmpty()
                .sortedWith(compareBy({ it.performedAt }, { it.id }))
                .groupBy { Instant.ofEpochMilli(it.performedAt).atZone(zone).toLocalDate() }
                .map { (date, dayEntries) -> DayEntries(date, dayEntries) }
            HistorySeries(device.id, device.name, slot, days)
        }

        HistoryUiState(
            mode = effectiveMode,
            availableModes = availableModes,
            devices = modeDevices.map { HistoryDevice(it, effectiveSelection[it.id]) },
            range = range,
            dates = series.flatMap { s -> s.days.map { it.date } }.distinct().sorted(),
            series = series,
            canSelectMore = effectiveSelection.size < MAX_SERIES,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setRange(newRange: HistoryRange) {
        range.value = newRange
    }

    fun setMode(newMode: DeviceType) {
        mode.value = newMode
    }

    fun toggleDevice(deviceId: Long) {
        val state = uiState.value
        val current = selections.value[state.mode]
            ?: state.devices.filter { it.colorSlot != null }
                .associate { it.device.id to it.colorSlot!! }
        val updated = if (deviceId in current) {
            current - deviceId
        } else {
            val freeSlot = (0 until MAX_SERIES).firstOrNull { it !in current.values }
            if (freeSlot == null) current else current + (deviceId to freeSlot)
        }
        // Pin the mode too, so the view doesn't jump to another mode when new training is logged.
        mode.value = state.mode
        selections.update { it + (state.mode to updated) }
    }

    private fun HistoryRange.startMillis(): Long {
        val months = months ?: return 0L
        return LocalDate.now().minusMonths(months)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    companion object {
        const val MAX_SERIES = 8

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                HistoryViewModel(app.database.deviceDao(), app.database.setLogDao(), app.database.cardioLogDao())
            }
        }
    }
}
