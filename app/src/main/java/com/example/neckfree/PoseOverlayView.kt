package com.example.neckfree

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.max

// ✅ [수정] PoseAnalysis 클래스를 import
import com.example.neckfree.PoseAnalysis

class PoseOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var analysisResult: PoseAnalysis? = null
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var scaleFactor: Float = 1f

    private var isCalibrationGuideVisible = false

    private val pointPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        strokeWidth = 12f
    }

    private val linePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val guideLinePaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 5f
        pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
    }

    fun setResults(
        poseAnalysis: PoseAnalysis,
        imageHeight: Int,
        imageWidth: Int
    ) {
        analysisResult = poseAnalysis
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate()
    }

    fun setCalibrationGuideVisibility(isVisible: Boolean) {
        isCalibrationGuideVisible = isVisible
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        analysisResult?.let { result ->
            drawLandmarks(canvas, result.allLandmarks)

            val ear = result.smoothedEar
            val shoulder = result.smoothedShoulder
            if (ear != null && shoulder != null) {
                drawLine(canvas, shoulder, ear)
            }

            if (result.allLandmarks.size >= 13) {
                drawLine(canvas, result.allLandmarks[11], result.allLandmarks[12])
            }
            
            if (isCalibrationGuideVisible && shoulder != null) {
                val shoulderX = shoulder.x() * imageWidth * scaleFactor
                canvas.drawLine(shoulderX, 0f, shoulderX, height.toFloat(), guideLinePaint)
            }
        }
    }

    private fun drawLandmarks(canvas: Canvas, landmarkList: List<NormalizedLandmark>) {
        if (landmarkList.isEmpty()) return
        val necessaryIndices = setOf(7, 8, 11, 12)
        for ((index, landmark) in landmarkList.withIndex()) {
            if (index in necessaryIndices) {
                val cx = landmark.x() * imageWidth * scaleFactor
                val cy = landmark.y() * imageHeight * scaleFactor
                canvas.drawPoint(cx, cy, pointPaint)
            }
        }
    }

    private fun drawLine(canvas: Canvas, start: NormalizedLandmark, end: NormalizedLandmark) {
        val startX = start.x() * imageWidth * scaleFactor
        val startY = start.y() * imageHeight * scaleFactor
        val endX = end.x() * imageWidth * scaleFactor
        val endY = end.y() * imageHeight * scaleFactor
        canvas.drawLine(startX, startY, endX, endY, linePaint)
    }
}