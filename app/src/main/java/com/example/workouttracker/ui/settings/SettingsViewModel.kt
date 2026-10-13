package com.example.workouttracker.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApplication
import com.example.workouttracker.data.backup.BackupRepository
import com.example.workouttracker.data.backup.InvalidBackupException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

enum class BackupResult { EXPORTED, EXPORT_FAILED, IMPORTED, IMPORT_INVALID, IMPORT_FAILED }

class SettingsViewModel(
    private val backupRepository: BackupRepository,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    private val _result = MutableStateFlow<BackupResult?>(null)
    val result: StateFlow<BackupResult?> = _result.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun export(uri: Uri) = runBusy {
        _result.value = try {
            val json = backupRepository.exportJson()
            withContext(Dispatchers.IO) {
                val stream = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
                stream.bufferedWriter().use { it.write(json) }
            }
            BackupResult.EXPORTED
        } catch (e: IOException) {
            BackupResult.EXPORT_FAILED
        }
    }

    fun import(uri: Uri) = runBusy {
        _result.value = try {
            val json = withContext(Dispatchers.IO) {
                val stream = contentResolver.openInputStream(uri) ?: throw IOException("No input stream")
                stream.bufferedReader().use { it.readText() }
            }
            backupRepository.importJson(json)
            BackupResult.IMPORTED
        } catch (e: InvalidBackupException) {
            BackupResult.IMPORT_INVALID
        } catch (e: IOException) {
            BackupResult.IMPORT_FAILED
        }
    }

    fun consumeResult() {
        _result.value = null
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                SettingsViewModel(BackupRepository(app.database), app.contentResolver)
            }
        }
    }
}
