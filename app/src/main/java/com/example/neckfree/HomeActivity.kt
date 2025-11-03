package com.example.neckfree

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        // ✅ [수정] Navigation Component를 사용하여 화면 전환을 관리
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNav.setupWithNavController(navController)

        // '자세 설정 시작' 이벤트 감지 (기존과 동일)
        sharedViewModel.startCalibrationEvent.observe(this) {
            if (it == true) {
                bottomNav.selectedItemId = R.id.navigation_live
            }
        }

        // '통계 탭으로 이동' 이벤트 감지 (기존과 동일)
        sharedViewModel.navigateToStatsEvent.observe(this) {
            if (it == true) {
                bottomNav.selectedItemId = R.id.navigation_stats
                sharedViewModel.navigateToStatsEvent.value = false 
            }
        }
    }
}