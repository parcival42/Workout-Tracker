package com.example.workouttracker.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApplication
import com.example.workouttracker.data.Device
import com.example.workouttracker.data.DeviceDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeviceListViewModel(private val deviceDao: DeviceDao) : ViewModel() {

    val devices: StateFlow<List<Device>> = deviceDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(device: Device) {
        if (device.name.isBlank()) return
        viewModelScope.launch {
            if (device.id == 0L) deviceDao.insert(device) else deviceDao.update(device)
        }
    }

    fun archive(device: Device) {
        viewModelScope.launch { deviceDao.update(device.copy(archived = true)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                DeviceListViewModel(app.database.deviceDao())
            }
        }
    }
}
