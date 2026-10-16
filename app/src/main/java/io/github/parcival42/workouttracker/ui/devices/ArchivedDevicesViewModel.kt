package io.github.parcival42.workouttracker.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.withTransaction
import io.github.parcival42.workouttracker.WorkoutTrackerApplication
import io.github.parcival42.workouttracker.data.AppDatabase
import io.github.parcival42.workouttracker.data.ArchivedDevice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchivedDevicesViewModel(private val database: AppDatabase) : ViewModel() {

    /** Null while loading, so the screen doesn't flash an empty state. */
    val devices: StateFlow<List<ArchivedDevice>?> = database.deviceDao().observeArchivedWithLogCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun restore(deviceId: Long) {
        viewModelScope.launch { database.deviceDao().setArchived(deviceId, false) }
    }

    /** Deletes the device with its whole history and its plan entries; cannot be undone. */
    fun delete(deviceId: Long) {
        viewModelScope.launch {
            // Children first: the foreign keys to the device have no cascade.
            database.withTransaction {
                database.setLogDao().deleteForDevice(deviceId)
                database.cardioLogDao().deleteForDevice(deviceId)
                database.planDao().deleteEntriesForDevice(deviceId)
                database.deviceDao().deleteById(deviceId)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                ArchivedDevicesViewModel(app.database)
            }
        }
    }
}
