package com.example.neckfree

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.sqrt

object PoseAnalyzer {

    // ✅ 자세 상태를 명확히 구분하기 위한 Enum 클래스 추가
    enum class PostureState {
        GOOD,
        TURTLE_NECK,
        NOT_DETECTED,
        CALIBRATING
    }

    private var customThreshold = 27.0

    fun setCustomThreshold(newThreshold: Double) {
        customThreshold = newThreshold
    }

    // ✅ 반환 타입을 Pair<PostureState, Double>로 변경
    fun analyze(result: PoseLandmarkerResult): Pair<PostureState, Double> {
        if (result.landmarks().isEmpty()) {
            return Pair(PostureState.NOT_DETECTED, 0.0)
        }

        val poseLandmarks: List<NormalizedLandmark> = result.landmarks()[0]

        if (poseLandmarks.size < 25) {
            return Pair(PostureState.NOT_DETECTED, 0.0)
        }

        val earMidpoint = getMidpoint(poseLandmarks[7], poseLandmarks[8])
        val shoulderMidpoint = getMidpoint(poseLandmarks[11], poseLandmarks[12])
        val hipMidpoint = getMidpoint(poseLandmarks[23], poseLandmarks[24])

        val neckAngle = calculateNeckAngle(earMidpoint, shoulderMidpoint, hipMidpoint)

        val postureState = when {
            neckAngle > customThreshold -> PostureState.TURTLE_NECK
            else -> PostureState.GOOD
        }

        return Pair(postureState, neckAngle)
    }

    private fun getMidpoint(p1: NormalizedLandmark, p2: NormalizedLandmark): NormalizedLandmark {
        return NormalizedLandmark.create(
            (p1.x() + p2.x()) / 2,
            (p1.y() + p2.y()) / 2,
            (p1.z() + p2.z()) / 2
        )
    }

    private fun calculateNeckAngle(ear: NormalizedLandmark, shoulder: NormalizedLandmark, hip: NormalizedLandmark): Double {
        val torsoVx = shoulder.x() - hip.x()
        val torsoVy = shoulder.y() - hip.y()
        val neckVx = ear.x() - shoulder.x()
        val neckVy = ear.y() - shoulder.y()

        val dotProduct = torsoVx * neckVx + torsoVy * neckVy
        val magnitudeTorso = sqrt(torsoVx * torsoVx + torsoVy * torsoVy)
        val magnitudeNeck = sqrt(neckVx * neckVx + neckVy * neckVy)

        if (magnitudeTorso == 0f || magnitudeNeck == 0f) return 0.0

        val angleRad = acos(dotProduct / (magnitudeTorso * magnitudeNeck))
        return Math.toDegrees(angleRad.toDouble())
    }
}