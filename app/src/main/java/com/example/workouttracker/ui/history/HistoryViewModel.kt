package com.example.workouttracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApplication
import com.example.workouttracker.data.Device
import com.example.workouttracker.data.DeviceDao
import com.example.workouttracker.data.SetLog
import com.example.workouttracker.data.SetLogDao
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

data class DaySets(val date: LocalDate, val sets: List<SetLog>)

data class HistorySeries(val deviceId: Long, val deviceName: String, val colorSlot: Int, val days: List<DaySets>)

data class HistoryUiState(
    val devices: List<HistoryDevice> = emptyList(),
    val range: HistoryRange = HistoryRange.THREE_MONTHS,
    /** All dates with at least one set of a selected device, ascending. */
    val dates: List<LocalDate> = emptyList(),
    val series: List<HistorySeries> = emptyList(),
    val canSelectMore: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    deviceDao: DeviceDao,
    private val setLogDao: SetLogDao,
) : ViewModel() {

    private val range = MutableStateFlow(HistoryRange.THREE_MONTHS)

    // deviceId -> color slot. A slot stays with its device while selected, so toggling
    // other devices never repaints the ones already shown.
    private val selection = MutableStateFlow<Map<Long, Int>?>(null)

    private val sets = range.flatMapLatest { setLogDao.observeSince(it.startMillis()) }

    val uiState: StateFlow<HistoryUiState> = combine(
        deviceDao.observeWithHistory(),
        range,
        selection,
        sets,
    ) { devices, range, selection, sets ->
        // Default: show the most recently trained device until the user picks something.
        val defaultDeviceId = sets.lastOrNull()?.deviceId ?: devices.firstOrNull()?.id
        val effectiveSelection = selection
            ?: defaultDeviceId?.let { mapOf(it to 0) }
            ?: emptyMap()

        val selectedSetsByDevice = sets
            .filter { it.deviceId in effectiveSelection }
            .groupBy { it.deviceId }
        val zone = ZoneId.systemDefault()
        val series = devices.mapNotNull { device ->
            val slot = effectiveSelection[device.id] ?: return@mapNotNull null
            val days = selectedSetsByDevice[device.id].orEmpty()
                .sortedWith(compareBy({ it.performedAt }, { it.id }))
                .groupBy { Instant.ofEpochMilli(it.performedAt).atZone(zone).toLocalDate() }
                .map { (date, daySets) -> DaySets(date, daySets) }
            HistorySeries(device.id, device.name, slot, days)
        }

        HistoryUiState(
            devices = devices.map { HistoryDevice(it, effectiveSelection[it.id]) },
            range = range,
            dates = series.flatMap { s -> s.days.map { it.date } }.distinct().sorted(),
            series = series,
            canSelectMore = effectiveSelection.size < MAX_SERIES,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setRange(newRange: HistoryRange) {
        range.value = newRange
    }

    fun toggleDevice(deviceId: Long) {
        val current = selection.value
            ?: uiState.value.devices.filter { it.colorSlot != null }
                .associate { it.device.id to it.colorSlot!! }
        selection.update {
            if (deviceId in current) {
                current - deviceId
            } else {
                val freeSlot = (0 until MAX_SERIES).firstOrNull { it !in current.values }
                if (freeSlot == null) current else current + (deviceId to freeSlot)
            }
        }
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
                HistoryViewModel(app.database.deviceDao(), app.database.setLogDao())
            }
        }
    }
}
