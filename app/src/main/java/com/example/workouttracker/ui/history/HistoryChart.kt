package com.example.workouttracker.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workouttracker.ui.components.formatKg
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

// Validated categorical palette (dark mode), assigned by fixed slot, never cycled.
val SeriesColors = listOf(
    Color(0xFF3987E5),
    Color(0xFFD95926),
    Color(0xFF199E70),
    Color(0xFFC98500),
    Color(0xFFD55181),
    Color(0xFF008300),
    Color(0xFF9085E9),
    Color(0xFFE66767),
)

private val GridColor = Color(0xFF2C2C2A)
private val AxisColor = Color(0xFF383835)
private val MutedInk = Color(0xFF898781)

/** Y axis units of a chart: the smallest visible range, allowed tick step factors and label format. */
class ValueScale(val minRange: Int, val stepFactors: List<Double>, val format: (Int) -> String)

// Grams; 2.5 kg steps are common on machines.
val WeightScale = ValueScale(minRange = 5_000, stepFactors = listOf(1.0, 2.0, 2.5, 5.0, 10.0), format = ::formatKg)

// Whole minutes; integer factors only, so ticks stay whole minutes.
val MinutesScale = ValueScale(minRange = 10, stepFactors = listOf(1.0, 2.0, 5.0, 10.0), format = Int::toString)

data class ChartPoint(
    val deviceId: Long,
    val deviceName: String,
    val date: LocalDate,
    val entry: HistoryEntry,
    /** Position on the x axis in day slots; entries of one day are spread inside their slot. */
    val x: Float,
)

fun buildChartPoints(dates: List<LocalDate>, series: List<HistorySeries>): Map<Long, List<ChartPoint>> {
    val dayIndex = dates.withIndex().associate { (index, date) -> date to index }
    return series.associate { s ->
        s.deviceId to s.days.flatMap { day ->
            val slot = dayIndex.getValue(day.date)
            day.entries.mapIndexed { i, entry ->
                ChartPoint(s.deviceId, s.deviceName, day.date, entry, slot + (i + 1f) / (day.entries.size + 1f))
            }
        }
    }
}

private data class YAxis(val min: Int, val max: Int, val ticks: List<Int>)

private fun niceYAxis(minValue: Int, maxValue: Int, scale: ValueScale): YAxis {
    val rawRange = max(maxValue - minValue, scale.minRange).toDouble()
    val roughStep = rawRange / 4
    val magnitude = 10.0.pow(floor(log10(roughStep)))
    val step = scale.stepFactors.map { it * magnitude }.first { it >= roughStep }
    val low = floor(minValue / step) * step
    val high = max(ceil(maxValue / step) * step, low + 2 * step)
    val ticks = generateSequence(low) { it + step }.takeWhile { it <= high + step / 2 }.map { it.toInt() }.toList()
    return YAxis(low.toInt(), high.toInt(), ticks)
}

@Composable
fun HistoryChart(
    dates: List<LocalDate>,
    points: Map<Long, List<ChartPoint>>,
    scale: ValueScale,
    colorSlots: Map<Long, Int>,
    selected: ChartPoint?,
    onSelect: (ChartPoint?) -> Unit,
    dateFormatter: DateTimeFormatter,
    surfaceColor: Color,
    inkColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = MutedInk, fontSize = 11.sp)
    val allPoints = remember(points) { points.values.flatten() }
    val yAxis = remember(allPoints, scale) {
        niceYAxis(allPoints.minOf { it.entry.value }, allPoints.maxOf { it.entry.value }, scale)
    }

    Canvas(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(allPoints, dates, yAxis) {
                detectTapGestures { tap ->
                    val layout = ChartLayout(size.width.toFloat(), size.height.toFloat(), this, yAxis, scale, dates.size, textMeasurer, labelStyle)
                    val hitRadius = 24.dp.toPx()
                    val nearest = allPoints.minByOrNull { (layout.position(it) - tap).getDistance() }
                    onSelect(nearest?.takeIf { (layout.position(it) - tap).getDistance() <= hitRadius })
                }
            },
    ) {
        val layout = ChartLayout(size.width, size.height, this, yAxis, scale, dates.size, textMeasurer, labelStyle)

        yAxis.ticks.forEach { tick ->
            val y = layout.y(tick)
            drawLine(GridColor, Offset(layout.left, y), Offset(layout.right, y), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure(scale.format(tick), labelStyle)
            drawText(label, topLeft = Offset(layout.left - label.size.width - 6.dp.toPx(), y - label.size.height / 2f))
        }
        drawLine(AxisColor, Offset(layout.left, layout.bottom), Offset(layout.right, layout.bottom), strokeWidth = 1.dp.toPx())

        val labelEvery = max(1, ceil(dates.size / max(1f, layout.plotWidth / 64.dp.toPx())).toInt())
        dates.forEachIndexed { index, date ->
            if (index % labelEvery != 0) return@forEachIndexed
            val label = textMeasurer.measure(date.format(dateFormatter), labelStyle)
            val centerX = layout.x(index + 0.5f)
            val x = (centerX - label.size.width / 2f).coerceIn(layout.left, size.width - label.size.width)
            drawText(label, topLeft = Offset(x, layout.bottom + 6.dp.toPx()))
        }

        points.forEach { (deviceId, seriesPoints) ->
            val color = SeriesColors[colorSlots.getValue(deviceId)]
            if (seriesPoints.size > 1) {
                val path = Path()
                seriesPoints.forEachIndexed { i, p ->
                    val o = layout.position(p)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            seriesPoints.forEach { p ->
                val o = layout.position(p)
                drawCircle(surfaceColor, radius = 6.dp.toPx(), center = o)
                drawCircle(color, radius = 4.dp.toPx(), center = o)
            }
        }

        selected?.let { p ->
            val o = layout.position(p)
            drawCircle(inkColor, radius = 8.dp.toPx(), center = o, style = Stroke(2.dp.toPx()))
        }
    }
}

private class ChartLayout(
    width: Float,
    height: Float,
    density: androidx.compose.ui.unit.Density,
    private val yAxis: YAxis,
    scale: ValueScale,
    private val daySlots: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    labelStyle: TextStyle,
) {
    val left: Float
    val right: Float = width - with(density) { 8.dp.toPx() }
    val top: Float = with(density) { 8.dp.toPx() }
    val bottom: Float = height - with(density) { 24.dp.toPx() }
    val plotWidth: Float

    init {
        val widestLabel = yAxis.ticks.maxOf { textMeasurer.measure(scale.format(it), labelStyle).size.width }
        left = widestLabel + with(density) { 12.dp.toPx() }
        plotWidth = right - left
    }

    fun x(slotPosition: Float): Float = left + plotWidth * slotPosition / max(daySlots, 1)

    fun y(value: Int): Float {
        val fraction = (value - yAxis.min).toFloat() / (yAxis.max - yAxis.min)
        return bottom - fraction * (bottom - top)
    }

    fun position(point: ChartPoint) = Offset(x(point.x), y(point.entry.value))
}
