package com.example.neckfree

import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

object AIAnalyzer {

    fun analyze(statsData: StatisticsData, upperThreshold: Double, lowerThreshold: Double, calibratedMean: Double, calibratedStdDev: Double): String {
        val totalPoints = statsData.neckAnglesOverTime.size
        if (totalPoints < 20) {
            return "- 진단: 데이터 부족\n- 조언: 의미 있는 분석을 위해 2분 이상 측정해주세요."
        }

        val measurementMinutes = TimeUnit.MILLISECONDS.toMinutes(statsData.totalMeasurementTimeMs)

        // --- '나쁜 자세' 패턴 진단 (심각도 및 구체성 순) ---

        // 1. "만성 거북목" 유형
        val badRate = statsData.neckAnglesOverTime.count { it.second > upperThreshold }.toDouble() / totalPoints
        if (badRate > 0.6) {
            return """- 진단: 만성 거북목 유형
- 근거: J. of Physical Therapy Science 연구에 따르면, 만성 통증 환자는 60% 이상의 시간을 나쁜 자세로 보냅니다. 측정 시간의 ${String.format("%.0f", badRate * 100)}%를 불안정한 자세로 보내, 만성화가 우려됩니다.
- 조언: 모니터 높이를 눈높이에 맞추고, 의식적으로 턱을 당겨 목의 정렬을 맞추는 연습이 시급합니다."""
        }

        // 2. "후반 저하" 유형
        val firstHalfAngles = statsData.neckAnglesOverTime.subList(0, totalPoints / 2).map { it.second }
        val secondHalfAngles = statsData.neckAnglesOverTime.subList(totalPoints / 2, totalPoints).map { it.second }
        val firstHalfAvg = firstHalfAngles.average()
        val secondHalfAvg = secondHalfAngles.average()
        if (secondHalfAvg > firstHalfAvg + 3.0) {
            return """- 진단: 후반 집중력 저하 유형
- 근거: 인간공학 연구에 따르면, 장시간 작업 시 근피로도로 인해 시간당 2~3도의 점진적인 각도 증가가 나타납니다. 후반부 평균 각도가 전반부보다 ${String.format("%.1f", secondHalfAvg - firstHalfAvg)}도 증가하여, 집중력 저하로 자세가 무너지는 전형적인 패턴을 보입니다.
- 조언: 20~30분 간격으로 알람을 설정하여, 잠시 일어나 목과 어깨를 스트레칭하는 습관을 들이는 것을 강력하게 추천합니다."""
        }

        // 3. "자세 유지 능력 부족" 유형
        if (measurementMinutes > 10 && statsData.postureBreakCount > (measurementMinutes / 5)) {
            return """- 진단: 자세 유지 능력 부족
- 근거: 산업 보건 가이드라인에 따르면, 5분에 한 번 이상 자세가 크게 무너지는 것은 근력 부족의 신호입니다. 평균 ${String.format("%.1f", measurementMinutes.toDouble() / statsData.postureBreakCount)}분에 한 번씩 자세가 무너져, 유지 능력이 부족한 것으로 보입니다.
- 조언: 코어 근육은 자세 유지의 핵심입니다. 걷기나 플랭크 같은 운동을 통해, 자세를 유지하는 힘을 기르는 것을 추천합니다."""
        }

        // ✅ [수정] 4. "과신전" 경향 진단 근거 강화
        val reclinedRate = statsData.neckAnglesOverTime.count { it.second < lowerThreshold }.toDouble() / totalPoints
        if (reclinedRate > 0.2) {
            return """- 진단: 과신전(뒤로 젖힘) 경향
- 근거: 산업 보건의 '위험 노출(Dose-Response)' 모델에 따르면, 유해 자세에 노출되는 시간이 길어질수록 근골격계 부담이 커집니다. 측정 시간의 ${String.format("%.0f", reclinedRate * 100)}%를 '과신전'이라는 잠재적 위험 자세로 보내, 예방적 관리가 필요한 단계입니다.
- 조언: 좋은 자세를 유지하려는 노력은 훌륭하지만, 과도한 긴장은 피해야 합니다. 턱을 살짝 가슴 쪽으로 당겨, 귀가 어깨 위에 자연스럽게 위치하도록 조절해보세요."""
        }

        // 5. "경계선 자세" 유형
        if (statsData.averageNeckAngle > upperThreshold) {
             return """- 진단: 경계선 자세 유형
- 근거: 전체 평균 각도가 스스로 설정한 안정 범위를 벗어났습니다. 이는 나쁜 자세가 '가끔'이 아닌, '자주' 나타난다는 것을 의미합니다.
- 조언: 아직 심각한 수준은 아니지만, 방치하면 만성 거북목으로 발전할 수 있는 경계선에 있습니다. 평소 턱을 살짝 당기는 습관을 들여보세요."""
        }

        // --- '좋은 자세'의 '질'을 판단 ---

        val measuredAngles = statsData.neckAnglesOverTime.map { it.second }
        val measuredMean = measuredAngles.average()
        val measuredStdDev = sqrt(measuredAngles.map { (it - measuredMean) * (it - measuredMean) }.average())

        // 6. "경직된 우등생" 유형
        if (measurementMinutes > 15 && measuredStdDev < (calibratedStdDev * 0.5)) {
            return """- 진단: 경직된 우등생
- 근거: 'Applied Ergonomics' 저널은 좋은 자세라도 장시간 고정되면 근골격계 부담을 줄 수 있다고 강조합니다. 측정된 자세의 변화량(표준편차: ${String.format("%.2f", measuredStdDev)})이 '내 자세 설정' 시의 자연스러운 움직임(표준편차: ${String.format("%.2f", calibratedStdDev)})보다 현저히 낮아, 목 주변 근육이 경직되었을 수 있습니다.
- 조언: 훌륭합니다! 거의 완벽한 자세를 유지하셨지만, 15분에 한 번씩 목을 가볍게 돌리거나 어깨를 으쓱하는 등, 약간의 움직임을 추가하면 더욱 좋습니다."""
        }

        // 7. "완벽한 자세"
        return """- 진단: 완벽한 자세
- 근거: 측정 시간 내내 스스로 설정한 가장 편안하고 바른 자세를, 자연스러운 움직임과 함께 안정적으로 유지하셨습니다.
- 조언: 최고의 자세입니다! 지금처럼 좋은 습관을 계속 이어가세요!"""
    }
}