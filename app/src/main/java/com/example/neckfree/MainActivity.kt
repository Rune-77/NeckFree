package com.example.neckfree

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var feedbackText: TextView
    private var poseLandmarker: PoseLandmarker? = null

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.viewFinder)
        feedbackText = findViewById(R.id.feedbackText)

        // 카메라 권한 체크
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }

        // Mediapipe PoseLandmarker 초기화 (안전하게)
        setupPoseLandmarkerSafely()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
        }

    // 안전하게 PoseLandmarker 초기화
    private fun setupPoseLandmarkerSafely() {
        try {
            // assets 폴더에 모델이 존재하는지 확인
            val modelName = "pose_landmarker_full.task"
            val files = assets.list("") ?: emptyArray()
            if (!files.contains(modelName)) {
                Toast.makeText(this, "모델 파일이 없습니다. Pose 기능이 비활성화됩니다.", Toast.LENGTH_LONG).show()
                return
            }

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelName)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result: PoseLandmarkerResult, _ ->
                    onPoseDetected(result)
                }
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(this, options)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "PoseLandmarker 초기화 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onPoseDetected(result: PoseLandmarkerResult) {
        val postureState = PoseAnalyzer.analyze(result)
        runOnUiThread {
            feedbackText.text = postureState
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        poseLandmarker?.close()
        executor.shutdown()
    }
}
