package io.github.parcival42.workouttracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.parcival42.workouttracker.WorkoutTrackerApplication
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.CardioLogDao
import io.github.parcival42.workouttracker.data.DeviceDao
import io.github.parcival42.workouttracker.data.PlanDao
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.SetLogDao
import io.github.parcival42.workouttracker.ui.components.MAX_DURATION_SECONDS
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One logged set or cardio session; [planName] is null for logs without a plan (e.g. quick log). */
sealed interface LogEntry {
    /** Unique across both tables, whose ids overlap. */
    val key: String
    val performedAt: Long
    val deviceName: String
    val planName: String?

    data class Strength(val set: SetLog, override val deviceName: String, override val planName: String?) : LogEntry {
        override val key get() = "s-${set.id}"
        override val performedAt get() = set.performedAt
    }

    data class Cardio(val session: CardioLog, override val deviceName: String, override val planName: String?) : LogEntry {
        override val key get() = "c-${session.id}"
        override val performedAt get() = session.performedAt
    }
}

data class LogEntryDay(val date: LocalDate, val entries: List<LogEntry>)

class LogEntriesViewModel(
    deviceDao: DeviceDao,
    planDao: PlanDao,
    private val setLogDao: SetLogDao,
    private val cardioLogDao: CardioLogDao,
) : ViewModel() {

    /** Newest day first, newest entry first; null while loading. */
    val days: StateFlow<List<LogEntryDay>?> = combine(
        setLogDao.observeAll(),
        cardioLogDao.observeAll(),
        // Includes archived devices: every device with logs.
        deviceDao.observeWithHistory(),
        planDao.observePlans(),
    ) { sets, sessions, devices, plans ->
        val deviceNames = devices.associate { it.id to it.name }
        val planNames = plans.associate { it.id to it.name }
        val entries = sets.map { LogEntry.Strength(it, deviceNames[it.deviceId].orEmpty(), planNames[it.planId]) } +
            sessions.map { LogEntry.Cardio(it, deviceNames[it.deviceId].orEmpty(), planNames[it.planId]) }
        val zone = ZoneId.systemDefault()
        entries
            .sortedByDescending { it.performedAt }
            .groupBy { Instant.ofEpochMilli(it.performedAt).atZone(zone).toLocalDate() }
            .map { (date, dayEntries) -> LogEntryDay(date, dayEntries) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateSet(set: SetLog, weightGrams: Int, reps: Int?, durationSeconds: Int?) {
        val updated = set.copy(
            weightGrams = weightGrams.coerceIn(0, MAX_WEIGHT_GRAMS),
            reps = reps?.coerceIn(1, MAX_REPS),
            durationSeconds = durationSeconds?.coerceIn(1, MAX_DURATION_SECONDS),
        )
        viewModelScope.launch { setLogDao.update(updated) }
    }

    fun updateSession(session: CardioLog, seconds: Int) {
        val updated = session.copy(durationSeconds = seconds.coerceIn(1, MAX_DURATION_SECONDS))
        viewModelScope.launch { cardioLogDao.update(updated) }
    }

    fun delete(entry: LogEntry) {
        viewModelScope.launch {
            when (entry) {
                is LogEntry.Strength -> setLogDao.delete(entry.set)
                is LogEntry.Cardio -> cardioLogDao.delete(entry.session)
            }
        }
    }

    /** Undoes [delete]: re-inserts the row with its original id. */
    fun restore(entry: LogEntry) {
        viewModelScope.launch {
            when (entry) {
                is LogEntry.Strength -> setLogDao.insert(entry.set)
                is LogEntry.Cardio -> cardioLogDao.insert(entry.session)
            }
        }
    }

    companion object {
        private const val MAX_WEIGHT_GRAMS = 1_000_000
        private const val MAX_REPS = 999

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                LogEntriesViewModel(
                    app.database.deviceDao(),
                    app.database.planDao(),
                    app.database.setLogDao(),
                    app.database.cardioLogDao(),
                )
            }
        }
    }
}
