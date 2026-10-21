package io.github.parcival42.workouttracker.ui.settings

import android.app.LocaleManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.BuildConfig
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.UserSettings
import io.github.parcival42.workouttracker.ui.components.Stepper
import io.github.parcival42.workouttracker.ui.components.TARGET_MINUTES_RANGE
import io.github.parcival42.workouttracker.ui.components.TARGET_REPS_RANGE
import io.github.parcival42.workouttracker.ui.components.TARGET_SETS_RANGE
import io.github.parcival42.workouttracker.ui.components.secondsToMinutes
import io.github.parcival42.workouttracker.ui.theme.AppTheme
import io.github.parcival42.workouttracker.ui.theme.colorScheme
import java.time.LocalDate

private enum class AppLanguage(val languageTag: String, val labelRes: Int) {
    SYSTEM("", R.string.language_system),
    GERMAN("de", R.string.language_german),
    ENGLISH("en", R.string.language_english),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onShowLicenses: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val planDefaults by viewModel.planDefaults.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    var pendingImportUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::export) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingImportUri = uri }

    LaunchedEffect(result) {
        val message = when (result) {
            BackupResult.EXPORTED -> R.string.backup_exported
            BackupResult.EXPORT_FAILED -> R.string.backup_export_failed
            BackupResult.IMPORTED -> R.string.backup_imported
            BackupResult.IMPORT_INVALID -> R.string.backup_import_invalid
            BackupResult.IMPORT_FAILED -> R.string.backup_import_failed
            null -> return@LaunchedEffect
        }
        // Consume only after showing: changing the key earlier would cancel the snackbar.
        snackbarHostState.showSnackbar(resources.getString(message))
        viewModel.consumeResult()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection(title = stringResource(R.string.settings_appearance_title)) {
                ThemePicker(selected = theme, onSelect = viewModel::setTheme)
                // LocaleManager needs Android 13 (API 33); older versions follow the system language.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    HorizontalDivider()
                    LanguageRow()
                }
            }
            SettingsSection(title = stringResource(R.string.settings_plan_defaults_title)) {
                PlanDefaultsSection(
                    defaults = planDefaults,
                    onSetsChange = viewModel::changeDefaultTargetSets,
                    onRepsChange = viewModel::changeDefaultTargetReps,
                    onMinutesChange = viewModel::changeDefaultTargetMinutes,
                )
            }
            SettingsSection(title = stringResource(R.string.settings_data_title)) {
                Text(
                    text = stringResource(R.string.settings_data_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Important, but not the main thing on this screen: tonal instead of filled.
                FilledTonalButton(
                    onClick = { exportLauncher.launch("workout-tracker-${LocalDate.now()}.json") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.backup_export_action))
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.backup_import_action))
                }
            }
            SettingsSection(title = stringResource(R.string.settings_about_title)) {
                AboutSection(onShowLicenses = onShowLicenses)
            }
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    viewModel.import(uri)
                }) {
                    Text(stringResource(R.string.backup_import_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Section title above a grouped card. */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .semantics { heading() },
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ThemePicker(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTheme.entries.forEach { theme ->
                ThemeOption(
                    theme = theme,
                    selected = theme == selected,
                    onClick = { onSelect(theme) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A small preview in the theme's own colors, so the choice is visible before picking it. */
@Composable
private fun ThemeOption(theme: AppTheme, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheme = remember(theme, context) { theme.colorScheme(context) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerHigh,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Three options share one row, so the swatches stay small; the border marks the selection.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(scheme.primary, scheme.tertiary, scheme.secondaryContainer).forEach { color ->
                    Box(modifier = Modifier.size(16.dp).background(color, CircleShape))
                }
            }
            Text(
                text = stringResource(theme.labelRes),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AboutSection(onShowLicenses: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val repositoryUrl = stringResource(R.string.settings_repository_url)
    Text(
        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(
        onClick = { uriHandler.openUri(repositoryUrl) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.settings_repository_action))
    }
    OutlinedButton(onClick = onShowLicenses, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.licenses_title))
    }
}

/** One row with the current language; the choice opens in a dialog. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LanguageRow() {
    val localeManager = LocalContext.current.getSystemService(LocaleManager::class.java)
    // Read once per composition; changing the app locale recreates the activity anyway.
    val current = remember(localeManager) {
        val tag = localeManager.applicationLocales.toLanguageTags()
        AppLanguage.entries.firstOrNull { it.languageTag.isNotEmpty() && tag.startsWith(it.languageTag) }
            ?: AppLanguage.SYSTEM
    }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { showDialog = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_language_title),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(current.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.settings_language_title)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = language == current,
                                    role = Role.RadioButton,
                                    onClick = {
                                        showDialog = false
                                        localeManager.applicationLocales = LocaleList.forLanguageTags(language.languageTag)
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = language == current, onClick = null)
                            Text(
                                text = stringResource(language.labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun PlanDefaultsSection(
    defaults: UserSettings,
    onSetsChange: (delta: Int) -> Unit,
    onRepsChange: (delta: Int) -> Unit,
    onMinutesChange: (delta: Int) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_plan_defaults_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Stepper(
        label = stringResource(R.string.target_sets),
        value = defaults.defaultTargetSets,
        onValueChange = { onSetsChange(it - defaults.defaultTargetSets) },
        range = TARGET_SETS_RANGE,
    )
    Stepper(
        label = stringResource(R.string.target_reps),
        value = defaults.defaultTargetReps,
        onValueChange = { onRepsChange(it - defaults.defaultTargetReps) },
        range = TARGET_REPS_RANGE,
    )
    Stepper(
        label = stringResource(R.string.settings_default_duration),
        value = secondsToMinutes(defaults.defaultTargetDurationSeconds),
        onValueChange = { onMinutesChange(it - secondsToMinutes(defaults.defaultTargetDurationSeconds)) },
        range = TARGET_MINUTES_RANGE,
    )
}
