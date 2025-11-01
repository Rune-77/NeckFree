package com.example.neckfree

import java.util.concurrent.TimeUnit

object AIAnalyzer {

    enum class PostureLevel {
        LEVEL_0, // 안정 자세
        LEVEL_1, // 주의 단계
        LEVEL_2  // 위험 단계
    }

    fun analyze(statsData: StatisticsData): String {
        val totalPoints = statsData.neckAnglesOverTime.size
        if (totalPoints < 10) {
            return "- 진단: 데이터 부족\n- 조언: 의미 있는 분석을 위해 최소 1분 이상 측정해주세요."
        }

        val mean = PoseAnalyzer.getCalibratedMean()
        val stdDev = PoseAnalyzer.getCalibratedStdDev()

        val level1Threshold = mean + (2 * stdDev)
        val level2Threshold = mean + (4 * stdDev)

        val levels = statsData.neckAnglesOverTime.map { (_, angle) ->
            when {
                angle > level2Threshold -> PostureLevel.LEVEL_2
                angle > level1Threshold -> PostureLevel.LEVEL_1
                else -> PostureLevel.LEVEL_0
            }
        }

        val measurementMinutes = TimeUnit.MILLISECONDS.toMinutes(statsData.totalMeasurementTimeMs)

        // 1. "습관성 거북목" 유형
        val level2Percentage = levels.count { it == PostureLevel.LEVEL_2 }.toDouble() / totalPoints
        if (level2Percentage > 0.5) { 
            return "- 진단: 습관성 거북목 유형\n- 조언: 측정 시간의 50% 이상을 스스로 설정한 바른 자세 범위를 훨씬 벗어난 '위험' 단계로 보냈습니다. 거북목 자세가 이미 만성화되었을 가능성이 높습니다. 모니터 높이를 조절하고, 의식적으로 턱을 당기는 연습이 시급합니다."
        }

        // 2. "후반 저하" 유형
        val firstHalfLevels = levels.subList(0, totalPoints / 2)
        val secondHalfLevels = levels.subList(totalPoints / 2, totalPoints)
        val firstHalfBadRate = firstHalfLevels.count { it != PostureLevel.LEVEL_0 }.toDouble() / firstHalfLevels.size
        val secondHalfBadRate = secondHalfLevels.count { it != PostureLevel.LEVEL_0 }.toDouble() / secondHalfLevels.size
        if (secondHalfBadRate > firstHalfBadRate * 1.5 && firstHalfBadRate < 0.3) {
            return "- 진단: 후반 저하 유형\n- 조언: 측정 초반에는 자세가 좋았지만, 시간이 지나면서 점차 무너지는 경향을 보입니다. 집중력 저하가 원인일 수 있습니다. 20분에 한 번씩 알람을 설정하여 자세를 바로잡는 것을 추천합니다."
        }

        // 3. "불안정한 자세" 유형
        if (statsData.postureBreakCount > measurementMinutes * 3 && measurementMinutes > 1) { // 분당 3회 이상으로 기준 강화
            return "- 진단: 불안정한 자세 유형\n- 조언: 바른 자세를 유지하는 능력이 부족하여 매우 자주 자세가 무너집니다. 코어 근육 강화를 위한 운동을 강력하게 추천합니다."
        }

        // 4. "경계선 자세" 유형 (신규)
        val badRate = levels.count { it != PostureLevel.LEVEL_0 }.toDouble() / totalPoints
        if (statsData.averageNeckAngle > level1Threshold) { // 평균 각도가 '주의' 단계에 해당할 경우
             return "- 진단: 경계선 자세 유형\n- 조언: 평균 목 각도가 스스로 설정한 바른 자세 범위를 벗어났습니다. 아직 심각한 수준은 아니지만, 방치하면 거북목으로 발전할 수 있는 경계선에 있습니다. 평소 턱을 살짝 당기는 습관을 들여보세요."
        }

        // 5. "자세 유지 능력 부족" 유형 (신규)
        if (statsData.postureBreakCount > measurementMinutes && measurementMinutes > 2) { // 2분 이상 측정 & 분당 1회 이상
            return "- 진단: 자세 유지 능력 부족\n- 조언: 자세가 유지되는 시간보다 흐트러지는 빈도가 더 높습니다. 바른 자세를 인지는 하고 있으나, 유지하는 능력이 부족한 상태입니다. 한 번 자세를 잡으면 최소 5분은 유지하는 것을 목표로 연습해보세요."
        }

        // 6. "경미한 거북목" 유형
        if (badRate > 0.2) { // 20% 이상을 '주의' 또는 '위험' 단계에서 보냈을 경우
             return "- 진단: 경미한 거북목 경향\n- 조언: 전반적으로 좋은 자세를 유지했지만, 측정 시간의 20% 이상을 나쁜 자세로 보냈습니다. 사소한 습관이 거북목으로 이어질 수 있습니다. 자세가 흐트러질 때마다 의식적으로 바로잡는 습관을 들여보세요."
        }

        // 7. 모든 규칙에 해당하지 않을 경우 (양호)
        return "- 진단: 양호한 자세\n- 조언: 훌륭합니다! 스스로 설정한 바른 자세 범위 내에서 안정적인 자세를 유지하셨습니다. 지금처럼 좋은 습관을 계속 이어가세요!"
    }
}