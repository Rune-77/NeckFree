package com.example.neckfree

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.bottom_nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        navView.setupWithNavController(navController)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        // Apply the saved viewing direction setting at startup
        val savedDirection = SettingsManager.getViewingDirection(this)
        PoseAnalyzer.setViewingDirection(savedDirection)

        // Listen for navigation changes to decide when to trigger calibration
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_live) {
                // This logic is now primarily handled within LiveFragment itself,
                // but you could add logic here if needed, for example, to reset
                // calibration when navigating away and back to the live fragment.
            }
        }
    }
}