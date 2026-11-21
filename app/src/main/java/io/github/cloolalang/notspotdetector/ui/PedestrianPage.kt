package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.ColoredRouteSpan
import io.github.cloolalang.notspotdetector.model.ImagePoint
import io.github.cloolalang.notspotdetector.model.PedestrianLive
import io.github.cloolalang.notspotdetector.model.PedestrianSample
import io.github.cloolalang.notspotdetector.model.PedestrianTrack
import io.github.cloolalang.notspotdetector.model.PlanCamera
import io.github.cloolalang.notspotdetector.model.WalkPin
import io.github.cloolalang.notspotdetector.model.WalkRoute
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Black page for a pedestrian walk. The start is the middle. North is up.
 * Pinch to zoom and drag to look around. The saved JPEG fits the whole trail.
 */
@Composable
fun PedestrianPage(
    pins: List<WalkPin>,
    samples: List<PedestrianSample>,
    spans: List<ColoredRouteSpan>,
    zoneStartMs: Long?,
    zoneEndMs: Long?,
    live: State<PedestrianLive>,
    modifier: Modifier = Modifier
) {
    var camera by remember { mutableStateOf(PlanCamera()) }
    val status by live
    val grid = PedestrianTrack.grid(PedestrianTrack.halfExtent(samples))
    val unknown = stringResource(R.string.pedestrian_live_unknown)
    val bearingText = status.bearingDegrees?.let { degrees ->
        stringResource(R.string.pedestrian_live_bearing, degrees)
    } ?: unknown
    val stepsText = status.steps?.let { count ->
        pluralStringResource(R.plurals.pedestrian_live_steps, count, count)
    } ?: unknown
    val distanceText = status.distanceMeters?.let { metres ->
        stringResource(R.string.pedestrian_live_distance, metres)
    } ?: unknown
    val gridText = stringResource(R.string.pedestrian_grid_size, grid.spacingMeters.roundToInt())
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = remember {
        TextStyle(color = Color.White, fontSize = 11.sp)
    }
    val readoutStyle = remember {
        TextStyle(color = Color.White, fontSize = 18.sp)
    }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val image = PedestrianTrack.PAGE_PIXELS.toFloat()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(screenHeight.coerceAtLeast(160.dp))
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                val viewWidth = { size.width.toFloat() }
                val viewHeight = { size.height.toFloat() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = down.position
                    var twoFinger = false
                    var fingersDown = true
                    while (fingersDown) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        val zoomed = camera.zoom > WalkRoute.MIN_ZOOM + 0.01f
                        if (pressed.size >= 2 || (pressed.size == 1 && zoomed && !twoFinger)) {
                            event.changes.forEach { change ->
                                if (change.positionChanged()) change.consume()
                            }
                        }
                        if (pressed.size >= 2) {
                            twoFinger = true
                            val pan = event.calculatePan()
                            if (pan != Offset.Zero) {
                                camera = WalkRoute.moveCentre(
                                    viewWidth(),
                                    viewHeight(),
                                    image,
                                    image,
                                    camera,
                                    pan.x,
                                    pan.y
                                )
                            }
                            val zoom = event.calculateZoom()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (centroid != Offset.Unspecified && zoom.isFinite() && abs(zoom - 1f) > 0.001f) {
                                camera = WalkRoute.zoomAbout(
                                    viewWidth(),
                                    viewHeight(),
                                    image,
                                    image,
                                    camera,
                                    centroid.x,
                                    centroid.y,
                                    zoom
                                )
                            }
                        } else if (pressed.size == 1 && !twoFinger) {
                            val change = pressed.first()
                            val dx = change.position.x - start.x
                            val dy = change.position.y - start.y
                            if (dx * dx + dy * dy > slop * slop && camera.zoom > WalkRoute.MIN_ZOOM + 0.01f) {
                                val delta = change.position - change.previousPosition
                                camera = WalkRoute.moveCentre(
                                    viewWidth(),
                                    viewHeight(),
                                    image,
                                    image,
                                    camera,
                                    delta.x,
                                    delta.y
                                )
                            }
                        }
                        fingersDown = event.changes.any { it.pressed }
                    }
                }
            }
    ) {
        drawRect(Color.Black)
        fun place(point: ImagePoint): Offset {
            val at = WalkRoute.imageToView(
                point = point,
                viewWidth = size.width,
                viewHeight = size.height,
                imageWidth = image,
                imageHeight = image,
                camera = camera
            )
            return Offset(at.x, at.y)
        }
        val gridStroke = (minOf(size.width, size.height) * 0.0025f).coerceIn(1f, 3f)
        fun drawGridLine(start: ImagePoint, end: ImagePoint, axis: Boolean) {
            drawLine(
                color = Color.White.copy(alpha = if (axis) 0.45f else 0.2f),
                start = place(start),
                end = place(end),
                strokeWidth = if (axis) gridStroke * 2f else gridStroke
            )
        }
        for (x in grid.vertical) {
            drawGridLine(ImagePoint(x, 0f), ImagePoint(x, 1f), axis = abs(x - 0.5f) < 0.004f)
        }
        for (y in grid.horizontal) {
            drawGridLine(ImagePoint(0f, y), ImagePoint(1f, y), axis = abs(y - 0.5f) < 0.004f)
        }
        val stroke = (minOf(size.width, size.height) * 0.012f).coerceIn(4f, 18f)
        for (span in spans) {
            val from = place(span.start)
            val to = place(span.end)
            drawLine(Color.White, from, to, stroke + 4f, cap = StrokeCap.Round)
            drawLine(Color(span.colorArgb.toInt()), from, to, stroke, cap = StrokeCap.Round)
        }
        val radius = stroke * 0.55f
        for (pin in pins) {
            val at = place(ImagePoint(pin.xFraction, pin.yFraction))
            drawCircle(Color.White, radius + 1.5f, at)
            drawCircle(Color.Black, radius, at)
        }
        val start = zoneStartMs
        val end = zoneEndMs
        if (start != null && end != null) {
            for (tick in WalkRoute.ticksOnRoute(pins, start, end)) {
                val at = place(tick.point)
                val text = WalkRoute.elapsedLabel(tick.atMs - start)
                val layout = textMeasurer.measure(text, labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        at.x - layout.size.width / 2f,
                        (at.y - radius - layout.size.height - 4f).coerceAtLeast(0f)
                    )
                )
            }
        }
        val north = textMeasurer.measure("N", labelStyle)
        val northAt = place(ImagePoint(0.5f, 0.04f))
        drawText(north, topLeft = Offset(northAt.x - north.size.width / 2f, northAt.y))

        val readout = listOf(bearingText, stepsText, distanceText, gridText).map { line ->
            textMeasurer.measure(line, readoutStyle)
        }
        val gap = 2f
        val pad = 14f
        val boxLeft = 16f
        val boxTop = 16f
        val textWidth = readout.maxOf { it.size.width }.toFloat()
        val textHeight = readout.sumOf { it.size.height } + gap * (readout.size - 1)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.14f),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(textWidth + pad * 2, textHeight + pad * 2),
            cornerRadius = CornerRadius(12f, 12f)
        )
        var textTop = boxTop + pad
        for (line in readout) {
            drawText(line, topLeft = Offset(boxLeft + pad, textTop))
            textTop += line.size.height + gap
        }
    }
}
