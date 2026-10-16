package io.github.parcival42.workouttracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.github.parcival42.workouttracker.R

/** A bundled third-party library; all of them are licensed under Apache 2.0. */
private data class Library(val name: String, val author: String)

// Keep in sync with the implementation dependencies in app/build.gradle.kts.
private val Libraries = listOf(
    Library("Android Jetpack (AndroidX): Activity, Compose, Core, Lifecycle, Navigation, Room", "The Android Open Source Project"),
    Library("Kotlin standard library, kotlinx.coroutines", "JetBrains s.r.o. and Kotlin contributors"),
    Library("Reorderable", "Calvin Liang"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val resources = LocalResources.current
    val licenseText = remember(resources) {
        resources.openRawResource(R.raw.apache_license_2_0).bufferedReader().use { it.readText() }
            // The file is hard-wrapped at 80 columns; reflow paragraphs so they wrap on narrow screens.
            .split(Regex("""\n\s*\n"""))
            .map { paragraph -> paragraph.trim().replace(Regex("""\s+"""), " ") }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(Libraries) { library ->
                Column {
                    Text(library.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = library.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                HorizontalDivider()
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
