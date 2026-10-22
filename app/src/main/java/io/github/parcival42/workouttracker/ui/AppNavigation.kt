package io.github.parcival42.workouttracker.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.ui.devices.ArchivedDevicesScreen
import io.github.parcival42.workouttracker.ui.devices.DeviceListScreen
import io.github.parcival42.workouttracker.ui.history.HistoryScreen
import io.github.parcival42.workouttracker.ui.history.LogEntriesScreen
import io.github.parcival42.workouttracker.ui.quicklog.QuickLogScreen
import io.github.parcival42.workouttracker.ui.settings.LicensesScreen
import io.github.parcival42.workouttracker.ui.settings.SettingsScreen
import io.github.parcival42.workouttracker.ui.training.TrainingScreen
import io.github.parcival42.workouttracker.ui.plans.PlanListScreen

private object Routes {
    const val TRAINING = "training"
    const val PLANS = "plans"
    const val DEVICES = "devices"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val QUICK_LOG = "quicklog"
    const val ARCHIVED_DEVICES = "devices/archived"
    const val LOG_ENTRIES = "history/entries"
    const val LICENSES = "settings/licenses"
}

// Plan picked from a plan list, kept in the Training entry's saved state while the app runs.
private const val SELECTED_PLAN_KEY = "selectedPlanId"

private enum class TopLevelDestination(
    val route: String,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    TRAINING(Routes.TRAINING, R.drawable.ic_fitness, R.string.training_title),
    HISTORY(Routes.HISTORY, R.drawable.ic_chart, R.string.history_title),
    DEVICES(Routes.DEVICES, R.drawable.ic_category, R.string.devices_title),
    SETTINGS(Routes.SETTINGS, R.drawable.ic_settings, R.string.settings_title),
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                            label = { Text(stringResource(destination.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // Only the bottom bar's space is applied here; each screen handles its own top bar insets.
        val bottomPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = Routes.TRAINING,
            modifier = Modifier
                .padding(bottomPadding)
                .consumeWindowInsets(bottomPadding),
        ) {
            composable(Routes.TRAINING) { entry ->
                val selectedPlanId by entry.savedStateHandle
                    .getStateFlow<Long?>(SELECTED_PLAN_KEY, null)
                    .collectAsStateWithLifecycle()
                TrainingScreen(
                    selectedPlanId = selectedPlanId,
                    onSelectPlan = { entry.savedStateHandle[SELECTED_PLAN_KEY] = it },
                    onShowPlanList = { navController.navigate(Routes.PLANS) },
                    onQuickLog = { navController.navigate(Routes.QUICK_LOG) },
                )
            }
            composable(Routes.PLANS) {
                // A new plan opens like a picked one: in the Training tab's workout.
                val openPlan: (Long) -> Unit = { planId ->
                    navController.getBackStackEntry(Routes.TRAINING).savedStateHandle[SELECTED_PLAN_KEY] = planId
                    navController.popBackStack(Routes.TRAINING, inclusive = false)
                }
                PlanListScreen(
                    onOpenPlan = openPlan,
                    onPlanCreated = openPlan,
                    onQuickLog = { navController.navigate(Routes.QUICK_LOG) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.QUICK_LOG) {
                QuickLogScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onShowEntries = { navController.navigate(Routes.LOG_ENTRIES) })
            }
            composable(Routes.LOG_ENTRIES) {
                LogEntriesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DEVICES) {
                DeviceListScreen(onShowArchived = { navController.navigate(Routes.ARCHIVED_DEVICES) })
            }
            composable(Routes.ARCHIVED_DEVICES) {
                ArchivedDevicesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onShowLicenses = { navController.navigate(Routes.LICENSES) })
            }
            composable(Routes.LICENSES) {
                LicensesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
