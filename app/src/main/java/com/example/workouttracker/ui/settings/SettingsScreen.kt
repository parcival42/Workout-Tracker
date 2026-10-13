package com.example.workouttracker.ui.settings

import android.app.LocaleManager
import android.net.Uri
import android.os.LocaleList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.R
import java.time.LocalDate

private enum class AppLanguage(val languageTag: String, val labelRes: Int) {
    SYSTEM("", R.string.language_system),
    GERMAN("de", R.string.language_german),
    ENGLISH("en", R.string.language_english),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LanguageSection()
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_data_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.settings_data_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
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

@Composable
private fun LanguageSection() {
    val localeManager = LocalContext.current.getSystemService(LocaleManager::class.java)
    // Read once per composition; changing the app locale recreates the activity anyway.
    val current = remember(localeManager) {
        val tag = localeManager.applicationLocales.toLanguageTags()
        AppLanguage.entries.firstOrNull { it.languageTag.isNotEmpty() && tag.startsWith(it.languageTag) }
            ?: AppLanguage.SYSTEM
    }

    Column(modifier = Modifier.selectableGroup()) {
        Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleMedium)
        AppLanguage.entries.forEach { language ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = language == current,
                        role = Role.RadioButton,
                        onClick = {
                            localeManager.applicationLocales = LocaleList.forLanguageTags(language.languageTag)
                        },
                    )
                    .padding(vertical = 4.dp),
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
}
