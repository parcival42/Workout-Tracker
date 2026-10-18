package io.github.parcival42.workouttracker.ui.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.PlanType
import io.github.parcival42.workouttracker.data.TrainingPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanListScreen(
    onOpenPlan: (Long) -> Unit,
    onPlanCreated: (Long) -> Unit,
    onQuickLog: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: PlanListViewModel = viewModel(factory = PlanListViewModel.Factory),
) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plans_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.plan_add_title),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                Text(
                    text = stringResource(R.string.plans_default_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item(key = "quick-log") {
                QuickLogRow(onClick = onQuickLog)
                HorizontalDivider()
            }
            items(plans.orEmpty(), key = { it.id }) { plan ->
                PlanRow(
                    plan = plan,
                    onClick = { onOpenPlan(plan.id) },
                    onToggleDefault = { viewModel.toggleDefault(plan) },
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddDialog) {
        NewPlanDialog(
            onConfirm = { name, type ->
                showAddDialog = false
                viewModel.create(name, type, onCreated = onPlanCreated)
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun QuickLogRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_fitness),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(stringResource(R.string.quick_log_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.quick_log_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanRow(plan: TrainingPlan, onClick: () -> Unit, onToggleDefault: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = plan.name, style = MaterialTheme.typography.titleMedium)
            if (plan.type == PlanType.CIRCUIT) {
                Text(
                    text = stringResource(R.string.plan_circuit_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onToggleDefault) {
            Icon(
                painter = painterResource(if (plan.isDefault) R.drawable.ic_star else R.drawable.ic_star_border),
                contentDescription = stringResource(
                    if (plan.isDefault) R.string.action_unset_default_plan else R.string.action_set_default_plan
                ),
                tint = if (plan.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
