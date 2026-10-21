package io.github.parcival42.workouttracker.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.parcival42.workouttracker.WorkoutTrackerApplication
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.CardioLogDao
import io.github.parcival42.workouttracker.data.DeviceDao
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.SetLogDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The latest logged set or session of a device, shown in the device list. */
data class LastLog(val set: SetLog?, val session: CardioLog?) {
    val performedAt: Long get() = maxOf(set?.performedAt ?: 0L, session?.performedAt ?: 0L)
}

class DeviceListViewModel(
    private val deviceDao: DeviceDao,
    setLogDao: SetLogDao,
    cardioLogDao: CardioLogDao,
) : ViewModel() {

    val devices: StateFlow<List<Device>> = deviceDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedDevices: StateFlow<List<Device>> = deviceDao.observeArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // A device has either sets or sessions (by type), so the two never collide.
    val lastLogs: StateFlow<Map<Long, LastLog>> = combine(
        setLogDao.observeLastSetPerDevice(),
        cardioLogDao.observeLastPerDevice(),
    ) { sets, sessions ->
        sets.associate { it.deviceId to LastLog(set = it, session = null) } +
            sessions.associate { it.deviceId to LastLog(set = null, session = it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val deviceIdsWithLogs: StateFlow<Set<Long>> = deviceDao.observeIdsWithLogs()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun save(device: Device) {
        if (device.name.isBlank()) return
        viewModelScope.launch {
            if (device.id == 0L) deviceDao.insert(device) else deviceDao.update(device)
        }
    }

    fun addAll(devices: List<Device>) {
        if (devices.isEmpty()) return
        viewModelScope.launch { deviceDao.insertAll(devices) }
    }

    fun archive(device: Device) {
        viewModelScope.launch { deviceDao.setArchived(device.id, true) }
    }

    fun restore(device: Device) {
        viewModelScope.launch { deviceDao.setArchived(device.id, false) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                DeviceListViewModel(app.database.deviceDao(), app.database.setLogDao(), app.database.cardioLogDao())
            }
        }
    }
}
