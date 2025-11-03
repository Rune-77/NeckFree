package com.example.neckfree

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsFragment : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        val sharedPref = activity?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref?.getLong("logged_in_user_id", -1L) ?: -1L

        if (userId == -1L) {
            Toast.makeText(context, "Please log in again.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.loginFragment)
            return view
        }

        val calibrateButton: Button = view.findViewById(R.id.calibrationButton)
        calibrateButton.setOnClickListener {
            // Switch to the Live tab and then start calibration
            (activity?.findViewById<BottomNavigationView>(R.id.bottom_nav_view))?.selectedItemId = R.id.navigation_live
            sharedViewModel.startCalibrationEvent.value = true
        }

        val radioGroup = view.findViewById<RadioGroup>(R.id.viewingDirectionRadioGroup)
        val currentDirection = SettingsManager.getViewingDirection(requireContext(), userId)
        if (currentDirection == PoseAnalyzer.ViewingDirection.LEFT) {
            radioGroup.check(R.id.leftDirectionRadioButton)
        } else {
            radioGroup.check(R.id.rightDirectionRadioButton)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newDirection = if (checkedId == R.id.leftDirectionRadioButton) {
                PoseAnalyzer.ViewingDirection.LEFT
            } else {
                PoseAnalyzer.ViewingDirection.RIGHT
            }
            SettingsManager.saveViewingDirection(requireContext(), newDirection, userId)
            PoseAnalyzer.setViewingDirection(newDirection)
        }

        val logoutButton: Button = view.findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            // Clear user session
            sharedPref?.edit()?.remove("logged_in_user_id")?.apply()

            // Navigate to login screen and clear back stack
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<BottomNavigationView>(R.id.bottom_nav_view)?.visibility = View.VISIBLE
    }
}
