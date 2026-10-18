package io.github.parcival42.workouttracker.ui.quicklog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.parcival42.workouttracker.WorkoutTrackerApplication
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.CardioLogDao
import io.github.parcival42.workouttracker.data.DeviceDao
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.SetLogDao
import io.github.parcival42.workouttracker.data.UserSettings
import io.github.parcival42.workouttracker.data.UserSettingsDao
import io.github.parcival42.workouttracker.ui.components.MAX_DURATION_SECONDS
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A device with the values a quick log is prefilled with. */
data class QuickLogDevice(
    val id: Long,
    val name: String,
    val type: DeviceType,
    val minWeightGrams: Int,
    val weightStepGrams: Int,
    val weightGrams: Int,
    val reps: Int,
    val sets: Int,
    // Exact duration: the last session, or the personal default.
    val durationSeconds: Int,
)

/** What was just logged, so it can be undone. */
sealed interface QuickLogResult {
    val deviceName: String

    data class Sets(override val deviceName: String, val ids: List<Long>) : QuickLogResult
    data class Session(override val deviceName: String, val id: Long) : QuickLogResult
}

// Logs training outside of any plan (planId = null); plans never count these logs.
class QuickLogViewModel(
    deviceDao: DeviceDao,
    private val setLogDao: SetLogDao,
    private val cardioLogDao: CardioLogDao,
    userSettingsDao: UserSettingsDao,
) : ViewModel() {

    /** Null while loading, so the screen doesn't flash an empty state. */
    val devices: StateFlow<List<QuickLogDevice>?> = combine(
        deviceDao.observeActive(),
        setLogDao.observeLastSetPerDevice(),
        cardioLogDao.observeLastPerDevice(),
        userSettingsDao.observe().map { it ?: UserSettings() },
    ) { devices, lastSets, lastSessions, settings ->
        val lastSetByDevice = lastSets.associateBy { it.deviceId }
        val lastSessionByDevice = lastSessions.associateBy { it.deviceId }
        devices.map { device ->
            val lastSet = lastSetByDevice[device.id]
            QuickLogDevice(
                id = device.id,
                name = device.name,
                type = device.type,
                minWeightGrams = device.minWeightGrams,
                weightStepGrams = device.weightStepGrams,
                weightGrams = lastSet?.weightGrams ?: device.minWeightGrams,
                reps = lastSet?.reps ?: settings.defaultTargetReps,
                sets = settings.defaultTargetSets,
                durationSeconds = lastSessionByDevice[device.id]?.durationSeconds ?: settings.defaultTargetDurationSeconds,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logSets(device: QuickLogDevice, weightGrams: Int, reps: Int, sets: Int, onLogged: (QuickLogResult) -> Unit) {
        val now = System.currentTimeMillis()
        // Offset by 1 ms per set so sets logged in one tap keep a stable order.
        val logs = List(sets.coerceIn(1, MAX_SETS)) { index ->
            SetLog(
                deviceId = device.id,
                performedAt = now + index,
                weightGrams = weightGrams.coerceIn(0, MAX_WEIGHT_GRAMS),
                reps = reps.coerceIn(1, MAX_REPS),
                planId = null,
            )
        }
        viewModelScope.launch { onLogged(QuickLogResult.Sets(device.name, setLogDao.insertAll(logs))) }
    }

    fun logSession(device: QuickLogDevice, seconds: Int, onLogged: (QuickLogResult) -> Unit) {
        val session = CardioLog(
            deviceId = device.id,
            performedAt = System.currentTimeMillis(),
            durationSeconds = seconds.coerceIn(1, MAX_DURATION_SECONDS),
            planId = null,
        )
        viewModelScope.launch { onLogged(QuickLogResult.Session(device.name, cardioLogDao.insert(session))) }
    }

    fun undo(result: QuickLogResult) {
        viewModelScope.launch {
            when (result) {
                is QuickLogResult.Sets -> setLogDao.deleteByIds(result.ids)
                is QuickLogResult.Session -> cardioLogDao.deleteById(result.id)
            }
        }
    }

    companion object {
        const val MAX_SETS = 20
        private const val MAX_WEIGHT_GRAMS = 1_000_000
        private const val MAX_REPS = 999

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                QuickLogViewModel(
                    app.database.deviceDao(),
                    app.database.setLogDao(),
                    app.database.cardioLogDao(),
                    app.database.userSettingsDao(),
                )
            }
        }
    }
}
