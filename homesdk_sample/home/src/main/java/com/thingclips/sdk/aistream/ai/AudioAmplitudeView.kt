package com.thingclips.sdk.aistream.ai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class AudioAmplitudeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var amplitudes: DoubleArray = DoubleArray(50) // 50 bars for the amplitude
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.color = Color.GRAY
        paint.strokeWidth = 6f
    }

    fun setAmplitudes(amps: DoubleArray?) {
        if (amps != null) {
            this.amplitudes = amps
            invalidate() // Redraw the view
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        val barCount = amplitudes.size
        if (barCount == 0) return

        val barWidth = w.toFloat() / barCount
        val centerY = h / 2f

        amplitudes.forEachIndexed { i, amp ->
            val x = i * barWidth + barWidth / 2
            val barHeight = (h / 2f) * max(amp.toFloat(), 0.05f) // Minimum height
            canvas.drawLine(x, centerY - barHeight, x, centerY + barHeight, paint)
        }
    }
}