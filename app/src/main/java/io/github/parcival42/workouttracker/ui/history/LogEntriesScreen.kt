package io.github.parcival42.workouttracker.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.ui.components.EmptyHint
import io.github.parcival42.workouttracker.ui.components.formatDuration
import io.github.parcival42.workouttracker.ui.components.setSummary
import io.github.parcival42.workouttracker.ui.workout.EditSessionDialog
import io.github.parcival42.workouttracker.ui.workout.EditSetDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEntriesScreen(
    onBack: () -> Unit,
    viewModel: LogEntriesViewModel = viewModel(factory = LogEntriesViewModel.Factory),
) {
    val days by viewModel.days.collectAsStateWithLifecycle()
    var editingKey by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }

    val delete: (LogEntry) -> Unit = { entry ->
        viewModel.delete(entry)
        editingKey = null
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.entry_deleted),
                actionLabel = resources.getString(R.string.action_undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(entry)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.entries_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val loadedDays = days ?: return@Scaffold
        if (loadedDays.isEmpty()) {
            EmptyHint(
                text = stringResource(R.string.history_empty),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                loadedDays.forEach { day ->
                    item(key = "day-${day.date.toEpochDay()}") {
                        Text(
                            text = day.date.format(dateFormatter),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(day.entries, key = { it.key }) { entry ->
                        EntryRow(
                            entry = entry,
                            time = Instant.ofEpochMilli(entry.performedAt).atZone(ZoneId.systemDefault()).format(timeFormatter),
                            onClick = { editingKey = entry.key },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // Looked up from the current list, so the dialog closes if the entry disappears.
    when (val entry = days?.firstNotNullOfOrNull { day -> day.entries.firstOrNull { it.key == editingKey } }) {
        is LogEntry.Strength -> EditSetDialog(
            title = stringResource(R.string.entry_edit_set_title),
            set = entry.set,
            onConfirm = { weightGrams, reps, durationSeconds ->
                viewModel.updateSet(entry.set, weightGrams, reps, durationSeconds)
                editingKey = null
            },
            onDelete = { delete(entry) },
            onDismiss = { editingKey = null },
        )
        is LogEntry.Cardio -> EditSessionDialog(
            title = stringResource(R.string.entry_edit_session_title),
            session = entry.session,
            onConfirm = { seconds ->
                viewModel.updateSession(entry.session, seconds)
                editingKey = null
            },
            onDelete = { delete(entry) },
            onDismiss = { editingKey = null },
        )
        null -> Unit
    }
}

@Composable
private fun EntryRow(entry: LogEntry, time: String, onClick: () -> Unit) {
    val value = when (entry) {
        is LogEntry.Strength -> LocalResources.current.setSummary(entry.set)
        is LogEntry.Cardio -> stringResource(R.string.duration_exact, formatDuration(entry.session.durationSeconds))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.action_edit_entry), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.deviceName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.entry_summary, value, entry.planName ?: stringResource(R.string.entry_no_plan)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
