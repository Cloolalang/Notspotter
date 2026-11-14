package io.github.cloolalang.notspotdetector.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sin

/**
 * A walk on a floor plan. Pins are where the user tapped, in image fractions.
 * The line between two pins is coloured from the RSRP tape for that stretch of time.
 * Marks every 30 seconds use the same clock as the zone-test tapes.
 */
object WalkRoute {
    const val TICK_STEP_MS = 30_000L
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 32f

    fun elapsedLabel(elapsedMs: Long): String {
        val totalSeconds = elapsedMs.coerceAtLeast(0L) / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }

    /**
     * How often a time label is drawn under the tapes. The tick lines stay every 30 seconds.
     * A short walk can show every mark. A long one labels whole minutes so the words still fit.
     */
    fun labelStepMs(durationMs: Long): Long {
        return when {
            durationMs <= 5L * 60_000L -> TICK_STEP_MS
            durationMs <= 20L * 60_000L -> 60_000L
            else -> 120_000L
        }
    }

    fun tickTimes(startMs: Long, endMs: Long, stepMs: Long = TICK_STEP_MS): List<Long> {
        if (stepMs <= 0L || endMs < startMs) return emptyList()
        val ticks = ArrayList<Long>()
        var time = startMs
        while (time <= endMs && ticks.size < MAX_TICKS) {
            ticks.add(time)
            val next = time + stepMs
            if (next <= time) break
            time = next
        }
        return ticks
    }

    fun contentRect(
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): ContentRect {
        if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
            return ContentRect(0f, 0f, 0f, 0f)
        }
        val scale = minOf(viewWidth / imageWidth, viewHeight / imageHeight)
        val width = imageWidth * scale
        val height = imageHeight * scale
        return ContentRect(
            left = (viewWidth - width) / 2f,
            top = (viewHeight - height) / 2f,
            width = width,
            height = height
        )
    }

    /**
     * Where the floor plan is drawn after zoom and pan when rotation is zero.
     * Zoom 1 and no pan is the fitted picture. [panX] and [panY] move the centre,
     * in view pixels. With rotation, use [planSize] plus [imageToView] / [fractionInView].
     */
    fun visibleRect(
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        zoom: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f
    ): ContentRect {
        val size = planSize(viewWidth, viewHeight, imageWidth, imageHeight, zoom)
        if (size.width <= 0f || size.height <= 0f) return size
        return ContentRect(
            left = viewWidth / 2f + panX - size.width / 2f,
            top = viewHeight / 2f + panY - size.height / 2f,
            width = size.width,
            height = size.height
        )
    }

    /** Drawn size of the plan at [zoom], before pan and rotation. */
    fun planSize(
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        zoom: Float = 1f
    ): ContentRect {
        val fitted = contentRect(viewWidth, viewHeight, imageWidth, imageHeight)
        if (fitted.width <= 0f || fitted.height <= 0f) return fitted
        val scale = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        return ContentRect(0f, 0f, fitted.width * scale, fitted.height * scale)
    }

    /** Keeps the picture from sliding off the view. A zoomed plan still covers the view. */
    fun moveCentre(
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        camera: PlanCamera,
        deltaX: Float,
        deltaY: Float
    ): PlanCamera {
        return clampPan(
            viewWidth,
            viewHeight,
            imageWidth,
            imageHeight,
            camera.copy(panX = camera.panX + deltaX, panY = camera.panY + deltaY)
        )
    }

    /**
     * Scales about the point under [centroidX], [centroidY], so that spot stays under the fingers.
     * [zoomFactor] above 1 zooms in. Below 1 zooms out, and the picture never shrinks past a fit.
     */
    fun zoomAbout(
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        camera: PlanCamera,
        centroidX: Float,
        centroidY: Float,
        zoomFactor: Float
    ): PlanCamera {
        if (!zoomFactor.isFinite() || zoomFactor <= 0f) return camera
        val under = fractionInView(
            tapX = centroidX,
            tapY = centroidY,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            camera = camera
        )
        val newZoom = (camera.zoom * zoomFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val zoomed = camera.copy(zoom = newZoom)
        if (under == null) return clampPan(viewWidth, viewHeight, imageWidth, imageHeight, zoomed)
        return keepPointUnder(
            viewWidth, viewHeight, imageWidth, imageHeight, zoomed, under, centroidX, centroidY
        )
    }

    /**
     * Rotates about the point under [centroidX], [centroidY], so that spot stays under the fingers.
     * [deltaDegrees] is the change from the gesture (Compose [calculateRotation]).
     */
    fun rotateAbout(
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        camera: PlanCamera,
        centroidX: Float,
        centroidY: Float,
        deltaDegrees: Float
    ): PlanCamera {
        if (!deltaDegrees.isFinite() || abs(deltaDegrees) < 0.001f) return camera
        val under = fractionInView(
            tapX = centroidX,
            tapY = centroidY,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            camera = camera
        )
        val rotated = camera.copy(
            rotationDegrees = normalizeDegrees(camera.rotationDegrees + deltaDegrees)
        )
        if (under == null) return clampPan(viewWidth, viewHeight, imageWidth, imageHeight, rotated)
        return keepPointUnder(
            viewWidth, viewHeight, imageWidth, imageHeight, rotated, under, centroidX, centroidY
        )
    }

    /** View-pixel position of an image fraction after zoom, pan, and rotation. */
    fun imageToView(
        point: ImagePoint,
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        camera: PlanCamera
    ): ImagePoint {
        val size = planSize(viewWidth, viewHeight, imageWidth, imageHeight, camera.zoom)
        val localX = point.x * size.width - size.width / 2f
        val localY = point.y * size.height - size.height / 2f
        val rad = camera.rotationDegrees * PI.toFloat() / 180f
        val cos = cos(rad)
        val sin = sin(rad)
        return ImagePoint(
            x = viewWidth / 2f + camera.panX + localX * cos - localY * sin,
            y = viewHeight / 2f + camera.panY + localX * sin + localY * cos
        )
    }

    /** Tap point as a fraction of the floor plan. Null when the tap misses the picture. */
    fun fractionInView(
        tapX: Float,
        tapY: Float,
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        zoom: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f,
        rotationDegrees: Float = 0f
    ): ImagePoint? {
        return fractionInView(
            tapX = tapX,
            tapY = tapY,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            camera = PlanCamera(zoom = zoom, panX = panX, panY = panY, rotationDegrees = rotationDegrees)
        )
    }

    fun fractionInView(
        tapX: Float,
        tapY: Float,
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        camera: PlanCamera
    ): ImagePoint? {
        val size = planSize(viewWidth, viewHeight, imageWidth, imageHeight, camera.zoom)
        if (size.width <= 0f || size.height <= 0f) return null
        val dx = tapX - (viewWidth / 2f + camera.panX)
        val dy = tapY - (viewHeight / 2f + camera.panY)
        val rad = -camera.rotationDegrees * PI.toFloat() / 180f
        val cos = cos(rad)
        val sin = sin(rad)
        val localX = dx * cos - dy * sin
        val localY = dx * sin + dy * cos
        val x = (localX + size.width / 2f) / size.width
        val y = (localY + size.height / 2f) / size.height
        if (x < 0f || y < 0f || x > 1f || y > 1f) return null
        return ImagePoint(x = x, y = y)
    }

    private fun keepPointUnder(
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        camera: PlanCamera,
        under: ImagePoint,
        centroidX: Float,
        centroidY: Float
    ): PlanCamera {
        val at = imageToView(under, viewWidth, viewHeight, imageWidth, imageHeight, camera)
        return clampPan(
            viewWidth,
            viewHeight,
            imageWidth,
            imageHeight,
            camera.copy(
                panX = camera.panX + (centroidX - at.x),
                panY = camera.panY + (centroidY - at.y)
            )
        )
    }

    private fun clampPan(
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        camera: PlanCamera
    ): PlanCamera {
        val size = planSize(viewWidth, viewHeight, imageWidth, imageHeight, camera.zoom)
        if (size.width <= 0f || size.height <= 0f) return camera
        val (aabbWidth, aabbHeight) = rotatedAabb(size.width, size.height, camera.rotationDegrees)
        return camera.copy(
            panX = camera.panX.coerceIn(-panLimit(viewWidth, aabbWidth), panLimit(viewWidth, aabbWidth)),
            panY = camera.panY.coerceIn(-panLimit(viewHeight, aabbHeight), panLimit(viewHeight, aabbHeight))
        )
    }

    /**
     * Axis-aligned bounds of a [width]×[height] plan after [rotationDegrees].
     * Used so pan limits still reach the edges when the plan is rotated (e.g. 90°).
     */
    fun rotatedAabb(width: Float, height: Float, rotationDegrees: Float): Pair<Float, Float> {
        val rad = rotationDegrees * PI.toFloat() / 180f
        val cosAbs = abs(cos(rad))
        val sinAbs = abs(sin(rad))
        return (width * cosAbs + height * sinAbs) to (width * sinAbs + height * cosAbs)
    }

    private fun panLimit(view: Float, content: Float): Float = abs(content - view) / 2f

    fun normalizeDegrees(degrees: Float): Float {
        var value = degrees % 360f
        if (value > 180f) value -= 360f
        if (value <= -180f) value += 360f
        return value
    }

    /**
     * Moves [tap] so the leg from [from] lies on the nearest snap angle from [mode].
     * The angle is measured in image pixels, which matches the line on screen.
     * [WalkSnapMode.Off] returns [tap] unchanged.
     */
    fun snapLeg(
        from: WalkPin,
        tap: ImagePoint,
        imageWidth: Float,
        imageHeight: Float,
        mode: WalkSnapMode = WalkSnapMode.DEFAULT
    ): ImagePoint {
        val step = mode.stepRadians ?: return tap
        if (imageWidth <= 0f || imageHeight <= 0f) return tap
        val x0 = from.xFraction * imageWidth
        val y0 = from.yFraction * imageHeight
        val dx = tap.x * imageWidth - x0
        val dy = tap.y * imageHeight - y0
        val distance = hypot(dx, dy)
        if (distance < 1f) return tap
        val snapped = round(atan2(dy.toDouble(), dx.toDouble()) / step) * step
        val x = (x0 + cos(snapped) * distance) / imageWidth
        val y = (y0 + sin(snapped) * distance) / imageHeight
        return ImagePoint(
            x = x.toFloat().coerceIn(0f, 1f),
            y = y.toFloat().coerceIn(0f, 1f)
        )
    }

    fun place(point: ImagePoint, rect: ContentRect): ImagePoint {
        return ImagePoint(
            x = rect.left + point.x * rect.width,
            y = rect.top + point.y * rect.height
        )
    }

    /**
     * Straight legs between pins, split where the RSRP band changes.
     * A band that started before the leg still colours the start of that leg.
     */
    fun spans(pins: List<WalkPin>, segments: List<RsrpBinSegment>): List<RouteSpan> {
        return timedSpans(pins, segments.map { it.startedAtMs }) { atMs ->
            binAt(segments, atMs)
        }.map { (start, end, bin) -> RouteSpan(start, end, bin) }
    }

    /** Parallel overlay: same geometry as [spans], coloured by [colorAt] at each cut. */
    fun coloredSpans(
        pins: List<WalkPin>,
        changeStartsMs: List<Long>,
        colorAt: (Long) -> Long
    ): List<ColoredRouteSpan> {
        return timedSpans(pins, changeStartsMs) { atMs -> colorAt(atMs) }
            .map { (start, end, color) -> ColoredRouteSpan(start, end, color) }
    }

    private fun <T> timedSpans(
        pins: List<WalkPin>,
        changeStartsMs: List<Long>,
        sampleAt: (Long) -> T
    ): List<Triple<ImagePoint, ImagePoint, T>> {
        if (pins.size < 2) return emptyList()
        val orderedPins = pins.sortedBy { it.placedAtMs }
        val changes = changeStartsMs.sorted()
        val result = ArrayList<Triple<ImagePoint, ImagePoint, T>>()
        for (index in 0 until orderedPins.lastIndex) {
            val from = orderedPins[index]
            val to = orderedPins[index + 1]
            val startMs = from.placedAtMs
            val endMs = to.placedAtMs
            if (endMs <= startMs) continue
            val cuts = ArrayList<Long>(changes.size + 2)
            cuts.add(startMs)
            cuts.add(endMs)
            for (changeMs in changes) {
                if (changeMs > startMs && changeMs < endMs) cuts.add(changeMs)
            }
            val unique = cuts.distinct().sorted()
            val spanMs = (endMs - startMs).toFloat()
            for (cut in 0 until unique.lastIndex) {
                val left = unique[cut]
                val right = unique[cut + 1]
                result.add(
                    Triple(
                        lerp(from, to, (left - startMs).toFloat() / spanMs),
                        lerp(from, to, (right - startMs).toFloat() / spanMs),
                        sampleAt(left)
                    )
                )
            }
        }
        return result
    }

    /**
     * 30-second marks that fall on a drawn leg. Marks before the first pin and after
     * the last pin have no place on the plan.
     */
    fun ticksOnRoute(pins: List<WalkPin>, zoneStartMs: Long, zoneEndMs: Long): List<RouteTick> {
        if (pins.size < 2) return emptyList()
        val orderedPins = pins.sortedBy { it.placedAtMs }
        val firstMs = orderedPins.first().placedAtMs
        val lastMs = orderedPins.last().placedAtMs
        if (lastMs <= firstMs) return emptyList()
        return tickTimes(zoneStartMs, zoneEndMs).mapNotNull { tickMs ->
            if (tickMs < firstMs || tickMs > lastMs) return@mapNotNull null
            val leg = orderedPins.zipWithNext().firstOrNull { (from, to) ->
                tickMs >= from.placedAtMs && tickMs <= to.placedAtMs && to.placedAtMs > from.placedAtMs
            } ?: return@mapNotNull null
            val (from, to) = leg
            val spanMs = to.placedAtMs - from.placedAtMs
            val fraction = (tickMs - from.placedAtMs).toFloat() / spanMs.toFloat()
            RouteTick(atMs = tickMs, point = lerp(from, to, fraction))
        }
    }

    fun binAt(segments: List<RsrpBinSegment>, timeMs: Long): RsrpBin? {
        val ordered = segments.sortedBy { it.startedAtMs }
        val index = ordered.indexOfLast { it.startedAtMs <= timeMs }
        if (index < 0) return null
        return ordered[index].bin
    }

    /** Same colours as the RSRP tape, packed as ARGB. */
    fun rsrpRouteColor(bin: RsrpBin?): Long {
        return when (bin) {
            RsrpBin.ABOVE_95 -> 0xFF81D4FA
            RsrpBin.ABOVE_105 -> 0xFF66BB6A
            RsrpBin.ABOVE_115 -> 0xFFFFEB3B
            RsrpBin.BELOW_115 -> 0xFFFF1744
            RsrpBin.NO_SIGNAL, null -> 0xFF90A4AE
        }
    }

    /** Same colours as the RSRQ tape, packed as ARGB. */
    fun rsrqRouteColor(bin: RsrqBin?): Long {
        return when (bin) {
            RsrqBin.ABOVE_15 -> 0xFF66BB6A
            RsrqBin.FROM_17_TO_15 -> 0xFFFFEB3B
            RsrqBin.BELOW_17 -> 0xFFFF1744
            RsrqBin.NO_SIGNAL, RsrqBin.NO_RSRQ, null -> 0xFF90A4AE
        }
    }

    fun rsrqBinAt(segments: List<RsrqBinSegment>, timeMs: Long): RsrqBin? {
        val ordered = segments.sortedBy { it.startedAtMs }
        val index = ordered.indexOfLast { it.startedAtMs <= timeMs }
        if (index < 0) return null
        return ordered[index].bin
    }

    fun carrierEntryAt(entries: List<PrimaryCarrierTapeEntry>, timeMs: Long): PrimaryCarrierTapeEntry? {
        val ordered = entries.sortedBy { it.startedAtMs }
        val index = ordered.indexOfLast { it.startedAtMs <= timeMs }
        if (index < 0) return null
        return ordered[index]
    }

    /**
     * Shift a drawn segment sideways by [offsetPx] (positive = left of the travel direction).
     * Used so RSRQ and EARFCN/PCI trails sit parallel to the RSRP walk.
     */
    fun offsetSpan(
        start: ImagePoint,
        end: ImagePoint,
        offsetPx: Float
    ): Pair<ImagePoint, ImagePoint> {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = hypot(dx, dy)
        if (len < 0.001f || offsetPx == 0f) return start to end
        val nx = -dy / len * offsetPx
        val ny = dx / len * offsetPx
        return ImagePoint(start.x + nx, start.y + ny) to ImagePoint(end.x + nx, end.y + ny)
    }

    /**
     * Nearest walk leg under the tap, for Add marker. Returns a pin on that leg and the
     * list index of the later endpoint so the new mark can be inserted before it.
     */
    fun insertOnNearestLeg(
        pins: List<WalkPin>,
        tapX: Float,
        tapY: Float,
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        camera: PlanCamera,
        maxDistancePx: Float
    ): LegInsert? {
        if (pins.size < 2 || maxDistancePx <= 0f) return null
        val ordered = pins.withIndex().sortedBy { it.value.placedAtMs }
        var bestDistance = maxDistancePx
        var best: LegInsert? = null
        for (index in 0 until ordered.lastIndex) {
            val from = ordered[index]
            val to = ordered[index + 1]
            if (to.value.placedAtMs <= from.value.placedAtMs) continue
            val start = imageToView(
                ImagePoint(from.value.xFraction, from.value.yFraction),
                viewWidth, viewHeight, imageWidth, imageHeight, camera
            )
            val end = imageToView(
                ImagePoint(to.value.xFraction, to.value.yFraction),
                viewWidth, viewHeight, imageWidth, imageHeight, camera
            )
            val dx = end.x - start.x
            val dy = end.y - start.y
            val len2 = dx * dx + dy * dy
            if (len2 < 1f) continue
            val t = (((tapX - start.x) * dx + (tapY - start.y) * dy) / len2).coerceIn(0.02f, 0.98f)
            val px = start.x + dx * t
            val py = start.y + dy * t
            val distance = hypot(tapX - px, tapY - py)
            if (distance > bestDistance) continue
            val spanMs = to.value.placedAtMs - from.value.placedAtMs
            val atMs = if (spanMs <= 1L) {
                from.value.placedAtMs
            } else {
                from.value.placedAtMs + (spanMs * t).toLong().coerceIn(1L, spanMs - 1L)
            }
            val point = lerp(from.value, to.value, t)
            bestDistance = distance
            best = LegInsert(
                pin = WalkPin(
                    placedAtMs = atMs,
                    xFraction = point.x.coerceIn(0f, 1f),
                    yFraction = point.y.coerceIn(0f, 1f)
                ),
                insertBeforeIndex = to.index
            )
        }
        return best
    }

    private fun lerp(from: WalkPin, to: WalkPin, fraction: Float): ImagePoint {
        return ImagePoint(
            x = from.xFraction + (to.xFraction - from.xFraction) * fraction,
            y = from.yFraction + (to.yFraction - from.yFraction) * fraction
        )
    }

    private const val MAX_TICKS = 2_000
}

/** Result of [WalkRoute.insertOnNearestLeg]: insert [pin] before [insertBeforeIndex]. */
data class LegInsert(
    val pin: WalkPin,
    val insertBeforeIndex: Int
)

/** Zoom, pan, and rotation of the floor plan on screen. Zoom 1 fits the picture in the view. */
data class PlanCamera(
    val zoom: Float = WalkRoute.MIN_ZOOM,
    val panX: Float = 0f,
    val panY: Float = 0f,
    /** Clockwise degrees in the view (Compose canvas rotate). */
    val rotationDegrees: Float = 0f
)

data class WalkPin(
    val placedAtMs: Long,
    val xFraction: Float,
    val yFraction: Float
)

data class ImagePoint(
    val x: Float,
    val y: Float
)

data class ContentRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

data class RouteSpan(
    val start: ImagePoint,
    val end: ImagePoint,
    val bin: RsrpBin?
)

data class ColoredRouteSpan(
    val start: ImagePoint,
    val end: ImagePoint,
    val colorArgb: Long
)

data class RouteTick(
    val atMs: Long,
    val point: ImagePoint
)

data class WalkLegendSwatch(
    val color: Long,
    val label: String
)

/**
 * Angle steps for floor-plan walk legs. Off leaves the tap where it is.
 * Degrees45 is horizontal, vertical, and 45°. Degrees22_5 also allows 22.5°.
 */
enum class WalkSnapMode {
    Off,
    Degrees45,
    Degrees22_5;

    /** Radians between allowed angles, or null when snapping is off. */
    val stepRadians: Double?
        get() = when (this) {
            Off -> null
            Degrees45 -> PI / 4.0
            Degrees22_5 -> PI / 8.0
        }

    companion object {
        val DEFAULT = Degrees45

        fun fromStoredName(name: String?): WalkSnapMode {
            if (name.isNullOrBlank()) return DEFAULT
            return entries.find { it.name == name } ?: DEFAULT
        }
    }
}
