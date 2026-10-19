package io.github.parcival42.workouttracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.parcival42.workouttracker.WorkoutTrackerApplication
import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.CardioLogDao
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceDao
import io.github.parcival42.workouttracker.data.DevicePlan
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.PlanDao
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.SetLogDao
import io.github.parcival42.workouttracker.data.TrainingPlan
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
 * One logged set or cardio session. [value] is in chart units: grams for strength, seconds
 * for cardio. [reps] is null for cardio and timed sets; [durationSeconds] is only set for
 * timed strength sets (circuit stations).
 */
data class HistoryEntry(
    val id: Long,
    val deviceId: Long,
    val performedAt: Long,
    val value: Int,
    val reps: Int?,
    val planId: Long?,
    val durationSeconds: Int? = null,
    // Cardio only: the session's distance in meters, if logged.
    val distanceMeters: Int? = null,
)

/** [count] consecutive entries of one day with the same values, e.g. "3 × 10 · 17.5 kg". */
data class EntryGroup(val value: Int, val reps: Int?, val durationSeconds: Int?, val count: Int, val distanceMeters: Int? = null)

data class DayEntries(val date: LocalDate, val entries: List<HistoryEntry>) {
    val groups: List<EntryGroup> = buildList {
        entries.forEach { entry ->
            val last = lastOrNull()
            if (last != null && last.value == entry.value && last.reps == entry.reps &&
                last.durationSeconds == entry.durationSeconds && last.distanceMeters == entry.distanceMeters
            ) {
                set(lastIndex, last.copy(count = last.count + 1))
            } else {
                add(EntryGroup(entry.value, entry.reps, entry.durationSeconds, 1, entry.distanceMeters))
            }
        }
    }
}

/** What the cardio chart shows per day. Distance and speed only count sessions with a distance. */
enum class CardioMetric { DURATION, DISTANCE, SPEED }

data class HistorySeries(val deviceId: Long, val deviceName: String, val colorSlot: Int, val days: List<DayEntries>)

private data class PlanData(val devices: List<Device>, val plans: List<TrainingPlan>, val devicePlans: Set<DevicePlan>)

data class HistoryUiState(
    /** Plans with logged training; the plan filter is only shown if there is at least one. */
    val plans: List<TrainingPlan> = emptyList(),
    /** Selected plan filter; null shows all logs, including those without a plan. */
    val planId: Long? = null,
    /** Strength, cardio and bodyweight are never mixed in one chart: they have different units. */
    val mode: DeviceType = DeviceType.STRENGTH,
    /** Bodyweight mode: what the selected devices count (all of them count the same); null otherwise. */
    val measure: BodyweightMeasure? = null,
    /** Device types that have history; the mode switch is only shown for more than one. */
    val availableModes: List<DeviceType> = emptyList(),
    /** Devices of the current mode. */
    val devices: List<HistoryDevice> = emptyList(),
    val range: HistoryRange = HistoryRange.THREE_MONTHS,
    /** All dates with at least one entry of a selected device, ascending. */
    val dates: List<LocalDate> = emptyList(),
    val series: List<HistorySeries> = emptyList(),
    val canSelectMore: Boolean = true,
    /** Cardio mode only: the metric switch is shown if a device of the mode tracks distance or has distances. */
    val cardioMetric: CardioMetric = CardioMetric.DURATION,
    val hasCardioMetrics: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    deviceDao: DeviceDao,
    planDao: PlanDao,
    private val setLogDao: SetLogDao,
    private val cardioLogDao: CardioLogDao,
) : ViewModel() {

    private val range = MutableStateFlow(HistoryRange.THREE_MONTHS)

    private val planFilter = MutableStateFlow<Long?>(null)

    // null until the user picks a mode; then the mode of the most recent training is shown.
    private val mode = MutableStateFlow<DeviceType?>(null)

    // Per mode: deviceId -> color slot. A slot stays with its device while selected, so
    // toggling other devices never repaints the ones already shown.
    private val selections = MutableStateFlow<Map<DeviceType, Map<Long, Int>>>(emptyMap())

    private val logs = range.flatMapLatest { range ->
        val since = range.startMillis()
        combine(setLogDao.observeSince(since), cardioLogDao.observeSince(since), ::Pair)
    }

    private val planData = combine(
        deviceDao.observeWithHistory(),
        planDao.observePlans(),
        planDao.observeLoggedDevicePlans(),
    ) { devices, plans, devicePlans -> PlanData(devices, plans, devicePlans.toSet()) }

    private val cardioMetric = MutableStateFlow(CardioMetric.DURATION)

    private val filters = combine(range, planFilter, cardioMetric, ::Triple)

    val uiState: StateFlow<HistoryUiState> = combine(
        planData,
        filters,
        mode,
        selections,
        logs,
    ) { planData, (range, planFilter, cardioMetric), mode, selections, (sets, sessions) ->
        val allEntriesByType = entriesByType(planData.devices, sets, sessions)
        val plansWithHistory = planData.plans.filter { plan -> planData.devicePlans.any { it.planId == plan.id } }
        // Falls back to all plans if the selected plan was deleted.
        val effectivePlanId = planFilter?.takeIf { id -> plansWithHistory.any { it.id == id } }
        val devices = if (effectivePlanId == null) {
            planData.devices
        } else {
            planData.devices.filter { DevicePlan(it.id, effectivePlanId) in planData.devicePlans }
        }
        val entriesByType = if (effectivePlanId == null) {
            allEntriesByType
        } else {
            allEntriesByType.mapValues { (_, entries) -> entries.filter { it.planId == effectivePlanId } }
        }

        val availableModes = DeviceType.entries.filter { type -> devices.any { it.type == type } }
        val mostRecentMode = entriesByType
            .mapNotNull { (type, entries) -> entries.lastOrNull()?.let { type to it.performedAt } }
            .maxByOrNull { it.second }?.first
        val effectiveMode = mode?.takeIf { it in availableModes }
            ?: mostRecentMode?.takeIf { it in availableModes }
            ?: availableModes.firstOrNull()
            ?: DeviceType.STRENGTH

        val modeDevices = devices.filter { it.type == effectiveMode }
        val hasCardioMetrics = effectiveMode == DeviceType.CARDIO && (
            modeDevices.any { it.tracksDistance } || entriesByType[DeviceType.CARDIO].orEmpty().any { it.distanceMeters != null }
            )
        val effectiveMetric = if (hasCardioMetrics) cardioMetric else CardioMetric.DURATION
        // Distance and speed leave out sessions without a distance, so days without one disappear.
        val modeEntries = entriesByType[effectiveMode].orEmpty().let { entries ->
            if (effectiveMetric == CardioMetric.DURATION) entries else entries.filter { it.distanceMeters != null }
        }

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
            plans = plansWithHistory,
            planId = effectivePlanId,
            mode = effectiveMode,
            measure = modeDevices.firstOrNull { it.id in effectiveSelection && it.type == DeviceType.BODYWEIGHT }?.measure,
            availableModes = availableModes,
            devices = modeDevices.map { HistoryDevice(it, effectiveSelection[it.id]) },
            range = range,
            dates = series.flatMap { s -> s.days.map { it.date } }.distinct().sorted(),
            series = series,
            canSelectMore = effectiveSelection.keys.count { id -> modeDevices.any { it.id == id } } < MAX_SERIES,
            cardioMetric = effectiveMetric,
            hasCardioMetrics = hasCardioMetrics,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setRange(newRange: HistoryRange) {
        range.value = newRange
    }

    fun setPlanFilter(planId: Long?) {
        planFilter.value = planId
    }

    fun setCardioMetric(metric: CardioMetric) {
        cardioMetric.value = metric
    }

    fun setMode(newMode: DeviceType) {
        mode.value = newMode
    }

    fun toggleDevice(deviceId: Long) {
        val state = uiState.value
        var current = selections.value[state.mode]
            ?: state.devices.filter { it.colorSlot != null }
                .associate { it.device.id to it.colorSlot!! }
        // Bodyweight reps and times have different units: picking a device of the other measure
        // starts a new selection.
        val measure = state.devices.firstOrNull { it.device.id == deviceId }?.device?.measure
        if (state.mode == DeviceType.BODYWEIGHT) {
            current = current.filterKeys { id -> state.devices.firstOrNull { it.device.id == id }?.device?.measure == measure }
        }
        val updated = if (deviceId in current) {
            current - deviceId
        } else {
            // Devices hidden by the plan filter keep their slot unless all slots are taken.
            val visibleIds = state.devices.mapTo(HashSet()) { it.device.id }
            val candidates = if (current.size < MAX_SERIES) current else current.filterKeys { it in visibleIds }
            val freeSlot = (0 until MAX_SERIES).firstOrNull { it !in candidates.values }
            if (freeSlot == null) current else candidates + (deviceId to freeSlot)
        }
        // Pin the mode too, so the view doesn't jump to another mode when new training is logged.
        mode.value = state.mode
        selections.update { it + (state.mode to updated) }
    }

    /**
     * Chart entries per mode. Strength values are weights; bodyweight values are reps or seconds,
     * depending on the device's measure (sets without that value, e.g. a circuit station without
     * reps, are left out); cardio values are seconds.
     */
    private fun entriesByType(devices: List<Device>, sets: List<SetLog>, sessions: List<CardioLog>): Map<DeviceType, List<HistoryEntry>> {
        val deviceById = devices.associateBy { it.id }
        val strength = mutableListOf<HistoryEntry>()
        val bodyweight = mutableListOf<HistoryEntry>()
        sets.forEach { set ->
            val device = deviceById[set.deviceId] ?: return@forEach
            when (device.type) {
                DeviceType.STRENGTH -> strength += HistoryEntry(
                    set.id, set.deviceId, set.performedAt, set.weightGrams, set.reps, set.planId, set.durationSeconds,
                )
                DeviceType.BODYWEIGHT -> {
                    val value = if (device.measure == BodyweightMeasure.TIME) set.durationSeconds else set.reps
                    if (value != null) bodyweight += HistoryEntry(set.id, set.deviceId, set.performedAt, value, null, set.planId)
                }
                DeviceType.CARDIO -> Unit
            }
        }
        return mapOf(
            DeviceType.STRENGTH to strength,
            DeviceType.CARDIO to sessions.map {
                HistoryEntry(it.id, it.deviceId, it.performedAt, it.durationSeconds, null, it.planId, distanceMeters = it.distanceMeters)
            },
            DeviceType.BODYWEIGHT to bodyweight,
        )
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
                HistoryViewModel(
                    app.database.deviceDao(),
                    app.database.planDao(),
                    app.database.setLogDao(),
                    app.database.cardioLogDao(),
                )
            }
        }
    }
}
