package com.example.neckfree

import android.content.Context
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.atan2

// 데이터 클래스를 파일 최상단에 독립적으로 정의
data class PoseAnalysis(
    val postureState: PoseAnalyzer.PostureState,
    val displayAngle: Double,
    val smoothedEar: NormalizedLandmark?,
    val smoothedShoulder: NormalizedLandmark?,
    val smoothedHip: NormalizedLandmark?,
    val allLandmarks: List<NormalizedLandmark>
)

object PoseAnalyzer {

    private const val BAD_POSTURE_THRESHOLD_MS = 3000L // 3 seconds

    fun init(context: Context, userId: Long) {
        val (mean, stdDev) = SettingsManager.getCalibrationData(context, userId)
        setCalibrationData(mean.toDouble(), stdDev.toDouble())
    }

    enum class ViewingDirection {
        LEFT, RIGHT
    }

    private var viewingDirection: ViewingDirection = ViewingDirection.RIGHT

    fun setViewingDirection(direction: ViewingDirection) {
        viewingDirection = direction
    }

    private class LandmarkSmoother(private val alpha: Float) {
        private var lastLandmark: NormalizedLandmark? = null
        fun apply(landmark: NormalizedLandmark): NormalizedLandmark {
            val smoothedLandmark = if (lastLandmark != null) {
                val newX = alpha * landmark.x() + (1 - alpha) * lastLandmark!!.x()
                val newY = alpha * landmark.y() + (1 - alpha) * lastLandmark!!.y()
                val newZ = alpha * landmark.z() + (1 - alpha) * lastLandmark!!.z()
                NormalizedLandmark.create(newX, newY, newZ)
            } else {
                landmark
            }
            lastLandmark = smoothedLandmark
            return smoothedLandmark
        }
        fun reset() {
            lastLandmark = null
        }
    }

    enum class PostureState { GOOD, TURTLE_NECK, RECLINED_NECK, NOT_DETECTED, WARNING }

    private var calibratedMean = 0.0
    private var calibratedStdDev = 7.5
    private var upperThreshold = 22.5 // 7.5 * 3.0
    private var lowerThreshold = -22.5 // -7.5 * 3.0

    private val earSmoother = LandmarkSmoother(0.4f)
    private val shoulderSmoother = LandmarkSmoother(0.05f)
    private val hipSmoother = LandmarkSmoother(0.05f)

    // State tracking variables
    private var lastState: PostureState = PostureState.GOOD
    private var stateEnterTime: Long = 0L

    fun setCalibrationData(mean: Double, stdDev: Double) {
        calibratedMean = mean
        calibratedStdDev = stdDev
        // The acceptable range is now 3.0 standard deviations
        upperThreshold = mean + (3.0 * stdDev)
        lowerThreshold = mean - (3.0 * stdDev)
        resetState()
    }

    fun resetState() {
        earSmoother.reset()
        shoulderSmoother.reset()
        hipSmoother.reset()
        lastState = PostureState.GOOD
        stateEnterTime = System.currentTimeMillis()
    }

    fun getUpperThreshold(): Double = upperThreshold
    fun getLowerThreshold(): Double = lowerThreshold
    fun getCalibratedMean(): Double = calibratedMean
    fun getCalibratedStdDev(): Double = calibratedStdDev

    fun analyze(result: PoseLandmarkerResult): PoseAnalysis {
        if (result.landmarks().isEmpty() || result.landmarks()[0].size < 25) {
            return PoseAnalysis(PostureState.NOT_DETECTED, 0.0, null, null, null, emptyList())
        }

        val landmarks = result.landmarks()[0]

        val shoulderAnchor = getDynamicAnchor(landmarks[11], landmarks[12])
        val hipAnchor = getDynamicAnchor(landmarks[23], landmarks[24])
        val earAnchor = getMidpoint(landmarks[7], landmarks[8])

        if (shoulderAnchor == null || hipAnchor == null) {
            return PoseAnalysis(PostureState.NOT_DETECTED, 0.0, null, null, null, landmarks)
        }

        val smoothedEar = earSmoother.apply(earAnchor)
        val smoothedShoulder = shoulderSmoother.apply(shoulderAnchor)
        val smoothedHip = hipSmoother.apply(hipAnchor)

        val finalAngle = calculateNeckAngle(smoothedEar, smoothedShoulder, smoothedHip)

        // Determine the potential current state based on angle
        val potentialState = when {
            finalAngle > upperThreshold -> PostureState.TURTLE_NECK
            finalAngle < lowerThreshold -> PostureState.RECLINED_NECK
            else -> PostureState.GOOD
        }

        val currentTime = System.currentTimeMillis()

        // Check if state has changed
        if (potentialState != lastState) {
            lastState = potentialState
            stateEnterTime = currentTime
        }

        // Determine the final state based on time
        val finalState = if (lastState == PostureState.GOOD) {
            PostureState.GOOD
        } else {
            val timeInState = currentTime - stateEnterTime
            if (timeInState >= BAD_POSTURE_THRESHOLD_MS) {
                lastState // It's a confirmed bad posture
            } else {
                PostureState.WARNING // It's a temporary warning
            }
        }

        return PoseAnalysis(finalState, finalAngle, smoothedEar, smoothedShoulder, smoothedHip, landmarks)
    }

    private fun getDynamicAnchor(p1: NormalizedLandmark, p2: NormalizedLandmark): NormalizedLandmark? {
        val v1 = p1.visibility().orElse(0f)
        val v2 = p2.visibility().orElse(0f)

        return when {
            v1 > 0.7f && v2 > 0.7f -> getMidpoint(p1, p2)
            v1 > v2 -> p1
            v2 > v1 -> p2
            v1 > 0.2f -> p1
            v2 > 0.2f -> p2
            else -> null
        }
    }

    private fun getMidpoint(p1: NormalizedLandmark, p2: NormalizedLandmark): NormalizedLandmark {
        return NormalizedLandmark.create((p1.x() + p2.x()) / 2, (p1.y() + p2.y()) / 2, (p1.z() + p2.z()) / 2)
    }

    private fun calculateNeckAngle(ear: NormalizedLandmark, shoulder: NormalizedLandmark, hip: NormalizedLandmark): Double {
        val torsoVx = shoulder.x() - hip.x()
        val torsoVy = shoulder.y() - hip.y()
        val neckVx = ear.x() - shoulder.x()
        val neckVy = ear.y() - shoulder.y()

        if ((torsoVx * torsoVx + torsoVy * torsoVy) == 0f || (neckVx * neckVx + neckVy * neckVy) == 0f) return 0.0

        val torsoAngle = atan2(torsoVy.toDouble(), torsoVx.toDouble())
        val neckAngle = atan2(neckVy.toDouble(), neckVx.toDouble())

        var angleRad = neckAngle - torsoAngle

        while (angleRad > Math.PI) angleRad -= 2 * Math.PI
        while (angleRad < -Math.PI) angleRad += 2 * Math.PI

        if (viewingDirection == ViewingDirection.RIGHT) {
            angleRad = -angleRad
        }

        return Math.toDegrees(angleRad)
    }
}