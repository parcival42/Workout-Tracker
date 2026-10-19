package io.github.parcival42.workouttracker.ui.history

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.ui.components.EmptyHint
import io.github.parcival42.workouttracker.ui.devices.labelRes
import io.github.parcival42.workouttracker.ui.components.bodyweightSetSummary
import io.github.parcival42.workouttracker.ui.components.cardioSummary
import io.github.parcival42.workouttracker.ui.components.formatDuration
import io.github.parcival42.workouttracker.ui.components.formatKg
import io.github.parcival42.workouttracker.ui.components.formatKm
import io.github.parcival42.workouttracker.ui.components.formatKmh
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onShowEntries: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    if (state.devices.isNotEmpty()) {
                        IconButton(onClick = onShowEntries) {
                            Icon(
                                painter = painterResource(R.drawable.ic_list),
                                contentDescription = stringResource(R.string.entries_title),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.devices.isEmpty()) {
            EmptyHint(
                text = stringResource(R.string.history_empty),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            HistoryContent(
                state = state,
                onRangeSelect = viewModel::setRange,
                onPlanSelect = viewModel::setPlanFilter,
                onModeSelect = viewModel::setMode,
                onCardioMetricSelect = viewModel::setCardioMetric,
                onDeviceToggle = viewModel::toggleDevice,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    onRangeSelect: (HistoryRange) -> Unit,
    onPlanSelect: (Long?) -> Unit,
    onModeSelect: (DeviceType) -> Unit,
    onCardioMetricSelect: (CardioMetric) -> Unit,
    onDeviceToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCardio = state.mode == DeviceType.CARDIO
    val bodyweightMeasure = state.measure.takeIf { state.mode == DeviceType.BODYWEIGHT }
    val locale = LocalConfiguration.current.locales[0]
    val shortDateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale)
    }
    val longDateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    }
    val resources = LocalResources.current
    val cardioMetric = state.cardioMetric.takeIf { isCardio }
    val points = remember(state.dates, state.series, cardioMetric) { buildChartPoints(state.dates, state.series, cardioMetric) }
    val colorSlots = remember(state.series) { state.series.associate { it.deviceId to it.colorSlot } }
    var selectedPoint by remember(points) { mutableStateOf<ChartPoint?>(null) }

    // The kind of history is the main switch: tabs. Plan, range and metric change rarely, so they
    // share one row of dropdown chips that still show the current value.
    Column(modifier = modifier) {
        if (state.availableModes.size > 1) {
            PrimaryTabRow(selectedTabIndex = state.availableModes.indexOf(state.mode).coerceAtLeast(0)) {
                state.availableModes.forEach { mode ->
                    Tab(
                        selected = state.mode == mode,
                        onClick = { onModeSelect(mode) },
                        // Tab colors both states alike by default; M3 dims the unselected ones.
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text(stringResource(mode.labelRes()), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.plans.isNotEmpty()) {
                        val selectedPlan = state.plans.firstOrNull { it.id == state.planId }
                        DropdownChip(
                            label = selectedPlan?.name ?: stringResource(R.string.history_plan_all),
                            highlighted = selectedPlan != null,
                            options = listOf<Long?>(null) + state.plans.map { it.id },
                            selected = state.planId,
                            optionLabel = { id ->
                                state.plans.firstOrNull { it.id == id }?.name ?: stringResource(R.string.history_plan_all)
                            },
                            onSelect = onPlanSelect,
                        )
                    }
                    DropdownChip(
                        label = stringResource(state.range.labelRes()),
                        highlighted = state.range != HistoryRange.THREE_MONTHS,
                        options = HistoryRange.entries,
                        selected = state.range,
                        optionLabel = { stringResource(it.labelRes()) },
                        onSelect = onRangeSelect,
                    )
                    if (isCardio && state.hasCardioMetrics) {
                        DropdownChip(
                            label = stringResource(state.cardioMetric.labelRes()),
                            highlighted = state.cardioMetric != CardioMetric.DURATION,
                            options = CardioMetric.entries,
                            selected = state.cardioMetric,
                            optionLabel = { stringResource(it.labelRes()) },
                            onSelect = onCardioMetricSelect,
                        )
                    }
                }
            }

            // One scrolling row, so many devices don't push the chart down.
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.devices, key = { it.device.id }) { historyDevice ->
                        val slot = historyDevice.colorSlot
                        FilterChip(
                            selected = slot != null,
                            enabled = slot != null || state.canSelectMore,
                            onClick = { onDeviceToggle(historyDevice.device.id) },
                            label = { Text(historyDevice.device.name) },
                            leadingIcon = slot?.let { { SeriesDot(SeriesColors[it]) } },
                        )
                    }
                }
            }

            item {
                if (state.dates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(
                                if (state.series.isEmpty()) R.string.history_select_device else R.string.history_no_data_in_range
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val chartTitle = stringResource(
                            when {
                                cardioMetric == CardioMetric.DISTANCE -> R.string.history_chart_title_distance
                                cardioMetric == CardioMetric.SPEED -> R.string.history_chart_title_speed
                                isCardio -> R.string.history_chart_title_cardio
                                bodyweightMeasure == BodyweightMeasure.REPS -> R.string.history_chart_title_reps
                                bodyweightMeasure == BodyweightMeasure.TIME -> R.string.history_chart_title_time
                                else -> R.string.history_chart_title
                            }
                        )
                        Text(
                            text = chartTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HistoryChart(
                            dates = state.dates,
                            points = points,
                            scale = when {
                                cardioMetric == CardioMetric.DISTANCE -> DistanceScale
                                cardioMetric == CardioMetric.SPEED -> SpeedScale
                                isCardio -> MinutesScale
                                bodyweightMeasure == BodyweightMeasure.REPS -> RepsScale
                                bodyweightMeasure == BodyweightMeasure.TIME -> SecondsScale
                                else -> WeightScale
                            },
                            colorSlots = colorSlots,
                            selected = selectedPoint,
                            onSelect = { selectedPoint = it },
                            dateFormatter = shortDateFormatter,
                            surfaceColor = MaterialTheme.colorScheme.background,
                            inkColor = MaterialTheme.colorScheme.onBackground,
                            contentDescription = chartTitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                        )
                        Text(
                            text = selectedPoint?.let {
                                val groups = formatGroups(it.day.groups, resources, bodyweightMeasure)
                                val several = it.day.entries.size > 1
                                val summary = when {
                                    cardioMetric == CardioMetric.SPEED ->
                                        stringResource(R.string.history_speed_detail, formatKmh(it.value), groups)
                                    cardioMetric == CardioMetric.DISTANCE && several ->
                                        stringResource(R.string.history_distance_total, formatKm(it.value), groups)
                                    isCardio && several ->
                                        stringResource(R.string.history_cardio_total, formatDuration(it.value), groups)
                                    else -> groups
                                }
                                stringResource(R.string.history_point_detail, it.deviceName, it.day.date.format(shortDateFormatter), summary)
                            } ?: stringResource(R.string.history_tap_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedPoint != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            val datesDescending = state.dates.asReversed()
            items(datesDescending, key = { it.toEpochDay() }) { date ->
                HistoryDayEntry(
                    date = date,
                    series = state.series,
                    bodyweightMeasure = bodyweightMeasure,
                    dateFormatter = longDateFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

/** Filter chip that shows the current value and opens a menu; [highlighted] marks a non-default value. */
@Composable
private fun <T> DropdownChip(
    label: String,
    highlighted: Boolean,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = highlighted,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    trailingIcon = if (option == selected) {
                        { Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryDayEntry(
    date: LocalDate,
    series: List<HistorySeries>,
    dateFormatter: DateTimeFormatter,
    bodyweightMeasure: BodyweightMeasure?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        Text(
            text = date.format(dateFormatter),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        val resources = LocalResources.current
        series.forEach { s ->
            val day = s.days.firstOrNull { it.date == date } ?: return@forEach
            Row(verticalAlignment = Alignment.Top) {
                SeriesDot(SeriesColors[s.colorSlot], modifier = Modifier.padding(top = 6.dp))
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = s.deviceName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = formatGroups(day.groups, resources, bodyweightMeasure),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** [bodyweightMeasure] is set in bodyweight mode, where the group value is reps or seconds. */
private fun formatGroups(groups: List<EntryGroup>, resources: Resources, bodyweightMeasure: BodyweightMeasure?): String =
    groups.joinToString(", ") { group ->
        when {
            bodyweightMeasure != null -> {
                val value = if (bodyweightMeasure == BodyweightMeasure.TIME) {
                    resources.bodyweightSetSummary(reps = null, durationSeconds = group.value)
                } else {
                    resources.bodyweightSetSummary(reps = group.value, durationSeconds = null)
                }
                if (group.count == 1) value else resources.getString(R.string.history_group_bodyweight, group.count, value)
            }
            group.durationSeconds != null && group.reps != null -> resources.getString(
                R.string.history_group_strength_timed_reps,
                group.count,
                group.reps,
                formatDuration(group.durationSeconds),
                formatKg(group.value),
            )
            group.durationSeconds != null -> resources.getString(
                R.string.history_group_strength_timed,
                group.count,
                formatDuration(group.durationSeconds),
                formatKg(group.value),
            )
            group.reps != null -> resources.getString(R.string.history_group_strength, group.count, group.reps, formatKg(group.value))
            group.count == 1 -> resources.cardioSummary(group.value, group.distanceMeters)
            group.distanceMeters != null -> resources.getString(
                R.string.history_group_cardio_distance,
                group.count,
                formatDuration(group.value),
                formatKm(group.distanceMeters),
            )
            else -> resources.getString(R.string.history_group_cardio, group.count, formatDuration(group.value))
        }
    }

@Composable
private fun SeriesDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

private fun CardioMetric.labelRes(): Int = when (this) {
    CardioMetric.DURATION -> R.string.history_metric_duration
    CardioMetric.DISTANCE -> R.string.history_metric_distance
    CardioMetric.SPEED -> R.string.history_metric_speed
}

private fun HistoryRange.labelRes(): Int = when (this) {
    HistoryRange.ONE_MONTH -> R.string.history_range_1m
    HistoryRange.THREE_MONTHS -> R.string.history_range_3m
    HistoryRange.ONE_YEAR -> R.string.history_range_1y
    HistoryRange.ALL -> R.string.history_range_all
}
