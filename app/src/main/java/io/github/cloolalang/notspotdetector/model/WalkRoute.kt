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
     * Where the floor plan is drawn after zoom and pan. Zoom 1 and no pan is the fitted picture.
     * [panX] and [panY] move the centre of the picture, in view pixels.
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
        val fitted = contentRect(viewWidth, viewHeight, imageWidth, imageHeight)
        if (fitted.width <= 0f || fitted.height <= 0f) return fitted
        val scale = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val width = fitted.width * scale
        val height = fitted.height * scale
        return ContentRect(
            left = viewWidth / 2f + panX - width / 2f,
            top = viewHeight / 2f + panY - height / 2f,
            width = width,
            height = height
        )
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
        val fitted = contentRect(viewWidth, viewHeight, imageWidth, imageHeight)
        if (fitted.width <= 0f || fitted.height <= 0f) return camera
        val width = fitted.width * camera.zoom
        val height = fitted.height * camera.zoom
        return camera.copy(
            panX = (camera.panX + deltaX).coerceIn(-panLimit(viewWidth, width), panLimit(viewWidth, width)),
            panY = (camera.panY + deltaY).coerceIn(-panLimit(viewHeight, height), panLimit(viewHeight, height))
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
        val current = visibleRect(
            viewWidth, viewHeight, imageWidth, imageHeight, camera.zoom, camera.panX, camera.panY
        )
        if (current.width <= 0f || current.height <= 0f) return camera
        val fractionX = ((centroidX - current.left) / current.width).coerceIn(0f, 1f)
        val fractionY = ((centroidY - current.top) / current.height).coerceIn(0f, 1f)
        val newZoom = (camera.zoom * zoomFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val fitted = contentRect(viewWidth, viewHeight, imageWidth, imageHeight)
        val newWidth = fitted.width * newZoom
        val newHeight = fitted.height * newZoom
        val left = centroidX - fractionX * newWidth
        val top = centroidY - fractionY * newHeight
        val panX = left + newWidth / 2f - viewWidth / 2f
        val panY = top + newHeight / 2f - viewHeight / 2f
        return PlanCamera(
            zoom = newZoom,
            panX = panX.coerceIn(-panLimit(viewWidth, newWidth), panLimit(viewWidth, newWidth)),
            panY = panY.coerceIn(-panLimit(viewHeight, newHeight), panLimit(viewHeight, newHeight))
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
        panY: Float = 0f
    ): ImagePoint? {
        val rect = visibleRect(
            viewWidth, viewHeight, imageWidth, imageHeight, zoom, panX, panY
        )
        if (rect.width <= 0f || rect.height <= 0f) return null
        if (tapX < rect.left || tapY < rect.top) return null
        if (tapX > rect.left + rect.width || tapY > rect.top + rect.height) return null
        return ImagePoint(
            x = (tapX - rect.left) / rect.width,
            y = (tapY - rect.top) / rect.height
        )
    }

    private fun panLimit(view: Float, content: Float): Float = abs(content - view) / 2f

    /**
     * Moves [tap] so the leg from [from] lies on the nearest horizontal, vertical, or 45-degree line.
     * The angle is measured in image pixels, which matches the line on screen.
     */
    fun snapLeg(
        from: WalkPin,
        tap: ImagePoint,
        imageWidth: Float,
        imageHeight: Float
    ): ImagePoint {
        if (imageWidth <= 0f || imageHeight <= 0f) return tap
        val x0 = from.xFraction * imageWidth
        val y0 = from.yFraction * imageHeight
        val dx = tap.x * imageWidth - x0
        val dy = tap.y * imageHeight - y0
        val distance = hypot(dx, dy)
        if (distance < 1f) return tap
        val step = PI / 4.0
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
        if (pins.size < 2) return emptyList()
        val orderedPins = pins.sortedBy { it.placedAtMs }
        val orderedSegments = segments.sortedBy { it.startedAtMs }
        val result = ArrayList<RouteSpan>()
        for (index in 0 until orderedPins.lastIndex) {
            val from = orderedPins[index]
            val to = orderedPins[index + 1]
            val startMs = from.placedAtMs
            val endMs = to.placedAtMs
            if (endMs <= startMs) continue
            val cuts = ArrayList<Long>(orderedSegments.size + 2)
            cuts.add(startMs)
            cuts.add(endMs)
            for (segment in orderedSegments) {
                if (segment.startedAtMs > startMs && segment.startedAtMs < endMs) {
                    cuts.add(segment.startedAtMs)
                }
            }
            val unique = cuts.distinct().sorted()
            val spanMs = (endMs - startMs).toFloat()
            for (cut in 0 until unique.lastIndex) {
                val left = unique[cut]
                val right = unique[cut + 1]
                result.add(
                    RouteSpan(
                        start = lerp(from, to, (left - startMs).toFloat() / spanMs),
                        end = lerp(from, to, (right - startMs).toFloat() / spanMs),
                        bin = binAt(orderedSegments, left)
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

    private fun lerp(from: WalkPin, to: WalkPin, fraction: Float): ImagePoint {
        return ImagePoint(
            x = from.xFraction + (to.xFraction - from.xFraction) * fraction,
            y = from.yFraction + (to.yFraction - from.yFraction) * fraction
        )
    }

    private const val MAX_TICKS = 2_000
}

/** Zoom and pan of the floor plan on screen. Zoom 1 fits the picture in the view. */
data class PlanCamera(
    val zoom: Float = WalkRoute.MIN_ZOOM,
    val panX: Float = 0f,
    val panY: Float = 0f
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

data class RouteTick(
    val atMs: Long,
    val point: ImagePoint
)

data class WalkLegendSwatch(
    val color: Long,
    val label: String
)
