package io.github.parcival42.workouttracker.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.parcival42.workouttracker.WorkoutTrackerApplication
import io.github.parcival42.workouttracker.data.UserSettings
import io.github.parcival42.workouttracker.data.UserSettingsDao
import io.github.parcival42.workouttracker.data.backup.BackupRepository
import io.github.parcival42.workouttracker.data.backup.InvalidBackupException
import io.github.parcival42.workouttracker.ui.components.SECONDS_PER_MINUTE
import io.github.parcival42.workouttracker.ui.components.TARGET_MINUTES_RANGE
import io.github.parcival42.workouttracker.ui.components.TARGET_REPS_RANGE
import io.github.parcival42.workouttracker.ui.components.TARGET_SETS_RANGE
import io.github.parcival42.workouttracker.ui.components.secondsToMinutes
import io.github.parcival42.workouttracker.ui.theme.AppTheme
import io.github.parcival42.workouttracker.ui.theme.ThemeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

enum class BackupResult { EXPORTED, EXPORT_FAILED, IMPORTED, IMPORT_INVALID, IMPORT_FAILED }

class SettingsViewModel(
    private val backupRepository: BackupRepository,
    private val contentResolver: ContentResolver,
    private val userSettingsDao: UserSettingsDao,
    private val themeSettings: ThemeSettings,
) : ViewModel() {

    val theme: StateFlow<AppTheme> = themeSettings.theme

    fun setTheme(theme: AppTheme) = themeSettings.setTheme(theme)

    private val _result = MutableStateFlow<BackupResult?>(null)
    val result: StateFlow<BackupResult?> = _result.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val planDefaults: StateFlow<UserSettings> = userSettingsDao.observe()
        .map { it ?: UserSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    // Changes are deltas applied to the stored row one at a time, so fast taps are not lost
    // while the displayed value still lags behind the database.
    private val planDefaultsMutex = Mutex()

    fun changeDefaultTargetSets(delta: Int) = updatePlanDefaults {
        it.copy(defaultTargetSets = (it.defaultTargetSets + delta).coerceIn(TARGET_SETS_RANGE))
    }

    fun changeDefaultTargetReps(delta: Int) = updatePlanDefaults {
        it.copy(defaultTargetReps = (it.defaultTargetReps + delta).coerceIn(TARGET_REPS_RANGE))
    }

    fun changeDefaultTargetMinutes(delta: Int) = updatePlanDefaults {
        val minutes = (secondsToMinutes(it.defaultTargetDurationSeconds) + delta).coerceIn(TARGET_MINUTES_RANGE)
        it.copy(defaultTargetDurationSeconds = minutes * SECONDS_PER_MINUTE)
    }

    private fun updatePlanDefaults(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            planDefaultsMutex.withLock {
                userSettingsDao.upsert(transform(userSettingsDao.get() ?: UserSettings()))
            }
        }
    }

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
                SettingsViewModel(
                    BackupRepository(app.database),
                    app.contentResolver,
                    app.database.userSettingsDao(),
                    app.themeSettings,
                )
            }
        }
    }
}
