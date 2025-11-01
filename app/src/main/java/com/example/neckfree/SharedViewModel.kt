package com.example.neckfree

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    // '자세 교정 시작' 이벤트
    val startCalibrationEvent = MutableLiveData<Boolean>()

    // ✅ '통계 탭으로 이동' 이벤트
    val navigateToStatsEvent = MutableLiveData<Boolean>()
    
    // ✅ 통계 결과를 저장할 데이터
    val statisticsResult = MutableLiveData<Pair<Int, Int>>() // Pair<goodCount, badCount>

    fun setStatisticsResult(goodCount: Int, badCount: Int) {
        statisticsResult.value = Pair(goodCount, badCount)
    }
}
