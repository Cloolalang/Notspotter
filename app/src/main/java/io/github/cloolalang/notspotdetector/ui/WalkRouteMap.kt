package io.github.cloolalang.notspotdetector.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.ImagePoint
import io.github.cloolalang.notspotdetector.model.PlanCamera
import io.github.cloolalang.notspotdetector.model.RsrpBin
import io.github.cloolalang.notspotdetector.model.RsrpBinSegment
import io.github.cloolalang.notspotdetector.model.WalkPin
import io.github.cloolalang.notspotdetector.model.WalkRoute
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Floor plan for a zone test. Taps drop a mark at the zone clock. The line between
 * marks is coloured with the RSRP tape, and a dot every 30 seconds shares that clock.
 */
@Composable
fun WalkRouteMap(
    floorPlan: Bitmap?,
    pins: List<WalkPin>,
    rsrpSegments: List<RsrpBinSegment>,
    zoneStartMs: Long?,
    markTimeMs: Long?,
    liveBin: RsrpBin?,
    onChooseFloorPlan: () -> Unit,
    onMark: (WalkPin) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onChooseFloorPlan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.zone_test_floor_plan))
        }
        if (floorPlan == null) {
            Text(
                text = stringResource(R.string.zone_test_floor_plan_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        val bitmap = floorPlan
        val planDescription = stringResource(R.string.zone_test_floor_plan_image)
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        var camera by remember(bitmap) { mutableStateOf(PlanCamera()) }
        val markTime = rememberUpdatedState(markTimeMs)
        val canMark = rememberUpdatedState(zoneStartMs != null && markTimeMs != null)
        val mark = rememberUpdatedState(onMark)
        val pinsNow = rememberUpdatedState(pins)
        val textMeasurer = rememberTextMeasurer()
        val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.Black)
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight.coerceAtLeast(160.dp))
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = planDescription }
                    .pointerInput(bitmap.width, bitmap.height) {
                        val slop = viewConfiguration.touchSlop
                        val viewW = { size.width.toFloat() }
                        val viewH = { size.height.toFloat() }
                        val imageW = bitmap.width.toFloat()
                        val imageH = bitmap.height.toFloat()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val start = down.position
                            var twoFinger = false
                            var dragged = false
                            var fingersDown = true
                            while (fingersDown) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressed = event.changes.filter { it.pressed }
                                val zoomed = camera.zoom > WalkRoute.MIN_ZOOM + 0.01f
                                if (pressed.size >= 2 || (pressed.size == 1 && zoomed && !twoFinger)) {
                                    event.changes.forEach { change ->
                                        if (change.pressed) change.consume()
                                    }
                                }
                                if (pressed.size >= 2) {
                                    twoFinger = true
                                    val pan = event.calculatePan()
                                    if (pan != Offset.Zero) {
                                        camera = WalkRoute.moveCentre(
                                            viewW(), viewH(), imageW, imageH, camera, pan.x, pan.y
                                        )
                                    }
                                    val zoom = event.calculateZoom()
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    if (
                                        zoom.isFinite() &&
                                        abs(zoom - 1f) > 0.001f &&
                                        centroid != Offset.Unspecified
                                    ) {
                                        camera = WalkRoute.zoomAbout(
                                            viewW(), viewH(), imageW, imageH,
                                            camera, centroid.x, centroid.y, zoom
                                        )
                                    }
                                } else if (pressed.size == 1 && !twoFinger) {
                                    val change = pressed.first()
                                    val dx = change.position.x - start.x
                                    val dy = change.position.y - start.y
                                    val moved = dx * dx + dy * dy > slop * slop
                                    if (moved) dragged = true
                                    if (moved && zoomed) {
                                        val delta = change.position - change.previousPosition
                                        camera = WalkRoute.moveCentre(
                                            viewW(), viewH(), imageW, imageH, camera, delta.x, delta.y
                                        )
                                    }
                                }
                                fingersDown = event.changes.any { it.pressed }
                            }
                            if (!twoFinger && !dragged) {
                                val atMs = markTime.value
                                if (canMark.value && atMs != null) {
                                    val point = WalkRoute.fractionInView(
                                        tapX = start.x,
                                        tapY = start.y,
                                        viewWidth = viewW(),
                                        viewHeight = viewH(),
                                        imageWidth = imageW,
                                        imageHeight = imageH,
                                        zoom = camera.zoom,
                                        panX = camera.panX,
                                        panY = camera.panY
                                    )
                                    if (point != null) {
                                        val previous = pinsNow.value.lastOrNull()
                                        val snapped = if (previous == null) {
                                            point
                                        } else {
                                            WalkRoute.snapLeg(previous, point, imageW, imageH)
                                        }
                                        mark.value(WalkPin(atMs, snapped.x, snapped.y))
                                    }
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rect = WalkRoute.visibleRect(
                        viewWidth = size.width,
                        viewHeight = size.height,
                        imageWidth = bitmap.width.toFloat(),
                        imageHeight = bitmap.height.toFloat(),
                        zoom = camera.zoom,
                        panX = camera.panX,
                        panY = camera.panY
                    )
                    if (rect.width > 0f && rect.height > 0f) {
                        translate(left = rect.left, top = rect.top) {
                            scale(
                                scaleX = rect.width / bitmap.width.toFloat(),
                                scaleY = rect.height / bitmap.height.toFloat(),
                                pivot = Offset.Zero
                            ) {
                                drawImage(imageBitmap)
                            }
                        }
                    }
                    val startMs = zoneStartMs ?: return@Canvas
                    val stroke = 8.dp.toPx()
                    for (span in WalkRoute.spans(pins, rsrpSegments)) {
                        val from = WalkRoute.place(span.start, rect)
                        val to = WalkRoute.place(span.end, rect)
                        drawLine(
                            color = Color.White,
                            start = Offset(from.x, from.y),
                            end = Offset(to.x, to.y),
                            strokeWidth = stroke + 4f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(WalkRoute.rsrpRouteColor(span.bin)),
                            start = Offset(from.x, from.y),
                            end = Offset(to.x, to.y),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                    val endMs = markTimeMs ?: startMs
                    val pinPoints = pins.map { pin ->
                        WalkRoute.place(ImagePoint(pin.xFraction, pin.yFraction), rect)
                    }
                    val pinRadius = stroke * 1.1f
                    for (tick in WalkRoute.ticksOnRoute(pins, startMs, endMs)) {
                        val at = WalkRoute.place(tick.point, rect)
                        if (pinPoints.any { distance(it, at) < pinRadius * 1.6f }) continue
                        drawCircle(color = Color.White, radius = stroke * 0.7f, center = Offset(at.x, at.y))
                        drawCircle(color = Color.Black, radius = stroke * 0.35f, center = Offset(at.x, at.y))
                        val label = WalkRoute.elapsedLabel(tick.atMs - startMs)
                        val layout = textMeasurer.measure(
                            text = label,
                            style = labelStyle.merge(TextStyle(color = Color.Black))
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                x = (at.x - layout.size.width / 2f).coerceIn(
                                    0f,
                                    (size.width - layout.size.width).coerceAtLeast(0f)
                                ),
                                y = (at.y - pinRadius - layout.size.height).coerceAtLeast(0f)
                            )
                        )
                    }
                    pins.forEachIndexed { index, pin ->
                        val at = pinPoints[index]
                        val bin = if (index == pins.lastIndex && liveBin != null) {
                            liveBin
                        } else {
                            WalkRoute.binAt(rsrpSegments, pin.placedAtMs)
                        }
                        drawCircle(color = Color.White, radius = pinRadius + 2f, center = Offset(at.x, at.y))
                        drawCircle(
                            color = Color(WalkRoute.rsrpRouteColor(bin)),
                            radius = pinRadius,
                            center = Offset(at.x, at.y)
                        )
                        val label = WalkRoute.elapsedLabel(pin.placedAtMs - startMs)
                        val layout = textMeasurer.measure(
                            text = label,
                            style = labelStyle.merge(TextStyle(color = Color.Black))
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                x = (at.x - layout.size.width / 2f).coerceIn(
                                    0f,
                                    (size.width - layout.size.width).coerceAtLeast(0f)
                                ),
                                y = (at.y - pinRadius - layout.size.height - 2f).coerceAtLeast(0f)
                            )
                        )
                    }
                }
            }
            if (pins.isNotEmpty()) {
                Button(
                    onClick = onUndo,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(text = stringResource(R.string.zone_test_undo_mark))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val elapsed = if (zoneStartMs != null && markTimeMs != null) {
                WalkRoute.elapsedLabel(markTimeMs - zoneStartMs)
            } else {
                stringResource(R.string.zone_test_floor_plan_waiting)
            }
            Text(
                text = stringResource(R.string.zone_test_marks, pins.size, elapsed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(
                if (zoneStartMs == null) {
                    R.string.zone_test_floor_plan_before_start
                } else {
                    R.string.zone_test_floor_plan_tap
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Draws into the parent padding so the plan can reach the edges of its card. */
fun Modifier.expandPastPadding(padding: Dp): Modifier = layout { measurable, constraints ->
    val extra = padding.roundToPx()
    val width = constraints.maxWidth + extra * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = width, maxWidth = width)
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-extra, 0)
    }
}

private fun distance(a: ImagePoint, b: ImagePoint): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}
