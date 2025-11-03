package com.example.neckfree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class SettingsFragment : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var viewingDirectionRadioGroup: RadioGroup
    private lateinit var leftDirectionRadioButton: RadioButton
    private lateinit var rightDirectionRadioButton: RadioButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // '바른 자세 설정' 버튼 초기화 및 리스너 설정
        val calibrationButton: Button = view.findViewById(R.id.calibrationButton)
        calibrationButton.setOnClickListener {
            sharedViewModel.startCalibrationEvent.value = true
        }

        // '카메라 방향 설정' 라디오 그룹 초기화
        viewingDirectionRadioGroup = view.findViewById(R.id.viewingDirectionRadioGroup)
        leftDirectionRadioButton = view.findViewById(R.id.leftDirectionRadioButton)
        rightDirectionRadioButton = view.findViewById(R.id.rightDirectionRadioButton)

        // 저장된 설정 불러오기
        loadAndSetViewingDirection()

        // 라디오 그룹 리스너 설정
        viewingDirectionRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedDirection = when (checkedId) {
                R.id.leftDirectionRadioButton -> PoseAnalyzer.ViewingDirection.LEFT
                else -> PoseAnalyzer.ViewingDirection.RIGHT
            }
            SettingsManager.saveViewingDirection(requireContext(), selectedDirection)
            // 실시간으로 PoseAnalyzer에 적용
            PoseAnalyzer.setViewingDirection(selectedDirection)
        }

        return view
    }

    private fun loadAndSetViewingDirection() {
        val currentDirection = SettingsManager.getViewingDirection(requireContext())
        if (currentDirection == PoseAnalyzer.ViewingDirection.LEFT) {
            leftDirectionRadioButton.isChecked = true
        } else {
            rightDirectionRadioButton.isChecked = true
        }
        // 앱 시작 시 또는 Fragment가 다시 생성될 때 PoseAnalyzer에 현재 설정 적용
        PoseAnalyzer.setViewingDirection(currentDirection)
    }
}