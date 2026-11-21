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
import io.github.cloolalang.notspotdetector.model.PedestrianDebugSnapshot
import io.github.cloolalang.notspotdetector.model.PedestrianLive
import io.github.cloolalang.notspotdetector.model.PedestrianPosition
import io.github.cloolalang.notspotdetector.model.PedestrianRetraction
import io.github.cloolalang.notspotdetector.model.PedestrianSample
import io.github.cloolalang.notspotdetector.model.PedestrianTrack
import io.github.cloolalang.notspotdetector.model.StepPeakDetector
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Step detector plus rotation vector. North is azimuth 0, and the top of the phone
 * is the walking direction, so the phone is held flat with the screen up.
 * The accelerometer is read as well. Its peaks count a step when the step detector
 * is missing, permission is denied, or the detector stays quiet. Acceleration is not integrated.
 */
class PedestrianTracker(
    context: Context
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val handler = Handler(Looper.getMainLooper())
    private val rotation = FloatArray(9)
    private val orientation = FloatArray(3)
    private val peaks = StepPeakDetector()

    var strideMeters: Float = PedestrianTrack.DEFAULT_STRIDE_METERS
    var onLive: ((PedestrianLive) -> Unit)? = null

    private var onSample: ((PedestrianSample) -> Unit)? = null
    private var position = PedestrianPosition()
    private var heading: Float? = null
    private var originElapsed = 0L
    private var usingAccelerometer = false
    private var stepDetectorPresent = false
    private var lastDetectorStepMs = Long.MIN_VALUE
    private var tracking = false
    private val stepStack = ArrayDeque<TakenStep>()
    private var stepsAccepted = 0
    private var stepsDropped = 0
    private var distanceMeters = 0f
    private var lastBearingDegrees: Int? = null
    private var lastPublished: PedestrianLive? = null
    private var maxAccel = 0f
    private var rotationLabel = "missing"
    private var stepSource = "none"
    private var accelLabel = "off"
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
        onSample = null
        usingAccelerometer = false
        stepDetectorPresent = false
    }

    fun position(): PedestrianPosition = position

    /** One step along the current compass heading. Used by the on-screen tap button. */
    fun manualStep() {
        if (!tracking) return
        takeStep("manual")
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
        if (!tracking) return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val smoothed = PedestrianTrack.smoothHeading(heading, degrees)
                heading = smoothed
                noteHeading(smoothed)
                publishLive()
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                lastDetectorStepMs = SystemClock.elapsedRealtime()
                takeStep("step-detector")
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (!usingAccelerometer || event.values.size < 3) return
                val now = SystemClock.elapsedRealtime()
                val magnitude = hypot(event.values[0], hypot(event.values[1], event.values[2]))
                if (magnitude > maxAccel) maxAccel = magnitude
                val peaked = peaks.onMagnitude(magnitude, now)
                if (peaked && PedestrianTrack.accelerometerMayCountStep(
                        stepDetectorPresent,
                        lastDetectorStepMs,
                        now
                    )
                ) {
                    takeStep("accelerometer")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (!tracking || sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return
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
        this.onSample = onSample
        this.position = position
        heading = null
        peaks.reset()
        if (!resuming) {
            stepsAccepted = 0
            distanceMeters = 0f
            stepStack.clear()
            lastBearingDegrees = null
            lastPublished = null
        }
        stepsDropped = 0
        maxAccel = 0f
        lastDetectorStepMs = Long.MIN_VALUE
        lastLoggedHeading = null
        lastHeadingLogMs = 0L
        originElapsed = SystemClock.elapsedRealtime()
        tracking = true
        registerSensors()
        log(
            if (resuming) "resume" else "start",
            "stride=${fmt(strideMeters)} permission=${permissionLabel()} rotation=$rotationLabel " +
                "step=$stepSource accel=$accelLabel east=${fmt(position.eastMeters)} " +
                "north=${fmt(position.northMeters)}",
            suspect = rotationLabel == "missing" || stepSource == "none"
        )
        emit(0L)
        publishLive()
        handler.postDelayed(tick, PedestrianTrack.SAMPLE_STEP_MS)
    }

    private fun takeStep(source: String) {
        val azimuth = heading
        if (azimuth == null) {
            stepsDropped++
            log("step", "ignored source=$source reason=no heading", suspect = true)
            return
        }
        val stride = PedestrianTrack.coerceStride(strideMeters)
        val before = position
        position = PedestrianTrack.afterStep(position, azimuth, stride)
        stepStack.addLast(TakenStep(before, stride))
        stepsAccepted++
        distanceMeters += stride
        publishLive()
        log(
            "step",
            "accepted source=$source heading=${azimuth.roundToInt()}° stride=${fmt(strideMeters)} " +
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
                    "source=$stepSource maxAccel=${fmt(maxAccel)} $note",
                suspect = suspect
            )
            maxAccel = 0f
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
        if (stepSource == "none") {
            return "no step sensor" to true
        }
        if (stepsAccepted == 0 && elapsedMs >= 3_000L) {
            if (!usingAccelerometer) return "step detector has not fired" to true
            val peak = fmt(maxAccel)
            val high = fmt(StepPeakDetector.HIGH)
            val why = if (maxAccel >= StepPeakDetector.HIGH) {
                "no steps, peak $peak reached $high but the step was not armed"
            } else {
                "no steps, peak $peak below $high"
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
            bearingDegrees = PedestrianTrack.bearingDegrees(heading) ?: lastBearingDegrees,
            steps = stepsAccepted,
            distanceMeters = distanceMeters,
            canUndo = stepStack.isNotEmpty()
        )
        if (live.bearingDegrees != null) lastBearingDegrees = live.bearingDegrees
        if (live == lastPublished) return
        lastPublished = live
        onLive?.invoke(live)
    }

    private fun headingText(): String {
        val azimuth = heading ?: return "none"
        return "${azimuth.roundToInt()}°"
    }

    private fun registerSensors() {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationLabel = rotationSensor?.name ?: "missing"
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME, handler)
        }
        val stepSensor = if (stepDetectorAllowed()) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        } else {
            null
        }
        stepDetectorPresent = stepSensor != null
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
        }
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            usingAccelerometer = true
            accelLabel = accelerometer.name ?: "accelerometer"
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, handler)
        } else {
            accelLabel = "missing"
        }
        stepSource = when {
            stepDetectorPresent && usingAccelerometer -> "step-detector+accelerometer"
            stepDetectorPresent -> "step-detector"
            usingAccelerometer -> "accelerometer"
            else -> "none"
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
