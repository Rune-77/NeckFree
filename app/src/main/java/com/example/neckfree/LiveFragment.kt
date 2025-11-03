package com.example.neckfree

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.neckfree.db.MeasurementRecord
import com.example.neckfree.viewmodel.LiveViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.Executors
import kotlin.math.sqrt

class LiveFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    private lateinit var previewView: PreviewView
    private lateinit var feedbackText: TextView
    private lateinit var poseOverlayView: PoseOverlayView
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private lateinit var measureButton: Button

    private val executor = Executors.newSingleThreadExecutor()

    @Volatile private var isCalibrating = false
    @Volatile private var isCollectingForCalibration = false
    private val calibrationAngles = mutableListOf<Double>()

    private var measurementStartTime: Long = 0
    private val measuredStates = mutableListOf<Pair<PoseAnalyzer.PostureState, Long>>()
    private val measuredAnglesWithTime = mutableListOf<Pair<Long, Double>>()

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var liveViewModel: LiveViewModel

    private var calibrationTimer: CountDownTimer? = null
    private var collectionTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_live, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.findViewById<BottomNavigationView>(R.id.bottom_nav_view)?.visibility = View.VISIBLE

        previewView = view.findViewById(R.id.viewFinder)
        feedbackText = view.findViewById(R.id.feedbackText)
        poseOverlayView = view.findViewById(R.id.poseOverlay)
        measureButton = view.findViewById(R.id.measureButton)

        measureButton.setOnClickListener { toggleMeasurement() }

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        liveViewModel = ViewModelProvider(this).get(LiveViewModel::class.java)

        val sharedPref = activity?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref?.getLong("logged_in_user_id", -1L) ?: -1L

        if (userId != -1L) {
            PoseAnalyzer.init(requireContext(), userId)
            val savedDirection = SettingsManager.getViewingDirection(requireContext(), userId)
            PoseAnalyzer.setViewingDirection(savedDirection)
        }

        updateFeedbackText()

        sharedViewModel.startCalibrationEvent.observe(viewLifecycleOwner) { shouldStart ->
            if (shouldStart == true) {
                startCalibration()
                sharedViewModel.startCalibrationEvent.value = false // Reset the event
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            setupCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        // Sync button text with measurement state when user returns to the screen
        if (sharedViewModel.isMeasuring.value == true) {
            measureButton.text = "측정 종료"
        } else {
            measureButton.text = "측정 시작"
        }
        PoseAnalyzer.resetState() // Reset pose analysis state when the screen is shown
    }

    override fun onPause() {
        super.onPause()
        // If measuring, stop and discard the measurement when the user navigates away
        if (sharedViewModel.isMeasuring.value == true) {
            sharedViewModel.isMeasuring.value = false
            measuredStates.clear()
            measuredAnglesWithTime.clear()
            Toast.makeText(requireContext(), "측정이 중단되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleMeasurement() {
        val isCurrentlyMeasuring = sharedViewModel.isMeasuring.value ?: false
        sharedViewModel.isMeasuring.value = !isCurrentlyMeasuring

        if (!isCurrentlyMeasuring) {
            measuredStates.clear()
            measuredAnglesWithTime.clear()
            measurementStartTime = SystemClock.elapsedRealtime()
            measureButton.text = "측정 종료"
            Toast.makeText(requireContext(), "자세 측정을 시작합니다.", Toast.LENGTH_SHORT).show()
        } else {
            measureButton.text = "측정 시작"
            processAndSaveStats()
        }
    }

    private fun processAndSaveStats() {
        if (measuredStates.isEmpty()) {
            Toast.makeText(requireContext(), "측정된 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = activity?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref?.getLong("logged_in_user_id", -1L) ?: -1L

        if (userId == -1L) {
            Toast.makeText(requireContext(), "Error: User not logged in.", Toast.LENGTH_LONG).show()
            return
        }

        val totalMeasurementTimeMs = SystemClock.elapsedRealtime() - measurementStartTime
        
        val goodCount = measuredStates.count { it.first == PoseAnalyzer.PostureState.GOOD }
        val badCount = measuredStates.count { it.first == PoseAnalyzer.PostureState.TURTLE_NECK || it.first == PoseAnalyzer.PostureState.RECLINED_NECK }
        
        val averageNeckAngle = if (measuredAnglesWithTime.isNotEmpty()) measuredAnglesWithTime.map { it.second }.average() else 0.0

        var postureBreakCount = 0
        var badPostureTimeMs: Long = 0
        for (i in 1 until measuredStates.size) {
            val prevState = measuredStates[i-1]
            val currState = measuredStates[i]

            if (prevState.first == PoseAnalyzer.PostureState.GOOD && (currState.first == PoseAnalyzer.PostureState.TURTLE_NECK || currState.first == PoseAnalyzer.PostureState.RECLINED_NECK)) {
                postureBreakCount++
            }

            if (currState.first == PoseAnalyzer.PostureState.TURTLE_NECK || currState.first == PoseAnalyzer.PostureState.RECLINED_NECK) {
                badPostureTimeMs += (currState.second - prevState.second)
            }
        }
        
        val statsData = StatisticsData(
            goodPostureCount = goodCount,
            badPostureCount = badCount,
            totalMeasurementTimeMs = totalMeasurementTimeMs,
            postureBreakCount = postureBreakCount,
            averageNeckAngle = averageNeckAngle,
            badPostureTimeMs = badPostureTimeMs,
            neckAnglesOverTime = ArrayList(measuredAnglesWithTime)
        )

        val upperThreshold = PoseAnalyzer.getUpperThreshold()
        val lowerThreshold = PoseAnalyzer.getLowerThreshold()
        val calibratedMean = PoseAnalyzer.getCalibratedMean()
        val calibratedStdDev = PoseAnalyzer.getCalibratedStdDev()

        val record = MeasurementRecord(
            userId = userId,
            goodPostureCount = statsData.goodPostureCount,
            badPostureCount = statsData.badPostureCount,
            totalMeasurementTimeMs = statsData.totalMeasurementTimeMs,
            postureBreakCount = statsData.postureBreakCount,
            averageNeckAngle = statsData.averageNeckAngle,
            badPostureTimeMs = statsData.badPostureTimeMs,
            neckAnglesOverTime = statsData.neckAnglesOverTime,
            aiDiagnosis = AIAnalyzer.analyze(statsData, upperThreshold, lowerThreshold, calibratedMean, calibratedStdDev),
            calibratedMean = calibratedMean,
            calibratedStdDev = calibratedStdDev,
            upperThreshold = upperThreshold,
            lowerThreshold = lowerThreshold
        )
        liveViewModel.insert(record)
        
        sharedViewModel.setStatisticsResult(userId.toString(), statsData)
        sharedViewModel.navigateToStatsEvent.value = true
    }

    private fun startCalibration() {
        if (sharedViewModel.isMeasuring.value == true || isCalibrating) return
        isCalibrating = true
        measureButton.isEnabled = false
        poseOverlayView.setCalibrationGuideVisibility(true)

        calibrationTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                feedbackText.text = "화면의 수직선에 귀가 오도록 자세를 맞춰주세요... ${millisUntilFinished / 1000 + 1}"
            }

            override fun onFinish() {
                poseOverlayView.setCalibrationGuideVisibility(false)
                calibrationAngles.clear()
                isCollectingForCalibration = true

                collectionTimer = object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        feedbackText.text = "좋습니다! 이제 그 자세를 5초간 유지해주세요... ${millisUntilFinished / 1000}"
                    }

                    override fun onFinish() {
                        isCollectingForCalibration = false
                        isCalibrating = false
                        measureButton.isEnabled = true
                        calculateAndSetThreshold()
                    }
                }.start()
            }
        }.start()
    }

    private fun calculateAndSetThreshold() {
        if (calibrationAngles.size < 20) {
            Toast.makeText(requireContext(), "자세 데이터 수집에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        val sortedAngles = calibrationAngles.sorted()
        val trimCount = (sortedAngles.size * 0.2).toInt()
        val trimmedAngles = sortedAngles.subList(trimCount, sortedAngles.size - trimCount)
        if (trimmedAngles.isEmpty()) {
            Toast.makeText(requireContext(), "유효한 자세 데이터를 수집하지 못했습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val mean = trimmedAngles.average()
        val stdDev = sqrt(trimmedAngles.map { (it - mean) * (it - mean) }.average())

        PoseAnalyzer.setCalibrationData(mean, stdDev)

        val sharedPref = activity?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref?.getLong("logged_in_user_id", -1L) ?: -1L

        if (userId != -1L) {
            SettingsManager.saveCalibrationData(requireContext(), mean.toFloat(), stdDev.toFloat(), userId)
        }

        updateFeedbackText()
        val upper = PoseAnalyzer.getUpperThreshold()
        val lower = PoseAnalyzer.getLowerThreshold()
        val message = "자세 설정 완료!\n안전 범위: ${String.format("%.1f", lower)}° ~ ${String.format("%.1f", upper)}°"
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun updateFeedbackText() {
        val calibratedMean = PoseAnalyzer.getCalibratedMean()
        if (calibratedMean == 0.0) {
            feedbackText.text = "자세를 설정해주세요."
        } else {
            val upper = PoseAnalyzer.getUpperThreshold()
            val lower = PoseAnalyzer.getLowerThreshold()
            feedbackText.text = "안전 범위: ${String.format("%.1f", lower)}° ~ ${String.format("%.1f", upper)}°"
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) setupCamera() else Toast.makeText(requireContext(), "Camera permission is required.", Toast.LENGTH_SHORT).show()
    }

    private fun setupCamera() {
        poseLandmarkerHelper = PoseLandmarkerHelper(context = requireContext(), landmarkerListener = this)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(executor) { ip -> poseLandmarkerHelper.detectLiveStream(ip) } }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("LiveFragment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        if (!isAdded) return

        val poseAnalysis = PoseAnalyzer.analyze(resultBundle.results)

        if (isCollectingForCalibration) {
            calibrationAngles.add(poseAnalysis.displayAngle)
        }

        if (sharedViewModel.isMeasuring.value == true) {
            val elapsedTime = SystemClock.elapsedRealtime() - measurementStartTime
            measuredStates.add(Pair(poseAnalysis.postureState, SystemClock.elapsedRealtime()))
            measuredAnglesWithTime.add(Pair(elapsedTime, poseAnalysis.displayAngle))
        }

        activity?.runOnUiThread {
            if (isCalibrating) {
                // Calibration countdown timers in startCalibration() handle feedback text.
            } else if (PoseAnalyzer.getCalibratedMean() == 0.0) {
                feedbackText.text = "자세를 설정해주세요."
            } else {
                val feedbackMessage = when (poseAnalysis.postureState) {
                    PoseAnalyzer.PostureState.GOOD -> "좋은 자세를 유지하고 있습니다!"
                    PoseAnalyzer.PostureState.WARNING -> "자세가 흐트러지려 합니다. 주의하세요!"
                    PoseAnalyzer.PostureState.TURTLE_NECK -> "거북목 자세입니다. 고개를 뒤로 당기세요!"
                    PoseAnalyzer.PostureState.RECLINED_NECK -> "목을 너무 뒤로 젖혔습니다. 자세를 바로 하세요!"
                    PoseAnalyzer.PostureState.NOT_DETECTED -> "자세 분석 중..."
                }
                val upper = PoseAnalyzer.getUpperThreshold()
                val lower = PoseAnalyzer.getLowerThreshold()
                val rangeText = "안전 범위: ${String.format("%.1f", lower)}° ~ ${String.format("%.1f", upper)}°"
                feedbackText.text = "${rangeText}\n${feedbackMessage}\n각도: ${String.format("%.1f", poseAnalysis.displayAngle)}"
            }
            poseOverlayView.setResults(poseAnalysis, resultBundle.inputImageHeight, resultBundle.inputImageWidth)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread { Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        calibrationTimer?.cancel()
        collectionTimer?.cancel()
        isCalibrating = false
        isCollectingForCalibration = false
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
