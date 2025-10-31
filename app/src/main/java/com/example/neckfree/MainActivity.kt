package com.example.neckfree

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {

    private lateinit var previewView: PreviewView
    private lateinit var feedbackText: TextView
    private lateinit var poseOverlayView: PoseOverlayView
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private lateinit var calibrationButton: Button
    private lateinit var measureButton: Button // ✅ 측정 버튼 변수 추가

    private var camera: Camera? = null
    private val executor = Executors.newSingleThreadExecutor()

    // Calibration-related variables
    private var isCalibrating = false
    private val calibrationAngles = mutableListOf<Double>()

    // ✅ 측정 관련 변수 추가
    private var isMeasuring = false
    private val measuredStates = mutableListOf<PoseAnalyzer.PostureState>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.viewFinder)
        feedbackText = findViewById(R.id.feedbackText)
        poseOverlayView = findViewById(R.id.poseOverlay)
        calibrationButton = findViewById(R.id.calibrationButton)
        measureButton = findViewById(R.id.measureButton) // ✅ 측정 버튼 초기화

        calibrationButton.setOnClickListener {
            startCalibration()
        }

        // ✅ 측정 버튼 클릭 리스너 설정
        measureButton.setOnClickListener {
            toggleMeasurement()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            setupCamera()
        }
    }

    // ✅ 측정 시작/종료 토글 함수
    private fun toggleMeasurement() {
        isMeasuring = !isMeasuring

        if (isMeasuring) {
            // 측정 시작
            measuredStates.clear()
            measureButton.text = "측정 종료"
            calibrationButton.isEnabled = false // 측정 중에는 설정 변경 불가
            Toast.makeText(this, "자세 측정을 시작합니다.", Toast.LENGTH_SHORT).show()
        } else {
            // 측정 종료
            measureButton.text = "측정 시작"
            calibrationButton.isEnabled = true
            showStatistics()
        }
    }

    // ✅ 통계 결과 팝업으로 보여주는 함수
    private fun showStatistics() {
        if (measuredStates.isEmpty()) {
            Toast.makeText(this, "측정된 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val totalCount = measuredStates.size
        val goodCount = measuredStates.count { it == PoseAnalyzer.PostureState.GOOD }
        val badCount = totalCount - goodCount

        val goodPercentage = (goodCount.toDouble() / totalCount) * 100
        val badPercentage = (badCount.toDouble() / totalCount) * 100

        val message = "- 좋은 자세: ${String.format("%.1f", goodPercentage)}%\n" +
                      "- 나쁜 자세: ${String.format("%.1f", badPercentage)}%"

        AlertDialog.Builder(this)
            .setTitle("자세 측정 결과")
            .setMessage(message)
            .setPositiveButton("확인") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun startCalibration() {
        if (isCalibrating || isMeasuring) return

        isCalibrating = true
        calibrationAngles.clear()
        calibrationButton.isEnabled = false
        measureButton.isEnabled = false

        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                feedbackText.text = "바른 자세를 유지하세요... ${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                isCalibrating = false
                calibrationButton.isEnabled = true
                measureButton.isEnabled = true
                calculateAndSetThreshold()
            }
        }.start()
    }

    private fun calculateAndSetThreshold() {
        if (calibrationAngles.isEmpty()) {
            Toast.makeText(this, "자세 데이터 수집에 실패했습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val mean = calibrationAngles.average()
        val stdDev = sqrt(calibrationAngles.map { (it - mean) * (it - mean) }.average())
        val newThreshold = mean + (2 * stdDev)

        PoseAnalyzer.setCustomThreshold(newThreshold)

        val message = "자세 설정 완료!\n새로운 기준: ${String.format("%.1f", newThreshold)}도"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                setupCamera()
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private fun setupCamera() {
        poseLandmarkerHelper = PoseLandmarkerHelper(context = this, landmarkerListener = this)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
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
                camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("MainActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        val (postureState, neckAngle) = PoseAnalyzer.analyze(resultBundle.results)

        if (isCalibrating) {
            calibrationAngles.add(neckAngle)
        }

        // ✅ 측정 중일 때 자세 상태 기록
        if (isMeasuring) {
            measuredStates.add(postureState)
        }

        runOnUiThread {
            val feedbackMessage = when (postureState) {
                PoseAnalyzer.PostureState.GOOD -> "좋은 자세를 유지하고 있습니다!"
                PoseAnalyzer.PostureState.TURTLE_NECK -> "거북목 자세입니다. 고개를 뒤로 당기세요!"
                else -> "자세 분석 중..."
            }

            if (!isCalibrating) {
                feedbackText.text = "${feedbackMessage}\n각도: ${String.format("%.1f", neckAngle)}"
            }
            poseOverlayView.setResults(
                poseLandmarkerResult = resultBundle.results,
                imageHeight = resultBundle.inputImageHeight,
                imageWidth = resultBundle.inputImageWidth
            )
        }
    }

    override fun onError(error: String, errorCode: Int) {
        runOnUiThread { Toast.makeText(this, error, Toast.LENGTH_SHORT).show() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        poseLandmarkerHelper.clearPoseLandmarker()
        setupCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
        poseLandmarkerHelper.clearPoseLandmarker()
    }
}