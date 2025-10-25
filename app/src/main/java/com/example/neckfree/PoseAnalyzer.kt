package com.example.neckfree

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.atan2
import kotlin.math.*

object PoseAnalyzer {

    fun analyze(result: PoseLandmarkerResult): String {
        if (result.landmarks().isEmpty()) return "인식 중..."

        // ✅ 명시적 타입 지정
        val poseLandmarks: List<NormalizedLandmark> = result.landmarks()[0]

        val ear = poseLandmarks[7]       // 오른쪽 귀 (RIGHT_EAR)
        val shoulder = poseLandmarks[11] // 오른쪽 어깨 (RIGHT_SHOULDER)
        val hip = poseLandmarks[23]      // 오른쪽 엉덩이 (RIGHT_HIP)

        val neckAngle = angleBetween(
            ear.x(), ear.y(),
            shoulder.x(), shoulder.y(),
            hip.x(), hip.y()
        )

        return when {
            neckAngle > 25 -> "거북목 자세입니다. 고개를 뒤로 젖히세요!"
            neckAngle < -10 -> "고개가 너무 뒤로 젖혀졌습니다!"
            else -> "좋은 자세를 유지하고 있습니다!"
        }
    }

    private fun angleBetween(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float
    ): Double {
        val angle1 = atan2(y1 - y2, x1 - x2)
        val angle2 = atan2(y3 - y2, x3 - x2)
        return Math.toDegrees(angle1 - angle2.toDouble())
    }
}
