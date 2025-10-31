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
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        poseResult?.let { result ->
            for (landmarkList in result.landmarks()) {
                if (landmarkList.size >= 25) { // Ensure all necessary landmarks are present
                    drawLandmarks(canvas, landmarkList)
                    drawConnections(canvas, landmarkList)
                }
            }
        }
    }

    private fun drawLandmarks(canvas: Canvas, landmarkList: List<NormalizedLandmark>) {
        // ✅ 어깨(11,12)와 귀(7,8) 랜드마크만 그립니다.
        val necessaryIndices = setOf(7, 8, 11, 12)
        for ((index, landmark) in landmarkList.withIndex()) {
            if (index in necessaryIndices) {
                val cx = landmark.x() * imageWidth * scaleFactor
                val cy = landmark.y() * imageHeight * scaleFactor
                canvas.drawPoint(cx, cy, pointPaint)
            }
        }
    }
    
    private fun getMidpoint(p1: NormalizedLandmark, p2: NormalizedLandmark): NormalizedLandmark {
        return NormalizedLandmark.create(
            (p1.x() + p2.x()) / 2,
            (p1.y() + p2.y()) / 2,
            (p1.z() + p2.z()) / 2
        )
    }

    private fun drawConnections(canvas: Canvas, landmarkList: List<NormalizedLandmark>) {
        // Midpoints 계산
        val earMidpoint = getMidpoint(landmarkList[7], landmarkList[8])
        val shoulderMidpoint = getMidpoint(landmarkList[11], landmarkList[12])

        // ✅ 진짜 '목' 선과 어깨선만 그리기
        drawLine(canvas, shoulderMidpoint, earMidpoint)
        drawLine(canvas, landmarkList, 11, 12) // 어깨
    }

    // 인덱스를 사용하는 오버로드
    private fun drawLine(canvas: Canvas, landmarkList: List<NormalizedLandmark>, startIdx: Int, endIdx: Int) {
        if (landmarkList.size > max(startIdx, endIdx)) {
            drawLine(canvas, landmarkList[startIdx], landmarkList[endIdx])
        }
    }

    // NormalizedLandmark 객체를 직접 사용하는 메인 함수
    private fun drawLine(canvas: Canvas, start: NormalizedLandmark, end: NormalizedLandmark) {
        val startX = start.x() * imageWidth * scaleFactor
        val startY = start.y() * imageHeight * scaleFactor
        val endX = end.x() * imageWidth * scaleFactor
        val endY = end.y() * imageHeight * scaleFactor
        canvas.drawLine(startX, startY, endX, endY, linePaint)
    }
}