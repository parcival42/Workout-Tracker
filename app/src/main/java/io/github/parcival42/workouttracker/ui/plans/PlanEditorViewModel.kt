package io.github.parcival42.workouttracker.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.parcival42.workouttracker.WorkoutTrackerApplication
import io.github.parcival42.workouttracker.data.CircuitSettings
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceDao
import io.github.parcival42.workouttracker.data.PlanDao
import io.github.parcival42.workouttracker.data.PlanEntryItem
import io.github.parcival42.workouttracker.data.TrainingPlan
import io.github.parcival42.workouttracker.data.UserSettings
import io.github.parcival42.workouttracker.data.UserSettingsDao
import io.github.parcival42.workouttracker.data.withCircuitSettings
import io.github.parcival42.workouttracker.ui.components.SECONDS_PER_MINUTE
import io.github.parcival42.workouttracker.ui.components.TARGET_MINUTES_RANGE
import io.github.parcival42.workouttracker.ui.components.TARGET_REPS_RANGE
import io.github.parcival42.workouttracker.ui.components.TARGET_SETS_RANGE
import io.github.parcival42.workouttracker.ui.components.secondsToMinutes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val planDao: PlanDao,
    deviceDao: DeviceDao,
    private val userSettingsDao: UserSettingsDao,
) : ViewModel() {

    private val planId: Long = checkNotNull(savedStateHandle[PLAN_ID_ARG])

    val plan: StateFlow<TrainingPlan?> = planDao.observePlan(planId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val entries: StateFlow<List<PlanEntryItem>> = planDao.observeEntries(planId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableDevices: StateFlow<List<Device>> =
        combine(deviceDao.observeActive(), planDao.observeEntries(planId)) { devices, entries ->
            val usedDeviceIds = entries.mapTo(HashSet()) { it.deviceId }
            devices.filterNot { it.id in usedDeviceIds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rename(name: String) {
        val current = plan.value ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { planDao.updatePlan(current.copy(name = trimmed)) }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = plan.value ?: return
        viewModelScope.launch {
            planDao.deletePlan(current)
            onDeleted()
        }
    }

    fun addDevice(deviceId: Long) {
        viewModelScope.launch {
            // The personal defaults only prefill the new entry; it is edited independently afterwards.
            val defaults = userSettingsDao.get() ?: UserSettings()
            planDao.addEntry(
                planId = planId,
                deviceId = deviceId,
                targetSets = defaults.defaultTargetSets.coerceIn(TARGET_SETS_RANGE),
                targetReps = defaults.defaultTargetReps.coerceIn(TARGET_REPS_RANGE),
                targetDurationSeconds = secondsToMinutes(defaults.defaultTargetDurationSeconds)
                    .coerceIn(TARGET_MINUTES_RANGE) * SECONDS_PER_MINUTE,
            )
        }
    }

    fun updateTargets(entryId: Long, targetSets: Int, targetReps: Int) {
        viewModelScope.launch { planDao.updateTargets(entryId, targetSets, targetReps) }
    }

    fun updateTargetDuration(entryId: Long, targetMinutes: Int) {
        viewModelScope.launch { planDao.updateTargetDuration(entryId, targetMinutes * SECONDS_PER_MINUTE) }
    }

    fun updateCircuitSettings(settings: CircuitSettings) {
        val current = plan.value ?: return
        viewModelScope.launch { planDao.updatePlan(current.withCircuitSettings(settings)) }
    }

    /** Null uses the plan's station time again. */
    fun updateStationSeconds(entryId: Long, stationSeconds: Int?) {
        viewModelScope.launch { planDao.updateStationSeconds(entryId, stationSeconds) }
    }

    fun removeEntry(entryId: Long) {
        viewModelScope.launch { planDao.deleteEntry(entryId) }
    }

    fun saveOrder(orderedEntryIds: List<Long>) {
        viewModelScope.launch { planDao.reorderEntries(orderedEntryIds) }
    }

    companion object {
        const val PLAN_ID_ARG = "planId"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                PlanEditorViewModel(
                    createSavedStateHandle(),
                    app.database.planDao(),
                    app.database.deviceDao(),
                    app.database.userSettingsDao(),
                )
            }
        }
    }
}
