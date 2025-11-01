package com.example.neckfree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class SettingsFragment : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        val calibrationButton: Button = view.findViewById(R.id.calibrationButton)
        calibrationButton.setOnClickListener {
            // HomeActivity에 이벤트를 전달
            sharedViewModel.startCalibrationEvent.value = true
        }

        return view
    }
}
