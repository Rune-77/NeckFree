package com.example.neckfree

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.max
import kotlin.math.min

class PoseOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var poseResult: PoseLandmarkerResult? = null
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var scaleFactor: Float = 1f

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

    fun setResults(
        poseLandmarkerResult: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int
    ) {
        poseResult = poseLandmarkerResult
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)

        invalidate() // View를 다시 그리도록 요청
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        poseResult?.let { result ->
            for (landmark in result.landmarks()) {
                drawLandmarks(canvas, landmark)
                drawConnections(canvas, landmark)
            }
        }
    }

    private fun drawLandmarks(canvas: Canvas, landmarkList: List<NormalizedLandmark>) {
        for (landmark in landmarkList) {
            val cx = landmark.x() * imageWidth * scaleFactor
            val cy = landmark.y() * imageHeight * scaleFactor
            canvas.drawPoint(cx, cy, pointPaint)
        }
    }

    private fun drawConnections(canvas: Canvas, landmarkList: List<NormalizedLandmark>) {
        // 어깨
        drawLine(canvas, landmarkList, 11, 12)
        // 몸통
        drawLine(canvas, landmarkList, 11, 23)
        drawLine(canvas, landmarkList, 12, 24)
        drawLine(canvas, landmarkList, 23, 24)
        // 팔
        drawLine(canvas, landmarkList, 11, 13)
        drawLine(canvas, landmarkList, 13, 15)
        drawLine(canvas, landmarkList, 12, 14)
        drawLine(canvas, landmarkList, 14, 16)
        // 얼굴 주변 (필요하다면)
        // drawLine(canvas, landmarkList, 7, 8) // 귀
    }

    private fun drawLine(canvas: Canvas, landmarkList: List<NormalizedLandmark>, startIdx: Int, endIdx: Int) {
        if (landmarkList.size > max(startIdx, endIdx)) {
            val start = landmarkList[startIdx]
            val end = landmarkList[endIdx]

            val startX = start.x() * imageWidth * scaleFactor
            val startY = start.y() * imageHeight * scaleFactor
            val endX = end.x() * imageWidth * scaleFactor
            val endY = end.y() * imageHeight * scaleFactor

            canvas.drawLine(startX, startY, endX, endY, linePaint)
        }
    }
}
