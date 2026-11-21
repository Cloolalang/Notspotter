package io.github.cloolalang.notspotdetector.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import io.github.cloolalang.notspotdetector.model.ColoredRouteSpan
import io.github.cloolalang.notspotdetector.model.PedestrianTrack
import io.github.cloolalang.notspotdetector.model.WalkLegendSwatch
import io.github.cloolalang.notspotdetector.model.WalkPin
import io.github.cloolalang.notspotdetector.model.WalkRoute

/** Draws the pedestrian trail on a black square so it can be saved as a JPEG. */
object PedestrianPageImage {

    fun render(
        pins: List<WalkPin>,
        spans: List<ColoredRouteSpan>,
        zoneStartMs: Long,
        zoneEndMs: Long,
        halfExtentMeters: Float,
        caption: String,
        legendTitle: String,
        legend: List<WalkLegendSwatch>
    ): Bitmap? {
        if (pins.isEmpty()) return null
        val size = PedestrianTrack.PAGE_PIXELS
        val stroke = (size * 0.012f).coerceIn(6f, 28f)
        val textSize = (stroke * 2.4f).coerceIn(18f, 42f)
        val strip = legendLayout(textSize)
        val output = Bitmap.createBitmap(size, size + strip.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)
        drawGrid(canvas, size, halfExtentMeters, textSize)
        drawTrail(canvas, size, pins, spans, zoneStartMs, zoneEndMs, stroke, textSize)
        drawLegend(canvas, size, size, strip, caption, legendTitle, legend)
        return output
    }

    private fun drawGrid(canvas: Canvas, size: Int, halfExtentMeters: Float, textSize: Float) {
        val grid = PedestrianTrack.grid(halfExtentMeters)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        fun drawLine(startX: Float, startY: Float, endX: Float, endY: Float, axis: Boolean) {
            line.color = if (axis) Color.argb(120, 255, 255, 255) else Color.argb(50, 255, 255, 255)
            line.strokeWidth = if (axis) 3f else 1.5f
            canvas.drawLine(startX, startY, endX, endY, line)
        }
        for (x in grid.vertical) {
            val px = x * size
            drawLine(px, 0f, px, size.toFloat(), axis = kotlin.math.abs(x - 0.5f) < 0.004f)
        }
        for (y in grid.horizontal) {
            val py = y * size
            drawLine(0f, py, size.toFloat(), py, axis = kotlin.math.abs(y - 0.5f) < 0.004f)
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize * 0.8f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(
            "${grid.spacingMeters.toInt()} m",
            24f,
            size - 24f,
            label
        )
    }

    private fun drawTrail(
        canvas: Canvas,
        size: Int,
        pins: List<WalkPin>,
        spans: List<ColoredRouteSpan>,
        zoneStartMs: Long,
        zoneEndMs: Long,
        stroke: Float,
        textSize: Float
    ) {
        fun x(fraction: Float) = fraction * size
        fun y(fraction: Float) = fraction * size
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        for (span in spans) {
            line.color = Color.WHITE
            line.strokeWidth = stroke + 6f
            canvas.drawLine(x(span.start.x), y(span.start.y), x(span.end.x), y(span.end.y), line)
            line.color = span.colorArgb.toInt()
            line.strokeWidth = stroke
            canvas.drawLine(x(span.start.x), y(span.start.y), x(span.end.x), y(span.end.y), line)
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val ordered = pins.sortedBy { it.placedAtMs }
        val radius = stroke * 0.55f
        for (pin in ordered) {
            fill.color = Color.WHITE
            canvas.drawCircle(x(pin.xFraction), y(pin.yFraction), radius + 2f, fill)
            fill.color = Color.BLACK
            canvas.drawCircle(x(pin.xFraction), y(pin.yFraction), radius, fill)
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize * 0.85f
            textAlign = Paint.Align.CENTER
        }
        for (tick in WalkRoute.ticksOnRoute(ordered, zoneStartMs, zoneEndMs)) {
            val at = tick.point
            canvas.drawText(
                WalkRoute.elapsedLabel(tick.atMs - zoneStartMs),
                x(at.x),
                (y(at.y) - radius - 6f).coerceAtLeast(textSize),
                label
            )
        }
        val north = Paint(label).apply {
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Forward", size / 2f, textSize * 1.4f, north)
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
            textSize = textSize,
            padding = padding,
            captionBaseline = captionBaseline,
            titleBaseline = titleBaseline,
            swatchTop = swatchTop,
            swatchBottom = swatchBottom,
            swatch = swatch
        )
    }

    private data class LegendStrip(
        val height: Int,
        val textSize: Float,
        val padding: Float,
        val captionBaseline: Float,
        val titleBaseline: Float,
        val swatchTop: Float,
        val swatchBottom: Float,
        val swatch: Float
    )
}
