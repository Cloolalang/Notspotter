package io.github.cloolalang.notspotdetector.model

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A walk with no floor plan. Positions are metres right and forward of the start.
 * Forward is the direction the top of the phone pointed when the zone test started.
 * Up on the page is forward. A step moves [stride] metres along the phone's heading
 * relative to that start.
 */
object PedestrianTrack {
    const val SAMPLE_STEP_MS = 1_000L
    const val DEFAULT_STRIDE_METERS = 0.75f
    const val MIN_STRIDE_METERS = 0.40f
    val HEADING_SNAP_CHOICES = floatArrayOf(22f, 45f, 90f)
    const val DEFAULT_HEADING_SNAP_DEGREES = 22f
    const val MAX_STRIDE_METERS = 1.20f
    /** Keeps the first steps from filling the page. */
    const val MIN_HALF_EXTENT_METERS = 4f
    const val PAGE_MARGIN = 0.08f
    const val PAGE_PIXELS = 1080
    /** After a step-detector event, accelerometer peaks wait this long so one footfall is counted once. */
    const val DETECTOR_QUIET_MS = 1_200L

    /**
     * An accelerometer peak counts when the step detector is absent, has not fired yet,
     * or last fired at least [DETECTOR_QUIET_MS] before [nowMs].
     */
    fun accelerometerMayCountStep(
        detectorPresent: Boolean,
        lastDetectorStepMs: Long,
        nowMs: Long
    ): Boolean {
        if (!detectorPresent) return true
        if (lastDetectorStepMs == Long.MIN_VALUE) return true
        return nowMs - lastDetectorStepMs >= DETECTOR_QUIET_MS
    }

    /** Compass bearing in degrees, 0 to 359. 0 is north. */
    fun bearingDegrees(azimuth: Float?): Int? {
        if (azimuth == null || !azimuth.isFinite()) return null
        return normalizeHeading(azimuth).roundToInt() % 360
    }

    /** Path length through [samples], in metres. Repeated positions add nothing. */
    fun walkedMeters(samples: List<PedestrianSample>): Float {
        var total = 0f
        for (index in 1 until samples.size) {
            val east = samples[index].eastMeters - samples[index - 1].eastMeters
            val north = samples[index].northMeters - samples[index - 1].northMeters
            val step = hypot(east, north)
            if (step.isFinite()) total += step
        }
        return total
    }

    const val GRAVITY_MPS2 = 9.80665f

    /** Acceleration along world up. [upX], [upY], [upZ] is that direction in phone axes. */
    fun verticalAcceleration(
        x: Float,
        y: Float,
        z: Float,
        upX: Float,
        upY: Float,
        upZ: Float
    ): Float = x * upX + y * upY + z * upZ

    /** Removes gravity from a raw accelerometer sample. The result is in phone axes. */
    fun removeGravity(
        x: Float,
        y: Float,
        z: Float,
        upX: Float,
        upY: Float,
        upZ: Float
    ): FloatArray {
        return floatArrayOf(
            x - GRAVITY_MPS2 * upX,
            y - GRAVITY_MPS2 * upY,
            z - GRAVITY_MPS2 * upZ
        )
    }

    fun coerceStride(meters: Float): Float {
        if (!meters.isFinite()) return DEFAULT_STRIDE_METERS
        return meters.coerceIn(MIN_STRIDE_METERS, MAX_STRIDE_METERS)
    }

    /** Snap strength in degrees. The choices are 22, 45, and 90. */
    fun coerceHeadingSnapDegrees(degrees: Float): Float {
        if (!degrees.isFinite()) return DEFAULT_HEADING_SNAP_DEGREES
        return HEADING_SNAP_CHOICES.minBy { abs(it - degrees) }
    }

    fun headingSnapChoiceIndex(degrees: Float): Int {
        val choice = coerceHeadingSnapDegrees(degrees)
        val index = HEADING_SNAP_CHOICES.indexOfFirst { it == choice }
        return if (index < 0) 0 else index
    }

    /** Signed turn from [from] to [to], in degrees, from -180 to 180. */
    fun headingDelta(from: Float, to: Float): Float {
        var delta = normalizeHeading(to) - normalizeHeading(from)
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    /** Clockwise degrees from the starting aim [startDegrees] to [currentDegrees]. 0 is forward. */
    fun relativeHeading(startDegrees: Float, currentDegrees: Float): Float {
        return normalizeHeading(headingDelta(startDegrees, currentDegrees))
    }

    /** Heading in degrees, wrapped to 0 until 360. 0 is north. */
    fun normalizeHeading(degrees: Float): Float {
        if (!degrees.isFinite()) return 0f
        var value = degrees % 360f
        if (value < 0f) value += 360f
        return value
    }

    /**
     * Moves [current] a fraction of the way toward [sample], across the 0/360 wrap.
     * A null [current] takes [sample] as the first heading.
     */
    fun smoothHeading(current: Float?, sample: Float, alpha: Float = 0.35f): Float {
        val next = normalizeHeading(sample)
        val from = current?.let(::normalizeHeading) ?: return next
        val weight = alpha.coerceIn(0f, 1f)
        var delta = next - from
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return normalizeHeading(from + delta * weight)
    }

    fun afterStep(
        position: PedestrianPosition,
        azimuthDegrees: Float,
        strideMeters: Float
    ): PedestrianPosition {
        val stride = coerceStride(strideMeters)
        val rad = Math.toRadians(normalizeHeading(azimuthDegrees).toDouble())
        return PedestrianPosition(
            eastMeters = position.eastMeters + (sin(rad) * stride).toFloat(),
            northMeters = position.northMeters + (cos(rad) * stride).toFloat()
        )
    }

    /**
     * Adds the position at [elapsedMs] when a full second has passed since the last mark.
     * The same position is kept when nobody has stepped.
     */
    fun appendSample(
        samples: List<PedestrianSample>,
        elapsedMs: Long,
        position: PedestrianPosition
    ): List<PedestrianSample> {
        if (elapsedMs < 0L) return samples
        val last = samples.lastOrNull()
        if (last != null && elapsedMs < last.elapsedMs + SAMPLE_STEP_MS) return samples
        return samples + PedestrianSample(
            elapsedMs = elapsedMs,
            eastMeters = position.eastMeters,
            northMeters = position.northMeters
        )
    }

    fun halfExtent(samples: List<PedestrianSample>): Float {
        var farthest = MIN_HALF_EXTENT_METERS
        for (sample in samples) {
            val reach = max(abs(sample.eastMeters), abs(sample.northMeters))
            if (reach > farthest) farthest = reach
        }
        return farthest
    }

    /**
     * Square size for the page grid. Picks 1, 2, 5, 10… metres so about four or more
     * squares fit across the page. A short walk uses a smaller square than 5 m.
     */
    fun gridSpacingMeters(halfExtentMeters: Float): Float {
        val extent = if (halfExtentMeters.isFinite() && halfExtentMeters > 0f) {
            halfExtentMeters
        } else {
            MIN_HALF_EXTENT_METERS
        }
        val pageMeters = extent / (0.5f - PAGE_MARGIN)
        val steps = floatArrayOf(1f, 2f, 5f, 10f, 20f, 50f, 100f, 200f, 500f, 1000f)
        var chosen = steps[0]
        for (step in steps) {
            if (pageMeters / step >= 4f) chosen = step else break
        }
        return chosen
    }

    /** Grid lines in image fractions for the same projection as [project]. */
    fun grid(halfExtentMeters: Float): PedestrianGrid {
        val extent = if (halfExtentMeters.isFinite() && halfExtentMeters > 0f) {
            halfExtentMeters
        } else {
            MIN_HALF_EXTENT_METERS
        }
        val spacing = gridSpacingMeters(extent)
        val scale = (0.5f - PAGE_MARGIN) / extent
        return PedestrianGrid(
            spacingMeters = spacing,
            vertical = meterFractions(scale, spacing, northUp = false),
            horizontal = meterFractions(scale, spacing, northUp = true)
        )
    }

    /**
     * Moves the newest marks that sit on [from] back to [to].
     * Earlier marks that happen to share that place are left alone.
     */
    fun retractLastPosition(
        samples: List<PedestrianSample>,
        from: PedestrianPosition,
        to: PedestrianPosition
    ): List<PedestrianSample> {
        if (samples.isEmpty()) return samples
        val updated = samples.toMutableList()
        var index = updated.lastIndex
        while (index >= 0 && samePlace(updated[index], from)) {
            val sample = updated[index]
            updated[index] = sample.copy(
                eastMeters = to.eastMeters,
                northMeters = to.northMeters
            )
            index--
        }
        return updated
    }

    private fun meterFractions(scale: Float, spacing: Float, northUp: Boolean): List<Float> {
        val minMeters = -0.5f / scale
        val maxMeters = 0.5f / scale
        val first = ceil((minMeters / spacing).toDouble()).toInt()
        val last = floor((maxMeters / spacing).toDouble()).toInt()
        val fractions = ArrayList<Float>(last - first + 1)
        for (index in first..last) {
            val meters = index * spacing
            val fraction = if (northUp) 0.5f - meters * scale else 0.5f + meters * scale
            if (fraction in 0f..1f) fractions += fraction
        }
        return fractions
    }

    private fun samePlace(sample: PedestrianSample, position: PedestrianPosition): Boolean {
        return abs(sample.eastMeters - position.eastMeters) <= 0.02f &&
            abs(sample.northMeters - position.northMeters) <= 0.02f
    }

    /** Latest position at the centre, so a straight walk sits below the centre. */
    fun aroundLatest(samples: List<PedestrianSample>): List<PedestrianSample> {
        val latest = samples.lastOrNull() ?: return samples
        return samples.map { sample ->
            sample.copy(
                eastMeters = sample.eastMeters - latest.eastMeters,
                northMeters = sample.northMeters - latest.northMeters
            )
        }
    }

    fun project(samples: List<PedestrianSample>, zoneStartMs: Long): List<WalkPin> {
        if (samples.isEmpty()) return emptyList()
        val shifted = aroundLatest(samples)
        val scale = (0.5f - PAGE_MARGIN) / halfExtent(shifted)
        return shifted.mapIndexed { index, sample ->
            WalkPin(
                placedAtMs = zoneStartMs + samples[index].elapsedMs,
                xFraction = (0.5f + sample.eastMeters * scale).coerceIn(0f, 1f),
                yFraction = (0.5f - sample.northMeters * scale).coerceIn(0f, 1f)
            )
        }
    }

    /**
     * A mark every [everyMeters] along the walked path, in the same image fractions as [project].
     */
    fun distanceMarks(samples: List<PedestrianSample>, everyMeters: Float = 5f): List<PedestrianDistanceMark> {
        if (samples.size < 2 || everyMeters <= 0f || !everyMeters.isFinite()) return emptyList()
        val places = ArrayList<Pair<Int, PedestrianPosition>>()
        var travelled = 0f
        var next = everyMeters
        for (index in 1 until samples.size) {
            val from = samples[index - 1]
            val to = samples[index]
            val east = to.eastMeters - from.eastMeters
            val north = to.northMeters - from.northMeters
            val length = hypot(east, north)
            if (!length.isFinite() || length <= 0f) continue
            while (travelled + length + 0.0001f >= next) {
                val fraction = ((next - travelled) / length).coerceIn(0f, 1f)
                places += next.roundToInt() to PedestrianPosition(
                    eastMeters = from.eastMeters + east * fraction,
                    northMeters = from.northMeters + north * fraction
                )
                next += everyMeters
                if (places.size > 10_000) break
            }
            travelled += length
        }
        if (places.isEmpty()) return emptyList()
        val latest = samples.last()
        val scale = (0.5f - PAGE_MARGIN) / halfExtent(aroundLatest(samples))
        return places.map { (meters, position) ->
            PedestrianDistanceMark(
                meters = meters,
                xFraction = (0.5f + (position.eastMeters - latest.eastMeters) * scale).coerceIn(0f, 1f),
                yFraction = (0.5f - (position.northMeters - latest.northMeters) * scale).coerceIn(0f, 1f)
            )
        }
    }
}

data class PedestrianPosition(
    val eastMeters: Float = 0f,
    val northMeters: Float = 0f
)

data class PedestrianSample(
    val elapsedMs: Long,
    val eastMeters: Float,
    val northMeters: Float
)

/** A distance label on the pedestrian page, in the same image fractions as the trail. */
data class PedestrianDistanceMark(
    val meters: Int,
    val xFraction: Float,
    val yFraction: Float
)

/** Live numbers drawn on the black page. Null fields are shown as unknown. */
data class PedestrianLive(
    val bearingDegrees: Int? = null,
    val steps: Int? = null,
    val distanceMeters: Float? = null,
    val canUndo: Boolean = false,
    /** Direction the path is holding, when straight lines are on. */
    val snappedBearingDegrees: Int? = null
)

/** The position a removed step left, and the position to draw instead. */
data class PedestrianRetraction(
    val from: PedestrianPosition,
    val to: PedestrianPosition
)

/** Metre grid in the same image fractions as the trail. */
data class PedestrianGrid(
    val spacingMeters: Float,
    val vertical: List<Float>,
    val horizontal: List<Float>
)

/**
 * One step from the up-and-down bounce of a phone held in the hand.
 * Gravity is already removed. A step is a rise through [HIGH] after a fall through [LOW].
 */
class VerticalStepDetector {
    private var armed = true
    private var lastStepMs = Long.MIN_VALUE

    fun reset() {
        armed = true
        lastStepMs = Long.MIN_VALUE
    }

    fun onVertical(vertical: Float, nowMs: Long): Boolean {
        val strength = abs(vertical)
        if (!strength.isFinite()) return false
        val gapOpen = lastStepMs == Long.MIN_VALUE || nowMs - lastStepMs >= MIN_GAP_MS
        if (armed && strength >= HIGH && gapOpen) {
            armed = false
            lastStepMs = nowMs
            return true
        }
        if (strength <= LOW) armed = true
        return false
    }

    companion object {
        const val HIGH = 1.2f
        const val LOW = 0.4f
        const val MIN_GAP_MS = 400L
    }
}

/**
 * Guide arrow for straight walks.
 * Before a zone test, the arrow follows the top of the phone.
 * When the test starts, [capture] holds that direction. Later turns move the arrow
 * in whole steps of 22°, 45°, or 90°.
 */
class HeadingSnap {
    private var arrow: Float? = null
    private var guiding = false

    fun reset() {
        arrow = null
        guiding = false
    }

    /** Live direction of the top of the phone, before a line has been captured. */
    fun followPhone(raw: Float): Float {
        val sample = PedestrianTrack.normalizeHeading(raw)
        if (!guiding) arrow = sample
        return if (guiding) arrow ?: sample else sample
    }

    /** The zone test starts. The arrow holds [raw], the way the phone is pointing. */
    fun capture(raw: Float): Float {
        val sample = PedestrianTrack.normalizeHeading(raw)
        arrow = sample
        guiding = true
        return sample
    }

    /**
     * While a line is held, move the arrow by whole [snapDegrees] steps once the phone
     * has turned that far from the arrow.
     */
    fun direction(raw: Float, snapDegrees: Float): Float {
        val sample = PedestrianTrack.normalizeHeading(raw)
        if (!guiding) return capture(sample)
        val step = PedestrianTrack.coerceHeadingSnapDegrees(snapDegrees)
        var held = arrow ?: sample
        var delta = PedestrianTrack.headingDelta(held, sample)
        while (delta >= step) {
            held = PedestrianTrack.normalizeHeading(held + step)
            delta -= step
        }
        while (delta <= -step) {
            held = PedestrianTrack.normalizeHeading(held - step)
            delta += step
        }
        arrow = held
        return held
    }
}
