package com.example.neckfree

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNav = findViewById(R.id.bottom_nav_view)
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        if (savedInstanceState == null) {
            loadFragment(LiveFragment())
        }

        // '자세 설정 시작' 이벤트 감지
        sharedViewModel.startCalibrationEvent.observe(this) {
            if (it == true) {
                bottomNav.selectedItemId = R.id.navigation_live
            }
        }

        // ✅ [수정] '통계 탭으로 이동' 이벤트 감지
        sharedViewModel.navigateToStatsEvent.observe(this) {
            if (it == true) {
                bottomNav.selectedItemId = R.id.navigation_stats
                sharedViewModel.navigateToStatsEvent.value = false // 이벤트 처리 완료
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            var fragment: Fragment? = null
            when (item.itemId) {
                R.id.navigation_live -> fragment = LiveFragment()
                R.id.navigation_stats -> fragment = StatsFragment()
                R.id.navigation_settings -> fragment = SettingsFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment?) {
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit()
        }
    }
}