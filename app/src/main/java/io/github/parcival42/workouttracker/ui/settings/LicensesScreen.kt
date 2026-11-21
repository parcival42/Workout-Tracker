package io.github.parcival42.workouttracker.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import io.github.parcival42.workouttracker.R

/** Every bundled component with what it is used for and its license; a tap opens the license text. */
@Composable
fun LicensesScreen(onBack: () -> Unit, onShowLicense: (componentId: String) -> Unit) {
    LicenseScaffold(title = stringResource(R.string.licenses_title), onBack = onBack) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding), contentPadding = PaddingValues(vertical = 8.dp)) {
            item {
                Text(
                    text = stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(OpenSourceComponents, key = { it.id }) { component ->
                ListItem(
                    headlineContent = { Text(component.name) },
                    supportingContent = { Text(stringResource(component.usageRes)) },
                    trailingContent = {
                        Text(
                            text = component.license.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier.clickable { onShowLicense(component.id) },
                )
            }
        }
    }
}

/** The full license text of one component. */
@Composable
fun LicenseDetailScreen(componentId: String, onBack: () -> Unit) {
    val component = OpenSourceComponents.firstOrNull { it.id == componentId }
    val resources = LocalResources.current
    val licenseText = remember(resources, component) {
        component ?: return@remember ""
        resources.openRawResource(component.license.textRes).bufferedReader().use { it.readText() }
            // The files are hard-wrapped; reflow paragraphs so they wrap on narrow screens.
            .split(Regex("""\n\s*\n"""))
            .map { paragraph -> paragraph.trim().replace(Regex("""\s+"""), " ") }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }
    LicenseScaffold(title = component?.name ?: stringResource(R.string.licenses_title), onBack = onBack) { innerPadding ->
        if (component == null) return@LicenseScaffold
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Text(component.license.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = component.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            item {
                Text(
                    text = licenseText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
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
        content = content,
    )
}
