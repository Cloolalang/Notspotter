package io.github.cloolalang.notspotdetector.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import io.github.cloolalang.notspotdetector.data.PedestrianDebugLogger
import io.github.cloolalang.notspotdetector.model.HeadingSnap
import io.github.cloolalang.notspotdetector.model.PedestrianDebugSnapshot
import io.github.cloolalang.notspotdetector.model.PedestrianLive
import io.github.cloolalang.notspotdetector.model.PedestrianPosition
import io.github.cloolalang.notspotdetector.model.PedestrianRetraction
import io.github.cloolalang.notspotdetector.model.PedestrianSample
import io.github.cloolalang.notspotdetector.model.PedestrianTrack
import io.github.cloolalang.notspotdetector.model.VerticalStepDetector
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Compass plus a step count for a phone held flat in the hand, screen up, top edge
 * pointing the way you walk. North is azimuth 0.
 * With [tapSteps] off, a step is the up-and-down bounce, with gravity removed, and the
 * hardware step detector still counts a step when it fires. With [tapSteps] on, only a
 * tap on the page counts. Acceleration is not integrated.
 */
class PedestrianTracker(
    context: Context
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val handler = Handler(Looper.getMainLooper())
    private val rotation = FloatArray(9)
    private val orientation = FloatArray(3)
    private val peaks = VerticalStepDetector()
    private val headingSnapFilter = HeadingSnap()

    var strideMeters: Float = PedestrianTrack.DEFAULT_STRIDE_METERS
    /** On, page taps count steps and the motion sensors do not. */
    var tapSteps: Boolean = false
    /** On, the plotted line holds its direction through small compass wobble. */
    var headingSnap: Boolean = false
    var headingSnapDegrees: Float = PedestrianTrack.DEFAULT_HEADING_SNAP_DEGREES
    var onLive: ((PedestrianLive) -> Unit)? = null

    private var onSample: ((PedestrianSample) -> Unit)? = null
    private var position = PedestrianPosition()
    private var heading: Float? = null
    private var plotHeading: Float? = null
    /** Phone compass at the start of the zone test. Page forward is this direction. */
    private var startHeading: Float? = null
    private var originElapsed = 0L
    private var usingVertical = false
    private var usingRawAccelerometer = false
    private var hasUp = false
    private var upX = 0f
    private var upY = 0f
    private var upZ = 1f
    private var stepDetectorPresent = false
    private var lastDetectorStepMs = Long.MIN_VALUE
    private var tracking = false
    private var aiming = false
    private val stepStack = ArrayDeque<TakenStep>()
    private var stepsAccepted = 0
    private var stepsDropped = 0
    private var distanceMeters = 0f
    private var lastBearingDegrees: Int? = null
    private var lastPublished: PedestrianLive? = null
    private var maxVertical = 0f
    private var rotationLabel = "missing"
    private var stepSource = "none"
    private var verticalLabel = "off"
    private var lastLoggedHeading: Float? = null
    private var lastHeadingLogMs = 0L

    private val tick = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.elapsedRealtime() - originElapsed
            emit(elapsed)
            handler.postDelayed(this, PedestrianTrack.SAMPLE_STEP_MS)
        }
    }

    fun start(onSample: (PedestrianSample) -> Unit) {
        stop()
        begin(onSample, PedestrianPosition(), resuming = false)
    }

    /**
     * Continues from [position]. The first sample is elapsed 0 on this tracker's clock,
     * which the caller adds to the zone-test time so a pause is not squeezed away.
     */
    fun resume(position: PedestrianPosition, onSample: (PedestrianSample) -> Unit) {
        stop()
        begin(onSample, position, resuming = true)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        sensorManager.unregisterListener(this)
        if (tracking) {
            log(
                "stop",
                "steps=$stepsAccepted dropped=$stepsDropped east=${fmt(position.eastMeters)} " +
                    "north=${fmt(position.northMeters)} heading=${headingText()}"
            )
        }
        tracking = false
        aiming = false
        onSample = null
        usingVertical = false
        usingRawAccelerometer = false
        stepDetectorPresent = false
    }

    fun position(): PedestrianPosition = position

    /**
     * Compass only, before a zone test. With straight lines on, the arrow follows
     * the top of the phone so it can be aimed.
     */
    fun startAiming() {
        if (tracking || aiming) return
        handler.removeCallbacksAndMessages(null)
        sensorManager.unregisterListener(this)
        registerRotation()
        headingSnapFilter.reset()
        aiming = true
        publishLive()
    }

    /** One step along the current compass heading. Used when tap mode is on. */
    fun manualStep() {
        if (!tracking || !tapSteps) return
        takeStep("manual")
    }

    /** Locks the guide to the phone's current direction, or lets it follow again. */
    fun realignHeadingSnap() {
        val current = heading
        when {
            current == null || !headingSnap -> {
                headingSnapFilter.reset()
                plotHeading = current
            }
            tracking -> {
                plotHeading = headingSnapFilter.capture(current)
            }
            else -> {
                headingSnapFilter.reset()
                plotHeading = headingSnapFilter.followPhone(current)
            }
        }
        publishLive()
    }

    /** Removes the newest step and returns the positions the plot should follow. */
    fun undoLastStep(): PedestrianRetraction? {
        val taken = stepStack.removeLastOrNull() ?: return null
        val from = position
        position = taken.before
        stepsAccepted = (stepsAccepted - 1).coerceAtLeast(0)
        distanceMeters = (distanceMeters - taken.stride).coerceAtLeast(0f)
        publishLive()
        log(
            "undo",
            "east=${fmt(position.eastMeters)} north=${fmt(position.northMeters)} steps=$stepsAccepted"
        )
        return PedestrianRetraction(from = from, to = taken.before)
    }

    /** Drops the plotted walk and step count. The compass and the zone test keep running. */
    fun clearPlot() {
        stepStack.clear()
        position = PedestrianPosition()
        stepsAccepted = 0
        distanceMeters = 0f
        publishLive()
        log("clear", "plot cleared, heading=${headingText()}")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!tracking && !aiming) return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                upX = rotation[6]
                upY = rotation[7]
                upZ = rotation[8]
                hasUp = true
                val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val smoothed = PedestrianTrack.smoothHeading(heading, degrees)
                heading = smoothed
                if (tracking && startHeading == null) {
                    startHeading = if (headingSnap) {
                        headingSnapFilter.capture(smoothed)
                    } else {
                        smoothed
                    }
                    plotHeading = startHeading
                } else {
                    plotHeading = when {
                        headingSnap && tracking -> headingSnapFilter.direction(smoothed, headingSnapDegrees)
                        headingSnap -> headingSnapFilter.followPhone(smoothed)
                        else -> smoothed
                    }
                }
                noteHeading(smoothed)
                publishLive()
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (!tracking || tapSteps) return
                lastDetectorStepMs = SystemClock.elapsedRealtime()
                takeStep("step-detector")
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                if (!tracking || !usingVertical || usingRawAccelerometer || event.values.size < 3 || !hasUp) return
                considerVertical(
                    PedestrianTrack.verticalAcceleration(
                        event.values[0],
                        event.values[1],
                        event.values[2],
                        upX,
                        upY,
                        upZ
                    )
                )
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (!tracking || !usingRawAccelerometer || event.values.size < 3 || !hasUp) return
                val linear = PedestrianTrack.removeGravity(
                    event.values[0],
                    event.values[1],
                    event.values[2],
                    upX,
                    upY,
                    upZ
                )
                considerVertical(
                    PedestrianTrack.verticalAcceleration(linear[0], linear[1], linear[2], upX, upY, upZ)
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if ((!tracking && !aiming) || sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return
        log(
            "compass",
            "accuracy=$accuracy",
            suspect = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE,
            banner = false
        )
    }

    private fun begin(
        onSample: (PedestrianSample) -> Unit,
        position: PedestrianPosition,
        resuming: Boolean
    ) {
        val aim = heading
        this.onSample = onSample
        this.position = position
        if (!resuming) {
            headingSnapFilter.reset()
            startHeading = null
            if (aim != null) {
                startHeading = if (headingSnap) {
                    headingSnapFilter.capture(aim)
                } else {
                    PedestrianTrack.normalizeHeading(aim)
                }
                heading = startHeading
                plotHeading = startHeading
            } else {
                heading = null
                plotHeading = null
            }
        }
        peaks.reset()
        if (!resuming) {
            stepsAccepted = 0
            distanceMeters = 0f
            stepStack.clear()
            lastBearingDegrees = null
            lastPublished = null
        }
        stepsDropped = 0
        maxVertical = 0f
        hasUp = false
        lastDetectorStepMs = Long.MIN_VALUE
        lastLoggedHeading = null
        lastHeadingLogMs = 0L
        originElapsed = SystemClock.elapsedRealtime()
        aiming = false
        tracking = true
        registerSensors()
        log(
            if (resuming) "resume" else "start",
            "stride=${fmt(strideMeters)} mode=${if (tapSteps) "tap" else "accelerometer"} " +
                "snap=${if (headingSnap) fmt(headingSnapDegrees) + "°" else "off"} " +
                "permission=${permissionLabel()} rotation=$rotationLabel " +
                "step=$stepSource vertical=$verticalLabel threshold=${fmt(VerticalStepDetector.HIGH)} " +
                "east=${fmt(position.eastMeters)} north=${fmt(position.northMeters)}",
            suspect = rotationLabel == "missing" || (!tapSteps && stepSource == "none")
        )
        emit(0L)
        publishLive()
        handler.postDelayed(tick, PedestrianTrack.SAMPLE_STEP_MS)
    }

    private fun takeStep(source: String) {
        val azimuth = if (headingSnap) plotHeading ?: heading else heading
        val origin = startHeading
        if (azimuth == null || origin == null) {
            stepsDropped++
            log("step", "ignored source=$source reason=no heading", suspect = true)
            return
        }
        val relative = PedestrianTrack.relativeHeading(origin, azimuth)
        val stride = PedestrianTrack.coerceStride(strideMeters)
        val before = position
        position = PedestrianTrack.afterStep(position, relative, stride)
        stepStack.addLast(TakenStep(before, stride))
        stepsAccepted++
        distanceMeters += stride
        publishLive()
        log(
            "step",
            "accepted source=$source relative=${relative.roundToInt()}° " +
                "stride=${fmt(strideMeters)} " +
                "east=${fmt(position.eastMeters)} north=${fmt(position.northMeters)} steps=$stepsAccepted"
        )
    }

    private fun emit(elapsedMs: Long) {
        if (tracking) {
            val (note, suspect) = sampleNote(elapsedMs)
            log(
                "sample",
                "t=${elapsedMs / 1000}s east=${fmt(position.eastMeters)} north=${fmt(position.northMeters)} " +
                    "heading=${headingText()} steps=$stepsAccepted dropped=$stepsDropped " +
                    "source=$stepSource maxVertical=${fmt(maxVertical)} $note",
                suspect = suspect
            )
            maxVertical = 0f
        }
        onSample?.invoke(
            PedestrianSample(
                elapsedMs = elapsedMs,
                eastMeters = position.eastMeters,
                northMeters = position.northMeters
            )
        )
    }

    private fun sampleNote(elapsedMs: Long): Pair<String, Boolean> {
        if (rotationLabel == "missing" || heading == null) {
            return "no compass heading, steps are dropped" to true
        }
        if (tapSteps) {
            return "tap mode" to false
        }
        if (stepSource == "none") {
            return "no step sensor" to true
        }
        if (stepsAccepted == 0 && elapsedMs >= 3_000L) {
            if (!usingVertical) return "step detector has not fired" to true
            val peak = fmt(maxVertical)
            val high = fmt(VerticalStepDetector.HIGH)
            val why = if (maxVertical >= VerticalStepDetector.HIGH) {
                "no steps, vertical peak $peak reached $high but the step was not armed"
            } else {
                "no steps, vertical peak $peak below $high"
            }
            return why to true
        }
        return "ok" to false
    }

    private fun noteHeading(degrees: Float) {
        val previous = lastLoggedHeading
        val now = SystemClock.elapsedRealtime()
        if (previous != null) {
            val delta = headingDelta(previous, degrees)
            if (delta < 15f && now - lastHeadingLogMs < 5_000L) return
        }
        lastLoggedHeading = degrees
        lastHeadingLogMs = now
        log("heading", "azimuth=${degrees.roundToInt()}°", banner = false)
    }

    private fun headingDelta(from: Float, to: Float): Float {
        var delta = PedestrianTrack.normalizeHeading(to) - PedestrianTrack.normalizeHeading(from)
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return abs(delta)
    }

    private fun publishLive() {
        val live = PedestrianLive(
            steps = stepsAccepted,
            distanceMeters = distanceMeters,
            canUndo = stepStack.isNotEmpty(),
            snappedBearingDegrees = arrowDegrees()
        )
        if (live == lastPublished) return
        lastPublished = live
        onLive?.invoke(live)
    }

    /** Arrow angle on the page. 0 points forward, clockwise from the starting aim. */
    private fun arrowDegrees(): Int? {
        if (!tracking) return null
        val origin = startHeading ?: return 0
        val current = if (headingSnap) plotHeading ?: heading else heading
        if (current == null) return 0
        return PedestrianTrack.bearingDegrees(PedestrianTrack.relativeHeading(origin, current))
    }

    private fun headingText(): String {
        val azimuth = heading ?: return "none"
        return "${azimuth.roundToInt()}°"
    }

    private fun registerRotation() {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationLabel = rotationSensor?.name ?: "missing"
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME, handler)
        }
    }

    private fun registerSensors() {
        registerRotation()
        val stepSensor = if (stepDetectorAllowed()) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        } else {
            null
        }
        stepDetectorPresent = stepSensor != null
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
        }
        val linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (linear != null) {
            usingVertical = true
            usingRawAccelerometer = false
            verticalLabel = linear.name ?: "linear"
            sensorManager.registerListener(this, linear, SensorManager.SENSOR_DELAY_GAME, handler)
        } else if (accelerometer != null) {
            usingVertical = true
            usingRawAccelerometer = true
            verticalLabel = accelerometer.name ?: "accelerometer"
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, handler)
        } else {
            usingVertical = false
            usingRawAccelerometer = false
            verticalLabel = "missing"
        }
        stepSource = when {
            stepDetectorPresent && usingVertical -> "step-detector+vertical"
            stepDetectorPresent -> "step-detector"
            usingVertical -> "vertical"
            else -> "none"
        }
    }

    private fun considerVertical(vertical: Float) {
        val strength = abs(vertical)
        if (strength > maxVertical) maxVertical = strength
        if (tapSteps) return
        val now = SystemClock.elapsedRealtime()
        val peaked = peaks.onVertical(vertical, now)
        if (peaked && PedestrianTrack.accelerometerMayCountStep(stepDetectorPresent, lastDetectorStepMs, now)) {
            takeStep("vertical")
        }
    }

    private fun permissionLabel(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "not-required"
        return if (stepDetectorAllowed()) "granted" else "denied"
    }

    private fun stepDetectorAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun log(phase: String, detail: String, suspect: Boolean = false, banner: Boolean = true) {
        PedestrianDebugLogger.record(PedestrianDebugSnapshot(phase, detail, suspect, banner))
    }

    private fun fmt(value: Float): String = String.format(Locale.US, "%.2f", value)
}

private class TakenStep(
    val before: PedestrianPosition,
    val stride: Float
)
