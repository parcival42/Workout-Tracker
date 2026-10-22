package io.github.parcival42.workouttracker.ui.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.parcival42.workouttracker.WorkoutTrackerApplication
import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.CardioLogDao
import io.github.parcival42.workouttracker.data.CircuitSettings
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceDao
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.PlanDao
import io.github.parcival42.workouttracker.data.PlanEntry
import io.github.parcival42.workouttracker.data.PlanEntryItem
import io.github.parcival42.workouttracker.data.PlanType
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.SetLogDao
import io.github.parcival42.workouttracker.data.UserSettings
import io.github.parcival42.workouttracker.data.UserSettingsDao
import io.github.parcival42.workouttracker.data.circuitSettings
import io.github.parcival42.workouttracker.data.withCircuitSettings
import io.github.parcival42.workouttracker.ui.circuit.CircuitController
import io.github.parcival42.workouttracker.ui.circuit.CircuitEvent
import io.github.parcival42.workouttracker.ui.circuit.CircuitPhase
import io.github.parcival42.workouttracker.ui.circuit.CircuitPreview
import io.github.parcival42.workouttracker.ui.circuit.CircuitStation
import io.github.parcival42.workouttracker.ui.circuit.CircuitUiState
import io.github.parcival42.workouttracker.ui.circuit.buildCircuitPhases
import io.github.parcival42.workouttracker.ui.components.MAX_DISTANCE_METERS
import io.github.parcival42.workouttracker.ui.components.MAX_DURATION_SECONDS
import io.github.parcival42.workouttracker.ui.components.TARGET_SETS_RANGE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

sealed interface WorkoutItem {
    val entryId: Long
    val deviceId: Long
    val deviceName: String
    val isComplete: Boolean
    val hasSessionLogs: Boolean
    // Circuit plans only: the station time of this device; null in normal plans.
    val stationSeconds: Int?
}

data class StrengthItem(
    override val entryId: Long,
    override val deviceId: Long,
    override val deviceName: String,
    val targetSets: Int,
    val todaySets: List<SetLog>,
    val weightGrams: Int,
    val minWeightGrams: Int,
    val weightStepGrams: Int,
    val reps: Int,
    override val stationSeconds: Int? = null,
    // Bodyweight exercises: what counts; null for strength devices with a weight.
    val measure: BodyweightMeasure? = null,
    // Bodyweight measured by time: duration per set.
    val durationSeconds: Int = 0,
) : WorkoutItem {
    val isBodyweight: Boolean get() = measure != null
    // Circuit stations are timed: they log weight and duration, no reps.
    val isTimed: Boolean get() = stationSeconds != null
    val openSets: Int get() = (targetSets - todaySets.size).coerceAtLeast(0)
    override val isComplete: Boolean get() = openSets == 0
    override val hasSessionLogs: Boolean get() = todaySets.isNotEmpty()
}

data class CardioItem(
    override val entryId: Long,
    override val deviceId: Long,
    override val deviceName: String,
    val todaySessions: List<CardioLog>,
    // Exact duration of a session, as saved in the plan.
    val durationSeconds: Int,
    // One per round in a circuit plan, otherwise one.
    val targetSessions: Int = 1,
    override val stationSeconds: Int? = null,
    val tracksDistance: Boolean = false,
    // Distance of the next session: the last logged distance, or none. Always null without tracksDistance.
    val distanceMeters: Int? = null,
) : WorkoutItem {
    override val isComplete: Boolean get() = todaySessions.size >= targetSessions
    override val hasSessionLogs: Boolean get() = todaySessions.isNotEmpty()
}

data class WorkoutUiState(
    val planName: String = "",
    val items: List<WorkoutItem> = emptyList(),
    // Only set for circuit plans; normal plans show nothing of the circuit.
    val circuitPreview: CircuitPreview? = null,
    val circuitSettings: CircuitSettings? = null,
    val circuit: CircuitUiState? = null,
) {
    val isComplete: Boolean get() = items.isNotEmpty() && items.all { it.isComplete }
}

// [lastDistance]: cardio only, the distance of the last session that has one.
private data class LogsByDevice<T>(
    val today: Map<Long, List<T>>,
    val lastDistance: Map<Long, Int> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val planId: Long,
    private val planDao: PlanDao,
    private val setLogDao: SetLogDao,
    private val cardioLogDao: CardioLogDao,
    private val deviceDao: DeviceDao,
    private val userSettingsDao: UserSettingsDao,
    // Monotonic clock for the circuit timer.
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {

    // The set counter resets per calendar day (or on a restart); "today" is fixed when the screen opens.
    private val startOfToday: Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // Edited distances of the next cardio session (null = cleared on purpose). The distance is a
    // result, not a plan value, so it is not saved in the plan; logging the session drops the edit.
    private val distanceDrafts = MutableStateFlow<Map<Long, Int?>>(emptyMap())

    private val plan = planDao.observePlan(planId)

    // Sets count from the start of the current session: midnight, or a later restart of the plan.
    private val sessionStart = plan
        .map { maxOf(startOfToday, it?.sessionStartedAt ?: 0L) }
        .distinctUntilChanged()

    // Session start before the last restart, for undoing the restart.
    private var previousSessionStartedAt: Long? = null

    // Removed entry and the order before, for undoing the removal.
    private var removedEntry: Pair<PlanEntry, List<Long>>? = null

    /** Active devices, for adding them to the plan. */
    val devices: StateFlow<List<Device>> = deviceDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // The session's logs count per plan.
    private val strengthLogs = sessionStart
        .flatMapLatest { since -> setLogDao.observeSinceInPlan(since, planId) }
        .map { today -> LogsByDevice(today = today.groupBy { it.deviceId }) }

    private val cardioLogs = combine(
        sessionStart.flatMapLatest { since -> cardioLogDao.observeSinceInPlan(since, planId) },
        cardioLogDao.observeLastDistancePerDeviceInPlan(planId),
        cardioLogDao.observeLastDistancePerDevice(),
    ) { today, lastDistanceInPlan, lastDistanceAnywhere ->
        LogsByDevice(
            today = today.groupBy { it.deviceId },
            lastDistance = (lastDistanceAnywhere + lastDistanceInPlan)
                .mapNotNull { session -> session.distanceMeters?.let { session.deviceId to it } }
                .toMap(),
        )
    }

    // Cold, so the circuit reads live values even while no screen collects [uiState].
    private val workoutState: Flow<WorkoutUiState> = combine(
        plan,
        planDao.observeEntries(planId),
        strengthLogs,
        cardioLogs,
        distanceDrafts,
    ) { plan, entries, strengthLogs, cardioLogs, distanceDrafts ->
        val circuitSettings = plan?.takeIf { it.type == PlanType.CIRCUIT }?.circuitSettings
        WorkoutUiState(
            planName = plan?.name.orEmpty(),
            circuitSettings = circuitSettings,
            circuitPreview = circuitSettings?.let { settings ->
                CircuitPreview(
                    stations = entries.size,
                    rounds = settings.rounds,
                    totalSeconds = circuitPhases(circuitStations(entries, settings), settings).sumOf { it.durationSeconds },
                )
            },
            items = entries.map { entry ->
                when (entry.deviceType) {
                    DeviceType.STRENGTH, DeviceType.BODYWEIGHT -> {
                        val isBodyweight = entry.deviceType == DeviceType.BODYWEIGHT
                        StrengthItem(
                            entryId = entry.id,
                            deviceId = entry.deviceId,
                            deviceName = entry.deviceName,
                            targetSets = circuitSettings?.rounds ?: entry.targetSets,
                            todaySets = strengthLogs.today[entry.deviceId].orEmpty(),
                            weightGrams = if (isBodyweight) 0 else entry.weightGrams,
                            minWeightGrams = entry.minWeightGrams,
                            weightStepGrams = entry.weightStepGrams,
                            reps = entry.targetReps,
                            stationSeconds = circuitSettings?.let { entry.stationSeconds ?: it.stationSeconds },
                            measure = entry.measure.takeIf { isBodyweight },
                            durationSeconds = entry.targetDurationSeconds,
                        )
                    }
                    DeviceType.CARDIO -> CardioItem(
                        entryId = entry.id,
                        deviceId = entry.deviceId,
                        deviceName = entry.deviceName,
                        todaySessions = cardioLogs.today[entry.deviceId].orEmpty(),
                        durationSeconds = entry.targetDurationSeconds,
                        targetSessions = circuitSettings?.rounds ?: 1,
                        stationSeconds = circuitSettings?.let { entry.stationSeconds ?: it.stationSeconds },
                        tracksDistance = entry.tracksDistance,
                        distanceMeters = when {
                            !entry.tracksDistance -> null
                            entry.deviceId in distanceDrafts -> distanceDrafts[entry.deviceId]
                            else -> cardioLogs.lastDistance[entry.deviceId]
                        },
                    )
                }
            },
        )
    }

    // Only started for circuit plans.
    private val circuit = CircuitController(viewModelScope, clock, onStationDone = ::logStation)
    private var circuitStarting = false

    val circuitEvents: SharedFlow<CircuitEvent> get() = circuit.events

    val uiState: StateFlow<WorkoutUiState> = combine(workoutState, circuit.state) { state, circuitState ->
        state.copy(circuit = circuitState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    /** Saves the device's values in the plan; every following set uses them. */
    fun updateValues(item: StrengthItem, weightGrams: Int, sets: Int, reps: Int, durationSeconds: Int) {
        viewModelScope.launch {
            planDao.updateValues(
                entryId = item.entryId,
                weightGrams = if (item.isBodyweight) 0 else weightGrams.coerceIn(0, MAX_WEIGHT_GRAMS),
                targetSets = sets.coerceIn(TARGET_SETS_RANGE),
                targetReps = reps.coerceIn(1, MAX_REPS),
                targetDurationSeconds = durationSeconds.coerceIn(1, MAX_DURATION_SECONDS),
            )
        }
    }

    /** Saves the session duration in the plan; [distanceMeters] (null = none) only applies to the next session. */
    fun updateNextSession(item: CardioItem, seconds: Int, distanceMeters: Int?) {
        if (item.tracksDistance) {
            distanceDrafts.update { it + (item.deviceId to distanceMeters?.coerceIn(1, MAX_DISTANCE_METERS)) }
        }
        viewModelScope.launch { planDao.updateDuration(item.entryId, seconds.coerceIn(1, MAX_DURATION_SECONDS)) }
    }

    fun completeDevice(item: StrengthItem) = logSets(item, item.openSets)

    fun completeOneSet(item: StrengthItem) = logSets(item, 1)

    fun undoLastSet(item: StrengthItem) {
        viewModelScope.launch { setLogDao.deleteLastSinceInPlan(item.deviceId, planId, sessionStart.first()) }
    }

    fun updateSet(set: SetLog, weightGrams: Int, reps: Int?, durationSeconds: Int?) {
        val updated = set.copy(
            weightGrams = weightGrams.coerceIn(0, MAX_WEIGHT_GRAMS),
            reps = reps?.coerceIn(1, MAX_REPS),
            durationSeconds = durationSeconds?.coerceIn(1, MAX_DURATION_SECONDS),
        )
        viewModelScope.launch { setLogDao.update(updated) }
    }

    fun deleteSet(set: SetLog) {
        viewModelScope.launch { setLogDao.delete(set) }
    }

    /** [onLogged] gets the stored session (with its id), e.g. to offer correcting the distance. */
    fun logSession(item: CardioItem, onLogged: (CardioLog) -> Unit = {}) {
        val session = CardioLog(
            deviceId = item.deviceId,
            performedAt = System.currentTimeMillis(),
            durationSeconds = item.durationSeconds,
            planId = planId,
            distanceMeters = item.distanceMeters.takeIf { item.tracksDistance },
        )
        viewModelScope.launch {
            val id = cardioLogDao.insert(session)
            // The logged distance is now the last one, so the edit is no longer needed.
            distanceDrafts.update { it - item.deviceId }
            onLogged(session.copy(id = id))
        }
    }

    fun undoLastSession(item: CardioItem) {
        // Pin the shown distance so removing the session does not change it.
        if (item.tracksDistance) distanceDrafts.update { it + (item.deviceId to item.distanceMeters) }
        viewModelScope.launch { cardioLogDao.deleteLastSinceInPlan(item.deviceId, planId, sessionStart.first()) }
    }

    fun updateSession(session: CardioLog, seconds: Int, distanceMeters: Int?) {
        val updated = session.copy(
            durationSeconds = seconds.coerceIn(1, MAX_DURATION_SECONDS),
            distanceMeters = distanceMeters?.coerceIn(1, MAX_DISTANCE_METERS),
        )
        viewModelScope.launch { cardioLogDao.update(updated) }
    }

    fun deleteSession(session: CardioLog) {
        viewModelScope.launch { cardioLogDao.delete(session) }
    }

    /** Appends the devices in this order, starting where the user left off with each of them. */
    fun addDevices(deviceIds: List<Long>) {
        if (deviceIds.isEmpty()) return
        viewModelScope.launch {
            val defaults = userSettingsDao.get() ?: UserSettings()
            val devicesById = deviceDao.observeActive().first().associateBy { it.id }
            val lastSets = setLogDao.observeLastSetPerDevice().first().associateBy { it.deviceId }
            val lastSessions = cardioLogDao.observeLastPerDevice().first().associateBy { it.deviceId }
            val entries = deviceIds.mapNotNull { devicesById[it] }.map { device ->
                newPlanEntry(device, defaults, lastSets[device.id], lastSessions[device.id])
            }
            planDao.addEntries(planId, entries)
        }
    }

    /** Removes the device from the plan; its logs stay. Undo with [undoRemoveEntry]. */
    fun removeEntry(item: WorkoutItem) {
        viewModelScope.launch {
            val entry = planDao.getEntry(item.entryId) ?: return@launch
            removedEntry = entry to planDao.observeEntries(planId).first().map { it.id }
            planDao.deleteEntry(entry.id)
        }
    }

    fun undoRemoveEntry() {
        val (entry, order) = removedEntry ?: return
        removedEntry = null
        viewModelScope.launch { planDao.restoreEntry(entry, order) }
    }

    fun saveOrder(orderedEntryIds: List<Long>) {
        viewModelScope.launch { planDao.reorderEntries(orderedEntryIds) }
    }

    /** Name and, for circuit plans, the circuit settings. */
    fun savePlan(name: String, circuitSettings: CircuitSettings?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = plan.first() ?: return@launch
            val renamed = current.copy(name = trimmed)
            planDao.updatePlan(if (circuitSettings != null) renamed.withCircuitSettings(circuitSettings) else renamed)
        }
    }

    /** The Training tab then falls back to its automatic plan choice. */
    fun deletePlan() {
        viewModelScope.launch { plan.first()?.let { planDao.deletePlan(it) } }
    }

    /** Starts the plan over: only sets logged from now on count for today's progress. */
    fun restartPlan() {
        viewModelScope.launch {
            previousSessionStartedAt = plan.first()?.sessionStartedAt
            planDao.setSessionStart(planId, System.currentTimeMillis())
        }
    }

    fun undoRestartPlan() {
        viewModelScope.launch { planDao.setSessionStart(planId, previousSessionStartedAt) }
    }

    fun startCircuit() {
        if (circuit.isRunning || circuitStarting) return
        circuitStarting = true
        viewModelScope.launch {
            try {
                val settings = plan.first()?.takeIf { it.type == PlanType.CIRCUIT }?.circuitSettings ?: return@launch
                val stations = circuitStations(planDao.observeEntries(planId).first(), settings)
                if (stations.isEmpty()) return@launch
                // A further circuit on the same day starts a new session, so its progress counts from zero.
                if (workoutState.first().items.any { it.hasSessionLogs }) {
                    planDao.setSessionStart(planId, System.currentTimeMillis())
                }
                circuit.start(stations, settings.rounds, settings.restSeconds, settings.countdownSeconds)
            } finally {
                circuitStarting = false
            }
        }
    }

    /**
     * Saves a station's weight (strength only, null = unchanged) and time in the plan; a running
     * circuit uses the time from the station's next pass.
     */
    fun updateStation(item: WorkoutItem, weightGrams: Int?, seconds: Int) {
        viewModelScope.launch {
            if (weightGrams != null) planDao.updateWeight(item.entryId, weightGrams.coerceIn(0, MAX_WEIGHT_GRAMS))
            if (seconds == item.stationSeconds) return@launch
            val settings = plan.first()?.takeIf { it.type == PlanType.CIRCUIT }?.circuitSettings ?: return@launch
            // The plan's time is stored as "no own time", so the station follows later plan changes.
            planDao.updateStationSeconds(item.entryId, seconds.takeIf { it != settings.stationSeconds })
            circuit.updateStationSeconds(item.deviceId, seconds)
        }
    }

    fun pauseCircuit() = circuit.pause()

    fun resumeCircuit() = circuit.resume()

    /** Ends the current phase early; a skipped station is not logged. */
    fun skipCircuitPhase() = circuit.skip()

    /** Ends a running circuit or closes a finished one; logged stations stay. */
    fun stopCircuit() = circuit.stop()

    // Logs a finished station: the plan's weight and the station time. Reps can be added later
    // through the set chip.
    private suspend fun logStation(station: CircuitStation, seconds: Int) {
        when (val item = workoutState.first().items.firstOrNull { it.deviceId == station.deviceId }) {
            is StrengthItem -> setLogDao.insertAll(
                listOf(
                    SetLog(
                        deviceId = item.deviceId,
                        performedAt = System.currentTimeMillis(),
                        weightGrams = item.weightGrams,
                        reps = null,
                        planId = planId,
                        durationSeconds = seconds,
                    )
                )
            )
            is CardioItem -> cardioLogDao.insert(
                CardioLog(
                    deviceId = item.deviceId,
                    performedAt = System.currentTimeMillis(),
                    durationSeconds = seconds,
                    planId = planId,
                )
            )
            null -> Unit
        }
    }

    private fun logSets(item: StrengthItem, count: Int) {
        if (count <= 0) return
        val now = System.currentTimeMillis()
        // Offset by 1 ms per set so sets logged in one tap keep a stable order.
        val timed = item.measure == BodyweightMeasure.TIME
        val sets = List(count) { index ->
            SetLog(
                deviceId = item.deviceId,
                performedAt = now + index,
                weightGrams = item.weightGrams,
                reps = if (timed) null else item.reps,
                planId = planId,
                durationSeconds = if (timed) item.durationSeconds else null,
            )
        }
        viewModelScope.launch { setLogDao.insertAll(sets) }
    }

    companion object {
        private const val MAX_WEIGHT_GRAMS = 1_000_000
        private const val MAX_REPS = 999

        fun factory(planId: Long) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                WorkoutViewModel(
                    planId,
                    app.database.planDao(),
                    app.database.setLogDao(),
                    app.database.cardioLogDao(),
                    app.database.deviceDao(),
                    app.database.userSettingsDao(),
                )
            }
        }
    }
}

private fun circuitStations(entries: List<PlanEntryItem>, settings: CircuitSettings): List<CircuitStation> =
    entries.map { CircuitStation(it.deviceId, it.deviceName, it.stationSeconds ?: settings.stationSeconds) }

private fun circuitPhases(stations: List<CircuitStation>, settings: CircuitSettings): List<CircuitPhase> =
    buildCircuitPhases(
        stationSeconds = stations.map { it.seconds },
        rounds = settings.rounds,
        restSeconds = settings.restSeconds,
        countdownSeconds = settings.countdownSeconds,
    )
