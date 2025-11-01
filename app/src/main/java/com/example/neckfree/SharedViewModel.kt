package com.example.neckfree

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// ✅ [수정] 통계 데이터에 (시간, 각도) 쌍의 리스트를 포함하도록 변경
data class StatisticsData(
    val goodPostureCount: Int,
    val badPostureCount: Int,
    val totalMeasurementTimeMs: Long,
    val postureBreakCount: Int,
    val averageNeckAngle: Double,
    val badPostureTimeMs: Long,
    val neckAnglesOverTime: List<Pair<Long, Double>> // Pair<경과 시간(ms), 각도>
)

class SharedViewModel : ViewModel() {
    val startCalibrationEvent = MutableLiveData<Boolean>()
    val navigateToStatsEvent = MutableLiveData<Boolean>()
    val statisticsResult = MutableLiveData<StatisticsData>()

    fun setStatisticsResult(statsData: StatisticsData) {
        statisticsResult.value = statsData
    }
}
