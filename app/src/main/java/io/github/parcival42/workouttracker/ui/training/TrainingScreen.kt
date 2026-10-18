package io.github.parcival42.workouttracker.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.ui.components.NameDialog
import io.github.parcival42.workouttracker.ui.plans.PlanListScreen
import io.github.parcival42.workouttracker.ui.plans.PlanListViewModel
import io.github.parcival42.workouttracker.ui.plans.autoStartPlan
import io.github.parcival42.workouttracker.ui.workout.WorkoutScreen

// No plan -> create one; selected, default or single plan -> start it; otherwise choose from the list.
// The workout only lives here, so it always has the same top bar, however the plan was chosen.
@Composable
fun TrainingScreen(
    selectedPlanId: Long?,
    onSelectPlan: (Long) -> Unit,
    onPlanCreated: (Long) -> Unit,
    onEditPlan: (Long) -> Unit,
    onShowPlanList: () -> Unit,
    onQuickLog: () -> Unit,
    viewModel: PlanListViewModel = viewModel(factory = PlanListViewModel.Factory),
) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val loadedPlans = plans ?: return
    // A deleted selection falls back to the automatic choice.
    val activePlan = loadedPlans.firstOrNull { it.id == selectedPlanId } ?: loadedPlans.autoStartPlan()

    when {
        loadedPlans.isEmpty() -> NoPlanContent(
            onCreate = { name -> viewModel.create(name, onCreated = onPlanCreated) },
            onQuickLog = onQuickLog,
        )
        activePlan != null -> WorkoutScreen(
            planId = activePlan.id,
            onEditPlan = { onEditPlan(activePlan.id) },
            onSwitchPlan = onShowPlanList,
        )
        else -> PlanListScreen(
            onOpenPlan = onSelectPlan,
            onPlanCreated = onPlanCreated,
            onQuickLog = onQuickLog,
            viewModel = viewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoPlanContent(onCreate: (String) -> Unit, onQuickLog: () -> Unit) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.training_title)) }) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.training_no_plan),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { showDialog = true }) {
                    Text(stringResource(R.string.plan_add_title))
                }
                OutlinedButton(onClick = onQuickLog) {
                    Text(stringResource(R.string.quick_log_title))
                }
            }
        }
    }

    if (showDialog) {
        NameDialog(
            title = stringResource(R.string.plan_add_title),
            initialName = "",
            onConfirm = { name ->
                showDialog = false
                onCreate(name)
            },
            onDismiss = { showDialog = false },
        )
    }
}
