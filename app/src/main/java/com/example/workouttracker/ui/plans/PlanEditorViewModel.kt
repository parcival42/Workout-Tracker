package com.example.workouttracker.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApplication
import com.example.workouttracker.data.Device
import com.example.workouttracker.data.DeviceDao
import com.example.workouttracker.data.PlanDao
import com.example.workouttracker.data.PlanEntryItem
import com.example.workouttracker.data.TrainingPlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val planDao: PlanDao,
    deviceDao: DeviceDao,
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
            planDao.addEntry(planId, deviceId, DEFAULT_TARGET_SETS, DEFAULT_TARGET_REPS)
        }
    }

    fun updateTargets(entryId: Long, targetSets: Int, targetReps: Int) {
        viewModelScope.launch { planDao.updateTargets(entryId, targetSets, targetReps) }
    }

    fun removeEntry(entryId: Long) {
        viewModelScope.launch { planDao.deleteEntry(entryId) }
    }

    fun saveOrder(orderedEntryIds: List<Long>) {
        viewModelScope.launch { planDao.reorderEntries(orderedEntryIds) }
    }

    companion object {
        const val PLAN_ID_ARG = "planId"
        const val DEFAULT_TARGET_SETS = 3
        const val DEFAULT_TARGET_REPS = 10

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                PlanEditorViewModel(
                    createSavedStateHandle(),
                    app.database.planDao(),
                    app.database.deviceDao(),
                )
            }
        }
    }
}
