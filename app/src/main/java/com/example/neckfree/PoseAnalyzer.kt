package com.example.neckfree

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.atan2

object PoseAnalyzer {

    fun analyze(result: PoseLandmarkerResult): String {
        // 1. 사람이 인식되지 않으면 "인식 중..." 반환
        if (result.landmarks().isEmpty()) {
            return "인식 중..."
        }

        val poseLandmarks: List<NormalizedLandmark> = result.landmarks()[0]

        // 2. ✅ 안전장치: 상체 측정을 위한 어깨(11, 12) 좌표가 있는지 확인
        if (poseLandmarks.size < 13) {
            return "자세 측정을 위해 어깨를 보여주세요."
        }

        val rightEar = poseLandmarks[7]      // 오른쪽 귀
        val rightShoulder = poseLandmarks[11] // 오른쪽 어깨
        val leftShoulder = poseLandmarks[12]  // 왼쪽 어깨

        // 3. 어깨선을 기준으로 목의 기울기 각도 계산
        val neckAngle = calculateNeckAngle(rightEar, rightShoulder, leftShoulder)

        // 4. ✅ 새로운 기준에 맞춘 각도 판별
        return when {
            neckAngle > 20 -> "거북목 자세입니다. 고개를 뒤로 당기세요!"
            else -> "좋은 자세를 유지하고 있습니다!"
        }
    }

    /**
     * 양쪽 어깨선을 기준으로 몸의 수직 벡터를 계산하고,
     * 어깨-귀를 잇는 목 벡터와의 사이 각도를 계산합니다.
     */
    private fun calculateNeckAngle(
        ear: NormalizedLandmark,
        rShoulder: NormalizedLandmark,
        lShoulder: NormalizedLandmark
    ): Double {
        // 어깨선 벡터 (오른쪽 -> 왼쪽)
        val shoulderVx = lShoulder.x() - rShoulder.x()
        val shoulderVy = lShoulder.y() - rShoulder.y()

        // 몸의 수직 기준 벡터 (어깨선에 수직이고 위를 향함)
        val torsoVerticalVx = -shoulderVy
        val torsoVerticalVy = shoulderVx

        // 목 벡터 (어깨 -> 귀)
        val neckVx = ear.x() - rShoulder.x()
        val neckVy = ear.y() - rShoulder.y()

        // 두 벡터 사이의 각도 계산
        val angleRad = atan2(neckVy, neckVx) - atan2(torsoVerticalVy, torsoVerticalVx)
        var angleDeg = Math.toDegrees(angleRad.toDouble())

        // 각도를 -180 ~ 180 범위로 정규화
        if (angleDeg > 180) angleDeg -= 360
        if (angleDeg < -180) angleDeg += 360

        return angleDeg
    }
}