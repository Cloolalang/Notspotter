package io.github.cloolalang.notspotdetector.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import io.github.cloolalang.notspotdetector.model.ImagePoint
import io.github.cloolalang.notspotdetector.model.RsrpBinSegment
import io.github.cloolalang.notspotdetector.model.WalkLegendSwatch
import io.github.cloolalang.notspotdetector.model.WalkPin
import io.github.cloolalang.notspotdetector.model.WalkRoute
import kotlin.math.min

/** Draws the coloured walk onto a copy of the floor plan so it can be saved as a JPEG. */
object WalkRouteImage {

    fun render(
        floorPlan: Bitmap,
        pins: List<WalkPin>,
        segments: List<RsrpBinSegment>,
        zoneStartMs: Long,
        zoneEndMs: Long,
        caption: String,
        legendTitle: String,
        legend: List<WalkLegendSwatch>
    ): Bitmap? {
        val width = floorPlan.width
        val height = floorPlan.height
        if (width <= 0 || height <= 0) return null
        val stroke = (min(width, height) * 0.012f).coerceIn(6f, 28f)
        val textSize = (stroke * 2.4f).coerceIn(18f, 42f)
        val strip = legendLayout(textSize)
        val output = Bitmap.createBitmap(width, height + strip.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(floorPlan, 0f, 0f, null)
        drawRoute(canvas, width, height, pins, segments, zoneStartMs, zoneEndMs, stroke, textSize)
        drawLegend(canvas, width, height, strip, caption, legendTitle, legend)
        return output
    }

    private fun drawRoute(
        canvas: Canvas,
        width: Int,
        height: Int,
        pins: List<WalkPin>,
        segments: List<RsrpBinSegment>,
        zoneStartMs: Long,
        zoneEndMs: Long,
        stroke: Float,
        textSize: Float
    ) {
        val plan = RectF(0f, 0f, width.toFloat(), height.toFloat())
        fun point(fraction: ImagePoint): ImagePoint {
            return ImagePoint(
                x = plan.left + fraction.x * plan.width(),
                y = plan.top + fraction.y * plan.height()
            )
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        for (span in WalkRoute.spans(pins, segments)) {
            val start = point(span.start)
            val end = point(span.end)
            line.color = Color.WHITE
            line.strokeWidth = stroke + 6f
            canvas.drawLine(start.x, start.y, end.x, end.y, line)
            line.color = WalkRoute.rsrpRouteColor(span.bin).toInt()
            line.strokeWidth = stroke
            canvas.drawLine(start.x, start.y, end.x, end.y, line)
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val orderedPins = pins.sortedBy { it.placedAtMs }
        val pinPoints = orderedPins.map { point(ImagePoint(it.xFraction, it.yFraction)) }
        val pinRadius = stroke * 1.5f
        for (tick in WalkRoute.ticksOnRoute(pins, zoneStartMs, zoneEndMs)) {
            val at = point(tick.point)
            if (pinPoints.any { distance(it, at) < pinRadius * 1.6f }) continue
            fill.color = Color.WHITE
            canvas.drawCircle(at.x, at.y, stroke * 0.85f, fill)
            fill.color = Color.BLACK
            canvas.drawCircle(at.x, at.y, stroke * 0.45f, fill)
            drawLabel(
                canvas = canvas,
                text = WalkRoute.elapsedLabel(tick.atMs - zoneStartMs),
                x = at.x,
                y = (at.y - stroke * 1.6f).coerceAtLeast(textSize),
                textSize = textSize * 0.85f
            )
        }
        orderedPins.forEachIndexed { index, pin ->
            val at = pinPoints[index]
            fill.color = Color.WHITE
            canvas.drawCircle(at.x, at.y, pinRadius + 3f, fill)
            fill.color = WalkRoute.rsrpRouteColor(WalkRoute.binAt(segments, pin.placedAtMs)).toInt()
            canvas.drawCircle(at.x, at.y, pinRadius, fill)
            drawLabel(
                canvas = canvas,
                text = WalkRoute.elapsedLabel(pin.placedAtMs - zoneStartMs),
                x = at.x,
                y = (at.y - pinRadius - textSize * 0.35f).coerceAtLeast(textSize),
                textSize = textSize
            )
        }
    }

    private fun drawLegend(
        canvas: Canvas,
        width: Int,
        planHeight: Int,
        strip: LegendStrip,
        caption: String,
        legendTitle: String,
        legend: List<WalkLegendSwatch>
    ) {
        val top = planHeight.toFloat()
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRect(0f, top, width.toFloat(), top + strip.height, background)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = strip.textSize
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(caption, strip.padding, top + strip.captionBaseline, text)
        val title = Paint(text).apply { typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(legendTitle, strip.padding, top + strip.titleBaseline, title)
        var x = strip.padding
        val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (entry in legend) {
            val labelWidth = text.measureText(entry.label)
            if (x + strip.swatch + labelWidth + strip.padding > width) break
            swatchPaint.color = entry.color.toInt()
            canvas.drawRect(x, top + strip.swatchTop, x + strip.swatch, top + strip.swatchBottom, swatchPaint)
            canvas.drawText(entry.label, x + strip.swatch + strip.padding * 0.4f, top + strip.swatchBottom, text)
            x += strip.swatch + labelWidth + strip.padding * 2f
        }
    }

    private fun legendLayout(routeTextSize: Float): LegendStrip {
        val textSize = routeTextSize * 0.85f
        val padding = routeTextSize * 0.4f
        val metrics = Paint().apply { this.textSize = textSize }.fontMetrics
        val line = metrics.descent - metrics.ascent
        val swatch = routeTextSize * 0.7f
        val captionBaseline = padding - metrics.ascent
        val titleBaseline = captionBaseline + line + padding * 0.2f
        val swatchTop = titleBaseline + metrics.descent + padding * 0.45f
        val swatchBottom = swatchTop + swatch
        val height = (swatchBottom + padding).toInt().coerceAtLeast(1)
        return LegendStrip(
            height = height,
            captionBaseline = captionBaseline,
            titleBaseline = titleBaseline,
            swatchTop = swatchTop,
            swatchBottom = swatchBottom,
            swatch = swatch,
            textSize = textSize,
            padding = padding
        )
    }

    private data class LegendStrip(
        val height: Int,
        val captionBaseline: Float,
        val titleBaseline: Float,
        val swatchTop: Float,
        val swatchBottom: Float,
        val swatch: Float,
        val textSize: Float,
        val padding: Float
    )

    private fun drawLabel(canvas: Canvas, text: String, x: Float, y: Float, textSize: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            textAlign = Paint.Align.CENTER
        }
        val stroke = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = textSize / 5f
            color = Color.WHITE
        }
        canvas.drawText(text, x, y, stroke)
        canvas.drawText(text, x, y, fill)
    }

    private fun distance(a: ImagePoint, b: ImagePoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
