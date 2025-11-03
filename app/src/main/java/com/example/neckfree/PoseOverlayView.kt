package com.example.neckfree

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class PoseOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var poseAnalysis: PoseAnalysis? = null
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var showCalibrationGuide = false

    private val pointPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        strokeWidth = 12f
    }

    private val linePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 8f
    }

    private val guidePaint = Paint().apply {
        color = Color.RED
        strokeWidth = 5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    fun setResults(analysis: PoseAnalysis, imageHeight: Int, imageWidth: Int) {
        this.poseAnalysis = analysis
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        invalidate()
    }

    fun setCalibrationGuideVisibility(show: Boolean) {
        this.showCalibrationGuide = show
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showCalibrationGuide) {
            // Draw guide line based on shoulder position if available
            poseAnalysis?.smoothedShoulder?.let { shoulder ->
                val shoulderX = shoulder.x() * width
                canvas.drawLine(shoulderX, 0f, shoulderX, height.toFloat(), guidePaint)
            }
        }
    }

    private fun drawLandmark(canvas: Canvas, landmark: NormalizedLandmark) {
        val pointX = landmark.x() * width
        val pointY = landmark.y() * height
        canvas.drawCircle(pointX, pointY, 10f, pointPaint)
    }

    private fun drawLine(canvas: Canvas, start: NormalizedLandmark, end: NormalizedLandmark) {
        val startX = start.x() * width
        val startY = start.y() * height
        val endX = end.x() * width
        val endY = end.y() * height
        canvas.drawLine(startX, startY, endX, endY, linePaint)
    }
}
