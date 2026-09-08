package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cloolalang.notspotdetector.MonitorState
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.RttSample
import kotlinx.coroutines.delay

private const val CHART_MIN_RTT_MS = 20f
private const val CHART_MAX_RTT_MS = 500f

@Composable
fun RttGraphCard(
    samples: List<RttSample>,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isActive) {
        while (isActive) {
            nowMs = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val windowStartMs = nowMs - MonitorState.RTT_HISTORY_WINDOW_MS
    val visibleSamples = samples.filter { it.timestampMs >= windowStartMs }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.tertiary

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.rtt_graph_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.rtt_graph_window),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.rtt_graph_axis_hint,
                        CHART_MIN_RTT_MS.toInt(),
                        CHART_MAX_RTT_MS.toInt()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (visibleSamples.isEmpty()) {
                Text(
                    text = stringResource(R.string.rtt_graph_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = stringResource(R.string.rtt_graph_label_ms, CHART_MIN_RTT_MS.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.rtt_graph_label_ms, CHART_MAX_RTT_MS.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val chartLeft = 0f
                        val chartRight = size.width
                        val chartTop = 0f
                        val chartBottom = size.height
                        val chartWidth = chartRight - chartLeft
                        val chartHeight = chartBottom - chartTop

                        fun rttToY(rttMs: Float): Float {
                            val clamped = rttMs.coerceIn(CHART_MIN_RTT_MS, CHART_MAX_RTT_MS)
                            val normalized = (clamped - CHART_MIN_RTT_MS) / (CHART_MAX_RTT_MS - CHART_MIN_RTT_MS)
                            return chartTop + normalized * chartHeight
                        }

                        fun timeToX(timestampMs: Long): Float {
                            val progress = (timestampMs - windowStartMs).toFloat() / MonitorState.RTT_HISTORY_WINDOW_MS
                            return chartLeft + progress.coerceIn(0f, 1f) * chartWidth
                        }

                        listOf(CHART_MIN_RTT_MS, 100f, 250f, CHART_MAX_RTT_MS).forEach { rtt ->
                            val y = rttToY(rtt)
                            drawLine(
                                color = gridColor,
                                start = Offset(chartLeft, y),
                                end = Offset(chartRight, y),
                                strokeWidth = 1f
                            )
                        }

                        if (visibleSamples.size == 1) {
                            val sample = visibleSamples.first()
                            drawCircle(
                                color = pointColor,
                                radius = 5.dp.toPx(),
                                center = Offset(timeToX(sample.timestampMs), rttToY(sample.rttMs.toFloat()))
                            )
                        } else {
                            val path = Path()
                            visibleSamples.forEachIndexed { index, sample ->
                                val point = Offset(
                                    timeToX(sample.timestampMs),
                                    rttToY(sample.rttMs.toFloat())
                                )
                                if (index == 0) {
                                    path.moveTo(point.x, point.y)
                                } else {
                                    path.lineTo(point.x, point.y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                            visibleSamples.forEach { sample ->
                                drawCircle(
                                    color = pointColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(
                                        timeToX(sample.timestampMs),
                                        rttToY(sample.rttMs.toFloat())
                                    )
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.rtt_graph_time_start),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 44.dp)
                    )
                    Text(
                        text = stringResource(R.string.rtt_graph_time_now),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
