package com.example.workouttracker.ui.history

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.R
import com.example.workouttracker.data.DeviceType
import com.example.workouttracker.ui.components.EmptyHint
import com.example.workouttracker.ui.devices.labelRes
import com.example.workouttracker.ui.components.formatKg
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) },
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
                onModeSelect = viewModel::setMode,
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
    onModeSelect: (DeviceType) -> Unit,
    onDeviceToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCardio = state.mode == DeviceType.CARDIO
    val locale = LocalConfiguration.current.locales[0]
    val shortDateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale)
    }
    val longDateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    }
    val points = remember(state.dates, state.series) { buildChartPoints(state.dates, state.series) }
    val colorSlots = remember(state.series) { state.series.associate { it.deviceId to it.colorSlot } }
    var selectedPoint by remember(points) { mutableStateOf<ChartPoint?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.availableModes.size > 1) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    state.availableModes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.mode == mode,
                            onClick = { onModeSelect(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, state.availableModes.size),
                        ) {
                            Text(stringResource(mode.labelRes()))
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { onRangeSelect(range) },
                        label = { Text(stringResource(range.labelRes())) },
                    )
                }
            }
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.devices.forEach { historyDevice ->
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chartTitle = stringResource(
                        if (isCardio) R.string.history_chart_title_cardio else R.string.history_chart_title
                    )
                    Text(
                        text = chartTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HistoryChart(
                        dates = state.dates,
                        points = points,
                        scale = if (isCardio) MinutesScale else WeightScale,
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
                            val date = it.date.format(shortDateFormatter)
                            if (it.entry.reps == null) {
                                stringResource(R.string.history_point_detail_cardio, it.deviceName, date, it.entry.value)
                            } else {
                                stringResource(
                                    R.string.history_point_detail,
                                    it.deviceName,
                                    date,
                                    formatKg(it.entry.value),
                                    it.entry.reps,
                                )
                            }
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
                dateFormatter = longDateFormatter,
            )
        }
    }
}

@Composable
private fun HistoryDayEntry(
    date: LocalDate,
    series: List<HistorySeries>,
    dateFormatter: DateTimeFormatter,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        text = day.entries.joinToString("  ·  ") { entry ->
                            if (entry.reps == null) {
                                resources.getString(R.string.duration_min, entry.value)
                            } else {
                                resources.getString(R.string.workout_set_summary, formatKg(entry.value), entry.reps)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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

private fun HistoryRange.labelRes(): Int = when (this) {
    HistoryRange.ONE_MONTH -> R.string.history_range_1m
    HistoryRange.THREE_MONTHS -> R.string.history_range_3m
    HistoryRange.ONE_YEAR -> R.string.history_range_1y
    HistoryRange.ALL -> R.string.history_range_all
}
