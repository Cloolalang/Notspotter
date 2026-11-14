package io.github.cloolalang.notspotdetector.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import io.github.cloolalang.notspotdetector.model.ContentRect
import io.github.cloolalang.notspotdetector.model.ImagePoint
import io.github.cloolalang.notspotdetector.model.PlanCamera
import io.github.cloolalang.notspotdetector.model.PrimaryCarrierHistory
import io.github.cloolalang.notspotdetector.model.PrimaryCarrierTapeEntry
import io.github.cloolalang.notspotdetector.model.RsrpBin
import io.github.cloolalang.notspotdetector.model.RsrpBinSegment
import io.github.cloolalang.notspotdetector.model.RsrqBinSegment
import io.github.cloolalang.notspotdetector.model.WalkPin
import io.github.cloolalang.notspotdetector.model.WalkRoute
import io.github.cloolalang.notspotdetector.model.WalkSnapMode
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlinx.coroutines.delay

/**
 * Floor plan for a zone test. Taps drop a mark at the zone clock. The line between
 * marks is coloured with the RSRP tape, and a dot every 30 seconds shares that clock.
 */
@Composable
fun WalkRouteMap(
    floorPlan: Bitmap?,
    pins: List<WalkPin>,
    rsrpSegments: List<RsrpBinSegment>,
    rsrqSegments: List<RsrqBinSegment> = emptyList(),
    carrierEntries: List<PrimaryCarrierTapeEntry> = emptyList(),
    zoneStartMs: Long?,
    markTimeMs: Long?,
    liveBin: RsrpBin?,
    marksFromMaster: Boolean = false,
    snapMode: WalkSnapMode = WalkSnapMode.DEFAULT,
    adjustableTrailMarker: Boolean = false,
    plotRsrq: Boolean = false,
    plotEarfcnPci: Boolean = false,
    onSnapModeChange: (WalkSnapMode) -> Unit = {},
    onAdjustableTrailMarkerChange: (Boolean) -> Unit = {},
    onPlotRsrqChange: (Boolean) -> Unit = {},
    onPlotEarfcnPciChange: (Boolean) -> Unit = {},
    onChooseFloorPlan: () -> Unit,
    onMark: (WalkPin) -> Unit,
    onMovePin: (Int, WalkPin) -> Unit = { _, _ -> },
    onRemovePin: (Int) -> Unit = {},
    onInsertPin: (Int, WalkPin) -> Unit = { _, _ -> },
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!marksFromMaster) {
            OutlinedButton(
                onClick = onChooseFloorPlan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.zone_test_floor_plan))
            }
        }
        if (floorPlan == null) {
            Text(
                text = stringResource(
                    if (marksFromMaster) {
                        R.string.zone_test_floor_plan_from_master
                    } else {
                        R.string.zone_test_floor_plan_hint
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        var moveArmed by remember { mutableStateOf(false) }
        var removeArmed by remember { mutableStateOf(false) }
        var addArmed by remember { mutableStateOf(false) }
        LaunchedEffect(moveArmed) {
            if (!moveArmed) return@LaunchedEffect
            delay(MOVE_MARKER_IDLE_MS)
            moveArmed = false
        }
        LaunchedEffect(removeArmed) {
            if (!removeArmed) return@LaunchedEffect
            delay(MOVE_MARKER_IDLE_MS)
            removeArmed = false
        }
        LaunchedEffect(addArmed) {
            if (!addArmed) return@LaunchedEffect
            delay(MOVE_MARKER_IDLE_MS)
            addArmed = false
        }
        val bitmap = floorPlan
        val planDescription = stringResource(R.string.zone_test_floor_plan_image)
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        var camera by remember(bitmap) { mutableStateOf(PlanCamera()) }
        var draftTip by remember(bitmap) { mutableStateOf<ImagePoint?>(null) }
        var movingIndex by remember(bitmap) { mutableStateOf<Int?>(null) }
        var movingPoint by remember(bitmap) { mutableStateOf<ImagePoint?>(null) }
        val markTime = rememberUpdatedState(markTimeMs)
        val canMark = rememberUpdatedState(zoneStartMs != null && markTimeMs != null)
        val mark = rememberUpdatedState(onMark)
        val movePin = rememberUpdatedState(onMovePin)
        val removePin = rememberUpdatedState(onRemovePin)
        val insertPin = rememberUpdatedState(onInsertPin)
        val pinsNow = rememberUpdatedState(pins)
        val snapNow = rememberUpdatedState(snapMode)
        val trailNow = rememberUpdatedState(adjustableTrailMarker)
        val moveArmedNow = rememberUpdatedState(moveArmed)
        val removeArmedNow = rememberUpdatedState(removeArmed)
        val addArmedNow = rememberUpdatedState(addArmed)
        val textMeasurer = rememberTextMeasurer()
        val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.Black)
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val trailDash = remember { PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f) }
        val hitSlopPx = with(LocalDensity.current) { 28.dp.toPx() }
        val displayPins = remember(pins, movingIndex, movingPoint) {
            val index = movingIndex
            val point = movingPoint
            if (index == null || point == null || index !in pins.indices) {
                pins
            } else {
                pins.toMutableList().also {
                    it[index] = pins[index].copy(xFraction = point.x, yFraction = point.y)
                }
            }
        }
        var controlsOnLeft by remember { mutableStateOf(true) }
        val mapHeight = screenHeight.coerceAtLeast(160.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!marksFromMaster && controlsOnLeft) {
                MapPlotControls(
                    modifier = Modifier.fillMaxHeight(),
                    adjustableTrailMarker = adjustableTrailMarker,
                    onAdjustableTrailMarkerChange = onAdjustableTrailMarkerChange,
                    showMarkActions = pins.isNotEmpty(),
                    canAddOnLeg = pins.size >= 2,
                    moveArmed = moveArmed,
                    removeArmed = removeArmed,
                    addArmed = addArmed,
                    snapMode = snapMode,
                    onSnapModeChange = onSnapModeChange,
                    dockOnLeft = true,
                    onMoveToOtherSide = { controlsOnLeft = false },
                    onUndo = onUndo,
                    onToggleMove = {
                        moveArmed = !moveArmed
                        if (moveArmed) {
                            removeArmed = false
                            addArmed = false
                        }
                    },
                    onToggleRemove = {
                        removeArmed = !removeArmed
                        if (removeArmed) {
                            moveArmed = false
                            addArmed = false
                        }
                    },
                    onToggleAdd = {
                        addArmed = !addArmed
                        if (addArmed) {
                            moveArmed = false
                            removeArmed = false
                        }
                    }
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = planDescription }
                    .pointerInput(
                        bitmap.width,
                        bitmap.height,
                        marksFromMaster,
                        snapMode,
                        adjustableTrailMarker,
                        moveArmed,
                        removeArmed,
                        addArmed
                    ) {
                        val slop = viewConfiguration.touchSlop
                        val viewW = { size.width.toFloat() }
                        val viewH = { size.height.toFloat() }
                        val imageW = bitmap.width.toFloat()
                        val imageH = bitmap.height.toFloat()
                        fun pointAt(x: Float, y: Float): ImagePoint? = WalkRoute.fractionInView(
                            tapX = x,
                            tapY = y,
                            viewWidth = viewW(),
                            viewHeight = viewH(),
                            imageWidth = imageW,
                            imageHeight = imageH,
                            camera = camera
                        )

                        fun placePoint(raw: ImagePoint, previous: WalkPin?): ImagePoint {
                            if (previous == null) {
                                return ImagePoint(raw.x.coerceIn(0f, 1f), raw.y.coerceIn(0f, 1f))
                            }
                            return WalkRoute.snapLeg(previous, raw, imageW, imageH, snapNow.value)
                        }

                        fun hitPinIndex(x: Float, y: Float): Int? {
                            var best: Int? = null
                            var bestDistance = hitSlopPx
                            pinsNow.value.forEachIndexed { index, pin ->
                                val at = WalkRoute.imageToView(
                                    point = ImagePoint(pin.xFraction, pin.yFraction),
                                    viewWidth = viewW(),
                                    viewHeight = viewH(),
                                    imageWidth = imageW,
                                    imageHeight = imageH,
                                    camera = camera
                                )
                                val distance = hypot(at.x - x, at.y - y)
                                if (distance <= bestDistance) {
                                    bestDistance = distance
                                    best = index
                                }
                            }
                            return best
                        }

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val start = down.position
                            var twoFinger = false
                            var dragged = false
                            var trailDrag = false
                            var fingersDown = true
                            val moveMode = !marksFromMaster && moveArmedNow.value
                            val removeMode = !marksFromMaster && removeArmedNow.value
                            val addMode = !marksFromMaster && addArmedNow.value
                            val toolArmed = moveMode || removeMode || addMode
                            val grabbedIndex = if (moveMode) hitPinIndex(start.x, start.y) else null
                            val removeIndex = if (removeMode) hitPinIndex(start.x, start.y) else null
                            val addInsert = if (addMode) {
                                WalkRoute.insertOnNearestLeg(
                                    pins = pinsNow.value,
                                    tapX = start.x,
                                    tapY = start.y,
                                    viewWidth = viewW(),
                                    viewHeight = viewH(),
                                    imageWidth = imageW,
                                    imageHeight = imageH,
                                    camera = camera,
                                    maxDistancePx = hitSlopPx
                                )
                            } else {
                                null
                            }
                            val trailActive =
                                !marksFromMaster && !toolArmed && trailNow.value && canMark.value
                            if (grabbedIndex != null) {
                                down.consume()
                                val pin = pinsNow.value[grabbedIndex]
                                movingIndex = grabbedIndex
                                movingPoint = ImagePoint(pin.xFraction, pin.yFraction)
                            } else if (removeIndex != null || addInsert != null) {
                                down.consume()
                            } else if (trailActive) {
                                down.consume()
                                pointAt(start.x, start.y)?.let {
                                    draftTip = placePoint(it, pinsNow.value.lastOrNull())
                                }
                            }
                            while (fingersDown) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressed = event.changes.filter { it.pressed }
                                val zoomed = camera.zoom > WalkRoute.MIN_ZOOM + 0.01f
                                val holdingPin = grabbedIndex != null && !twoFinger
                                val holdingRemove = removeIndex != null && !twoFinger
                                val holdingAdd = addInsert != null && !twoFinger
                                val holdForTrail = trailActive && pressed.size == 1 && !twoFinger
                                val panWithOneFinger =
                                    zoomed && !holdingPin && !holdingRemove && !holdingAdd && !trailActive
                                if (
                                    pressed.size >= 2 ||
                                    (pressed.size == 1 && panWithOneFinger && !twoFinger) ||
                                    holdingPin ||
                                    holdingRemove ||
                                    holdingAdd ||
                                    holdForTrail
                                ) {
                                    event.changes.forEach { change -> change.consume() }
                                }
                                if (pressed.size >= 2) {
                                    twoFinger = true
                                    draftTip = null
                                    trailDrag = false
                                    movingIndex = null
                                    movingPoint = null
                                    val pan = event.calculatePan()
                                    if (pan != Offset.Zero) {
                                        camera = WalkRoute.moveCentre(
                                            viewW(), viewH(), imageW, imageH, camera, pan.x, pan.y
                                        )
                                    }
                                    val zoom = event.calculateZoom()
                                    val rotation = event.calculateRotation()
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    if (centroid != Offset.Unspecified) {
                                        if (zoom.isFinite() && abs(zoom - 1f) > 0.001f) {
                                            camera = WalkRoute.zoomAbout(
                                                viewW(), viewH(), imageW, imageH,
                                                camera, centroid.x, centroid.y, zoom
                                            )
                                        }
                                        if (rotation.isFinite() && abs(rotation) > 0.01f) {
                                            camera = WalkRoute.rotateAbout(
                                                viewW(), viewH(), imageW, imageH,
                                                camera, centroid.x, centroid.y, rotation
                                            )
                                        }
                                    }
                                } else if (pressed.size == 1 && !twoFinger) {
                                    val change = pressed.first()
                                    val dx = change.position.x - start.x
                                    val dy = change.position.y - start.y
                                    val moved = dx * dx + dy * dy > slop * slop
                                    if (moved) dragged = true
                                    when {
                                        grabbedIndex != null -> {
                                            pointAt(change.position.x, change.position.y)?.let { raw ->
                                                val previous = pinsNow.value.getOrNull(grabbedIndex - 1)
                                                movingPoint = placePoint(raw, previous)
                                            }
                                        }
                                        removeIndex != null || addInsert != null -> Unit
                                        trailActive -> {
                                            if (moved) trailDrag = true
                                            pointAt(change.position.x, change.position.y)?.let {
                                                draftTip = placePoint(it, pinsNow.value.lastOrNull())
                                            }
                                        }
                                        moved && zoomed -> {
                                            val delta = change.position - change.previousPosition
                                            camera = WalkRoute.moveCentre(
                                                viewW(), viewH(), imageW, imageH, camera, delta.x, delta.y
                                            )
                                        }
                                    }
                                }
                                fingersDown = event.changes.any { it.pressed }
                            }
                            val tip = draftTip
                            val movedPoint = movingPoint
                            val movedIndex = movingIndex
                            draftTip = null
                            movingIndex = null
                            movingPoint = null
                            if (marksFromMaster || twoFinger) return@awaitEachGesture
                            if (addInsert != null) {
                                insertPin.value(addInsert.insertBeforeIndex, addInsert.pin)
                                addArmed = false
                                return@awaitEachGesture
                            }
                            if (removeIndex != null) {
                                removePin.value(removeIndex)
                                removeArmed = false
                                return@awaitEachGesture
                            }
                            if (grabbedIndex != null && movedIndex != null && movedPoint != null) {
                                val original = pinsNow.value.getOrNull(grabbedIndex) ?: return@awaitEachGesture
                                movePin.value(
                                    grabbedIndex,
                                    original.copy(xFraction = movedPoint.x, yFraction = movedPoint.y)
                                )
                                moveArmed = false
                                return@awaitEachGesture
                            }
                            if (toolArmed) return@awaitEachGesture
                            val atMs = markTime.value
                            if (!canMark.value || atMs == null) return@awaitEachGesture
                            when {
                                trailActive && tip != null && (trailDrag || !dragged) ->
                                    mark.value(WalkPin(atMs, tip.x, tip.y))
                                !trailActive && !dragged -> {
                                    val point = pointAt(start.x, start.y) ?: return@awaitEachGesture
                                    val placed = placePoint(point, pinsNow.value.lastOrNull())
                                    mark.value(WalkPin(atMs, placed.x, placed.y))
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val plan = WalkRoute.planSize(
                        viewWidth = size.width,
                        viewHeight = size.height,
                        imageWidth = bitmap.width.toFloat(),
                        imageHeight = bitmap.height.toFloat(),
                        zoom = camera.zoom
                    )
                    if (plan.width <= 0f || plan.height <= 0f) return@Canvas
                    val rect = ContentRect(0f, 0f, plan.width, plan.height)
                    val centreX = size.width / 2f + camera.panX
                    val centreY = size.height / 2f + camera.panY
                    translate(left = centreX, top = centreY) {
                        rotate(degrees = camera.rotationDegrees, pivot = Offset.Zero) {
                            translate(left = -plan.width / 2f, top = -plan.height / 2f) {
                                scale(
                                    scaleX = plan.width / bitmap.width.toFloat(),
                                    scaleY = plan.height / bitmap.height.toFloat(),
                                    pivot = Offset.Zero
                                ) {
                                    drawImage(imageBitmap)
                                }
                                val startMs = zoneStartMs ?: return@translate
                                val stroke = 8.dp.toPx()
                                val parallelStroke = stroke * 0.7f
                                val parallelGap = stroke * 1.6f
                                val rsrqOffset = if (plotRsrq) parallelGap else 0f
                                val carrierOffset = when {
                                    plotEarfcnPci && plotRsrq -> -parallelGap
                                    plotEarfcnPci -> parallelGap
                                    else -> 0f
                                }
                                if (plotRsrq) {
                                    val rsrqSpans = WalkRoute.coloredSpans(
                                        pins = displayPins,
                                        changeStartsMs = rsrqSegments.map { it.startedAtMs }
                                    ) { atMs ->
                                        WalkRoute.rsrqRouteColor(WalkRoute.rsrqBinAt(rsrqSegments, atMs))
                                    }
                                    for (span in rsrqSpans) {
                                        val placedStart = WalkRoute.place(span.start, rect)
                                        val placedEnd = WalkRoute.place(span.end, rect)
                                        val (from, to) = WalkRoute.offsetSpan(
                                            placedStart,
                                            placedEnd,
                                            rsrqOffset
                                        )
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.85f),
                                            start = Offset(from.x, from.y),
                                            end = Offset(to.x, to.y),
                                            strokeWidth = parallelStroke + 3f,
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = Color(span.colorArgb),
                                            start = Offset(from.x, from.y),
                                            end = Offset(to.x, to.y),
                                            strokeWidth = parallelStroke,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                                if (plotEarfcnPci) {
                                    val carrierSpans = WalkRoute.coloredSpans(
                                        pins = displayPins,
                                        changeStartsMs = carrierEntries.map { it.startedAtMs }
                                    ) { atMs ->
                                        val entry = WalkRoute.carrierEntryAt(carrierEntries, atMs)
                                        if (entry == null) WalkRoute.rsrpRouteColor(null)
                                        else PrimaryCarrierHistory.colorArgb(entry)
                                    }
                                    for (span in carrierSpans) {
                                        val placedStart = WalkRoute.place(span.start, rect)
                                        val placedEnd = WalkRoute.place(span.end, rect)
                                        val (from, to) = WalkRoute.offsetSpan(
                                            placedStart,
                                            placedEnd,
                                            carrierOffset
                                        )
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.85f),
                                            start = Offset(from.x, from.y),
                                            end = Offset(to.x, to.y),
                                            strokeWidth = parallelStroke + 3f,
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = Color(span.colorArgb),
                                            start = Offset(from.x, from.y),
                                            end = Offset(to.x, to.y),
                                            strokeWidth = parallelStroke,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                                for (span in WalkRoute.spans(displayPins, rsrpSegments)) {
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
                                val tip = draftTip
                                val previous = pins.lastOrNull()
                                if (tip != null) {
                                    val to = WalkRoute.place(tip, rect)
                                    if (previous != null) {
                                        val from = WalkRoute.place(
                                            ImagePoint(previous.xFraction, previous.yFraction),
                                            rect
                                        )
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.9f),
                                            start = Offset(from.x, from.y),
                                            end = Offset(to.x, to.y),
                                            strokeWidth = stroke + 4f,
                                            cap = StrokeCap.Round,
                                            pathEffect = trailDash
                                        )
                                        drawLine(
                                            color = Color(WalkRoute.rsrpRouteColor(liveBin))
                                                .copy(alpha = 0.85f),
                                            start = Offset(from.x, from.y),
                                            end = Offset(to.x, to.y),
                                            strokeWidth = stroke,
                                            cap = StrokeCap.Round,
                                            pathEffect = trailDash
                                        )
                                    }
                                    val tipRadius = stroke * 1.1f
                                    drawCircle(
                                        color = Color.White,
                                        radius = tipRadius + 2f,
                                        center = Offset(to.x, to.y)
                                    )
                                    drawCircle(
                                        color = Color(WalkRoute.rsrpRouteColor(liveBin)),
                                        radius = tipRadius,
                                        center = Offset(to.x, to.y)
                                    )
                                }
                                val endMs = markTimeMs ?: startMs
                                val pinPoints = displayPins.map { pin ->
                                    WalkRoute.place(ImagePoint(pin.xFraction, pin.yFraction), rect)
                                }
                                val pinRadius = stroke * 1.1f
                                for (tick in WalkRoute.ticksOnRoute(displayPins, startMs, endMs)) {
                                    val at = WalkRoute.place(tick.point, rect)
                                    if (pinPoints.any { distance(it, at) < pinRadius * 1.6f }) continue
                                    drawCircle(
                                        color = Color.White,
                                        radius = stroke * 0.7f,
                                        center = Offset(at.x, at.y)
                                    )
                                    drawCircle(
                                        color = Color.Black,
                                        radius = stroke * 0.35f,
                                        center = Offset(at.x, at.y)
                                    )
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
                                                (plan.width - layout.size.width).coerceAtLeast(0f)
                                            ),
                                            y = (at.y - pinRadius - layout.size.height).coerceAtLeast(0f)
                                        )
                                    )
                                }
                                displayPins.forEachIndexed { index, pin ->
                                    val at = pinPoints[index]
                                    val bin = if (index == displayPins.lastIndex && liveBin != null) {
                                        liveBin
                                    } else {
                                        WalkRoute.binAt(rsrpSegments, pin.placedAtMs)
                                    }
                                    drawCircle(
                                        color = Color.White,
                                        radius = pinRadius + 2f,
                                        center = Offset(at.x, at.y)
                                    )
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
                                                (plan.width - layout.size.width).coerceAtLeast(0f)
                                            ),
                                            y = (at.y - pinRadius - layout.size.height - 2f)
                                                .coerceAtLeast(0f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
            if (!marksFromMaster && !controlsOnLeft) {
                MapPlotControls(
                    modifier = Modifier.fillMaxHeight(),
                    adjustableTrailMarker = adjustableTrailMarker,
                    onAdjustableTrailMarkerChange = onAdjustableTrailMarkerChange,
                    showMarkActions = pins.isNotEmpty(),
                    canAddOnLeg = pins.size >= 2,
                    moveArmed = moveArmed,
                    removeArmed = removeArmed,
                    addArmed = addArmed,
                    snapMode = snapMode,
                    onSnapModeChange = onSnapModeChange,
                    dockOnLeft = false,
                    onMoveToOtherSide = { controlsOnLeft = true },
                    onUndo = onUndo,
                    onToggleMove = {
                        moveArmed = !moveArmed
                        if (moveArmed) {
                            removeArmed = false
                            addArmed = false
                        }
                    },
                    onToggleRemove = {
                        removeArmed = !removeArmed
                        if (removeArmed) {
                            moveArmed = false
                            addArmed = false
                        }
                    },
                    onToggleAdd = {
                        addArmed = !addArmed
                        if (addArmed) {
                            moveArmed = false
                            removeArmed = false
                        }
                    }
                )
            }
        }
        if (!marksFromMaster) {
            PlotToggleRow(
                label = stringResource(R.string.zone_test_plot_rsrq),
                checked = plotRsrq,
                onCheckedChange = onPlotRsrqChange
            )
            PlotToggleRow(
                label = stringResource(R.string.zone_test_plot_earfcn_pci),
                checked = plotEarfcnPci,
                onCheckedChange = onPlotEarfcnPciChange
            )
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
                when {
                    marksFromMaster -> R.string.zone_test_floor_plan_from_master
                    zoneStartMs == null -> R.string.zone_test_floor_plan_before_start
                    addArmed -> R.string.zone_test_add_marker_armed
                    removeArmed -> R.string.zone_test_remove_marker_armed
                    moveArmed -> R.string.zone_test_move_marker_armed
                    adjustableTrailMarker -> R.string.zone_test_floor_plan_trail
                    else -> R.string.zone_test_floor_plan_tap
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Sits beside the floor plan (left or right) so it never covers the map.
 * Pan and zoom only affect the plan next to these controls.
 */
@Composable
private fun MapPlotControls(
    adjustableTrailMarker: Boolean,
    onAdjustableTrailMarkerChange: (Boolean) -> Unit,
    showMarkActions: Boolean,
    canAddOnLeg: Boolean,
    moveArmed: Boolean,
    removeArmed: Boolean,
    addArmed: Boolean,
    snapMode: WalkSnapMode,
    onSnapModeChange: (WalkSnapMode) -> Unit,
    dockOnLeft: Boolean,
    onMoveToOtherSide: () -> Unit,
    onUndo: () -> Unit,
    onToggleMove: () -> Unit,
    onToggleRemove: () -> Unit,
    onToggleAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val armedColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFEF6C00),
        contentColor = Color.White
    )
    Surface(
        modifier = modifier.width(168.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TextButton(
                onClick = onMoveToOtherSide,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(
                        if (dockOnLeft) {
                            R.string.zone_test_plot_controls_to_right
                        } else {
                            R.string.zone_test_plot_controls_to_left
                        }
                    )
                )
            }
            Text(
                text = stringResource(R.string.zone_test_adjustable_trail),
                style = MaterialTheme.typography.labelLarge
            )
            Switch(
                checked = adjustableTrailMarker,
                onCheckedChange = onAdjustableTrailMarkerChange
            )
            Text(
                text = stringResource(R.string.zone_test_adjustable_trail_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showMarkActions) {
                Button(
                    onClick = onUndo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.zone_test_undo_mark))
                }
                Button(
                    onClick = onToggleMove,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (moveArmed) armedColors else ButtonDefaults.buttonColors()
                ) {
                    Text(text = stringResource(R.string.zone_test_move_marker))
                }
                Button(
                    onClick = onToggleRemove,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (removeArmed) armedColors else ButtonDefaults.buttonColors()
                ) {
                    Text(text = stringResource(R.string.zone_test_remove_marker))
                }
                if (canAddOnLeg) {
                    Button(
                        onClick = onToggleAdd,
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (addArmed) armedColors else ButtonDefaults.buttonColors()
                    ) {
                        Text(text = stringResource(R.string.zone_test_add_marker))
                    }
                }
            }
            Text(
                text = stringResource(R.string.zone_test_snap_label),
                style = MaterialTheme.typography.labelLarge
            )
            SnapChip(
                selected = snapMode == WalkSnapMode.Off,
                label = stringResource(R.string.zone_test_snap_off),
                onClick = { onSnapModeChange(WalkSnapMode.Off) }
            )
            SnapChip(
                selected = snapMode == WalkSnapMode.Degrees45,
                label = stringResource(R.string.zone_test_snap_45),
                onClick = { onSnapModeChange(WalkSnapMode.Degrees45) }
            )
            SnapChip(
                selected = snapMode == WalkSnapMode.Degrees22_5,
                label = stringResource(R.string.zone_test_snap_22_5),
                onClick = { onSnapModeChange(WalkSnapMode.Degrees22_5) }
            )
        }
    }
}

@Composable
private fun SnapChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PlotToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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

private const val MOVE_MARKER_IDLE_MS = 10_000L
