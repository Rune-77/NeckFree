package com.example.neckfree

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class StatsFragment : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var lineChart: LineChart
    private lateinit var aiDiagnosisText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var badPostureTimeText: TextView
    private lateinit var breakCountText: TextView
    private lateinit var avgAngleText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        lineChart = view.findViewById(R.id.lineChart)
        aiDiagnosisText = view.findViewById(R.id.aiDiagnosisText)
        totalTimeText = view.findViewById(R.id.totalTimeText)
        badPostureTimeText = view.findViewById(R.id.badPostureTimeText)
        breakCountText = view.findViewById(R.id.breakCountText)
        avgAngleText = view.findViewById(R.id.avgAngleText)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        setupChart()
        observeViewModel()

        return view
    }

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setNoDataText("측정된 데이터가 없습니다.")

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.BLACK
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val millis = value.toLong()
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
                        return String.format("%02d:%02d", minutes, seconds)
                    }
                }
            }

            axisLeft.textColor = Color.BLACK
            axisRight.isEnabled = false
        }
    }

    private fun observeViewModel() {
        sharedViewModel.statisticsResult.observe(viewLifecycleOwner) { statsData ->
            if (statsData != null && statsData.neckAnglesOverTime.isNotEmpty()) {
                val entries = ArrayList<Entry>()
                statsData.neckAnglesOverTime.forEach { (timeMs, angle) ->
                    entries.add(Entry(timeMs.toFloat(), angle.toFloat()))
                }

                val dataSet = LineDataSet(entries, "목 각도 변화").apply {
                    color = Color.BLUE
                    setDrawCircles(false)
                    setDrawValues(false)
                }

                lineChart.data = LineData(dataSet)

                val threshold = PoseAnalyzer.getCustomThreshold().toFloat()
                val limitLine = LimitLine(threshold, "자세 기준 (${String.format("%.1f", threshold)}°)").apply {
                    lineColor = Color.RED
                    lineWidth = 2f
                    textColor = Color.BLACK
                    textSize = 12f
                }

                // ✅ [수정] '기준선 중심'의 동적 범위 설정 로직
                val allAngles = statsData.neckAnglesOverTime.map { it.second.toFloat() }
                val minAngle = allAngles.minOrNull() ?: threshold
                val maxAngle = allAngles.maxOrNull() ?: threshold

                // 1. 기준선 중심의 '관심 영역'을 정의합니다 (예: ±15도).
                val interestZoneBuffer = 15f
                val interestZoneMin = threshold - interestZoneBuffer
                val interestZoneMax = threshold + interestZoneBuffer

                // 2. 실제 데이터와 '관심 영역'을 모두 포함하는 최종 범위를 계산합니다.
                val finalMin = min(minAngle, interestZoneMin)
                val finalMax = max(maxAngle, interestZoneMax)

                // 3. 최종 범위에 약간의 여백을 주어 Y축 범위를 설정합니다.
                lineChart.axisLeft.axisMinimum = finalMin - 5f
                lineChart.axisLeft.axisMaximum = finalMax + 5f

                lineChart.axisLeft.removeAllLimitLines()
                lineChart.axisLeft.addLimitLine(limitLine)
                
                lineChart.invalidate()

                val diagnosis = AIAnalyzer.analyze(statsData)
                aiDiagnosisText.text = diagnosis
                
                val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(statsData.totalMeasurementTimeMs)
                val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(statsData.totalMeasurementTimeMs) % 60
                totalTimeText.text = "- 총 측정 시간: ${totalMinutes}분 ${totalSeconds}초"

                val badMinutes = TimeUnit.MILLISECONDS.toMinutes(statsData.badPostureTimeMs)
                val badSeconds = TimeUnit.MILLISECONDS.toSeconds(statsData.badPostureTimeMs) % 60
                badPostureTimeText.text = "- 자세가 무너진 시간: ${badMinutes}분 ${badSeconds}초"

                breakCountText.text = "- 자세가 무너진 횟수: ${statsData.postureBreakCount}회"
                avgAngleText.text = "- 평균 목 각도: ${String.format("%.1f", statsData.averageNeckAngle)}도"

            } else {
                lineChart.clear()
                aiDiagnosisText.text = "- 진단: 데이터 부족\n- 조언: 의미 있는 분석을 위해 최소 1분 이상 측정해주세요."
                totalTimeText.text = "- 총 측정 시간: 0분 0초"
                badPostureTimeText.text = "- 자세가 무너진 시간: 0분 0초"
                breakCountText.text = "- 자세가 무너진 횟수: 0회"
                avgAngleText.text = "- 평균 목 각도: 0.0도"
            }
        }
    }
}