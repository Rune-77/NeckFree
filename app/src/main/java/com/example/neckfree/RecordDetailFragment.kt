package com.example.neckfree

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.neckfree.db.MeasurementRecord
import com.example.neckfree.viewmodel.StatsViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class RecordDetailFragment : Fragment() {

    private val args: RecordDetailFragmentArgs by navArgs()
    private lateinit var statsViewModel: StatsViewModel

    // UI 요소
    private lateinit var lineChart: LineChart
    private lateinit var aiDiagnosisText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var badPostureTimeText: TextView
    private lateinit var breakCountText: TextView
    private lateinit var avgAngleText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_record_detail, container, false)

        // 뷰 초기화
        lineChart = view.findViewById(R.id.lineChart)
        aiDiagnosisText = view.findViewById(R.id.aiDiagnosisText)
        totalTimeText = view.findViewById(R.id.totalTimeText)
        badPostureTimeText = view.findViewById(R.id.badPostureTimeText)
        breakCountText = view.findViewById(R.id.breakCountText)
        avgAngleText = view.findViewById(R.id.avgAngleText)

        statsViewModel = ViewModelProvider(this).get(StatsViewModel::class.java)

        setupChart()
        observeViewModel()

        return view
    }

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setNoDataText("데이터 로딩 중...")

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
        // 전달받은 ID로 특정 기록을 관찰
        statsViewModel.getRecordById(args.recordId).observe(viewLifecycleOwner) { record ->
            record?.let { updateDetailView(it) }
        }
    }

    private fun updateDetailView(record: MeasurementRecord) {
        if (record.neckAnglesOverTime.isNotEmpty()) {
            val entries = ArrayList<Entry>()
            record.neckAnglesOverTime.forEach { (timeMs, angle) ->
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

            val allAngles = record.neckAnglesOverTime.map { it.second.toFloat() }
            val minAngle = allAngles.minOrNull() ?: threshold
            val maxAngle = allAngles.maxOrNull() ?: threshold

            val interestZoneBuffer = 15f
            val interestZoneMin = threshold - interestZoneBuffer
            val interestZoneMax = threshold + interestZoneBuffer

            val finalMin = min(minAngle, interestZoneMin)
            val finalMax = max(maxAngle, interestZoneMax)

            lineChart.axisLeft.axisMinimum = finalMin - 5f
            lineChart.axisLeft.axisMaximum = finalMax + 5f

            lineChart.axisLeft.removeAllLimitLines()
            lineChart.axisLeft.addLimitLine(limitLine)
            
            lineChart.invalidate()

            aiDiagnosisText.text = record.aiDiagnosis
            
            val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(record.totalMeasurementTimeMs)
            val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(record.totalMeasurementTimeMs) % 60
            totalTimeText.text = "- 총 측정 시간: ${totalMinutes}분 ${totalSeconds}초"

            val badMinutes = TimeUnit.MILLISECONDS.toMinutes(record.badPostureTimeMs)
            val badSeconds = TimeUnit.MILLISECONDS.toSeconds(record.badPostureTimeMs) % 60
            badPostureTimeText.text = "- 자세가 무너진 시간: ${badMinutes}분 ${badSeconds}초"

            breakCountText.text = "- 자세가 무너진 횟수: ${record.postureBreakCount}회"
            avgAngleText.text = "- 평균 목 각도: ${String.format("%.1f", record.averageNeckAngle)}도"

        } else {
            lineChart.clear()
            aiDiagnosisText.text = "- 진단: 데이터 부족\n- 조언: 의미 있는 분석을 위해 최소 1분 이상 측정해주세요."
        }
    }
}
