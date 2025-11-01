package com.example.neckfree

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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

    @Volatile private var isMeasuring = false
    private val measuredStates = mutableListOf<PoseAnalyzer.PostureState>()

    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_live, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.viewFinder)
        feedbackText = view.findViewById(R.id.feedbackText)
        poseOverlayView = view.findViewById(R.id.poseOverlay)
        measureButton = view.findViewById(R.id.measureButton)

        measureButton.setOnClickListener {
            toggleMeasurement()
        }

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.startCalibrationEvent.observe(viewLifecycleOwner) { shouldStart ->
            if (shouldStart == true) {
                startCalibration()
                sharedViewModel.startCalibrationEvent.value = false 
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

    private fun toggleMeasurement() {
        isMeasuring = !isMeasuring
        if (isMeasuring) {
            measuredStates.clear()
            measureButton.text = "측정 종료"
            Toast.makeText(requireContext(), "자세 측정을 시작합니다.", Toast.LENGTH_SHORT).show()
        } else {
            measureButton.text = "측정 시작"
            // ✅ [수정] 팝업 대신, ViewModel을 통해 통계 데이터와 화면 전환 이벤트를 전달
            processAndNavigateToStats()
        }
    }

    // ✅ [수정] 통계 데이터를 계산하고 ViewModel에 전달하는 함수
    private fun processAndNavigateToStats() {
        if (measuredStates.isEmpty()) {
            Toast.makeText(requireContext(), "측정된 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val totalCount = measuredStates.size
        val goodCount = measuredStates.count { it == PoseAnalyzer.PostureState.GOOD }
        val badCount = totalCount - goodCount

        // ViewModel에 결과 데이터 저장
        sharedViewModel.setStatisticsResult(goodCount, badCount)
        // ViewModel에 화면 전환 이벤트 발생
        sharedViewModel.navigateToStatsEvent.value = true
    }

    private fun startCalibration() {
        if (isCalibrating || isMeasuring) return
        isCalibrating = true
        measureButton.isEnabled = false

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                feedbackText.text = "자세를 준비하세요... ${millisUntilFinished / 1000 + 1}"
            }

            override fun onFinish() {
                calibrationAngles.clear()
                isCollectingForCalibration = true
                object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        feedbackText.text = "바른 자세를 유지하세요... ${millisUntilFinished / 1000}"
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
        if (calibrationAngles.size < 10) {
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
        val newThreshold = mean + (2 * stdDev)
        PoseAnalyzer.setCustomThreshold(newThreshold)
        val message = "자세 설정 완료!\n새로운 기준: ${String.format("%.1f", newThreshold)}도"
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                setupCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required.", Toast.LENGTH_SHORT).show()
            }
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
        val (postureState, neckAngle) = PoseAnalyzer.analyze(resultBundle.results)

        if (isCollectingForCalibration) {
            calibrationAngles.add(neckAngle)
        }
        if (isMeasuring) {
            measuredStates.add(postureState)
        }

        activity?.runOnUiThread {
            val feedbackMessage = when (postureState) {
                PoseAnalyzer.PostureState.GOOD -> "좋은 자세를 유지하고 있습니다!"
                PoseAnalyzer.PostureState.TURTLE_NECK -> "거북목 자세입니다. 고개를 뒤로 당기세요!"
                else -> "자세 분석 중..."
            }
            if (!isCalibrating) {
                feedbackText.text = "${feedbackMessage}\n각도: ${String.format("%.1f", neckAngle)}"
            } 
            poseOverlayView.setResults(
                resultBundle.results,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth
            )
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread { Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}