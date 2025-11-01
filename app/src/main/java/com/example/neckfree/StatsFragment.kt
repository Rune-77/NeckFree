package com.example.neckfree

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

class StatsFragment : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var pieChart: PieChart
    private lateinit var goodPosturePercentageText: TextView
    private lateinit var badPosturePercentageText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        pieChart = view.findViewById(R.id.pieChart)
        goodPosturePercentageText = view.findViewById(R.id.goodPosturePercentage)
        badPosturePercentageText = view.findViewById(R.id.badPosturePercentage)

        // ViewModel 초기화
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        setupChart()
        observeViewModel()

        return view
    }

    private fun setupChart() {
        pieChart.apply {
            isDrawHoleEnabled = true // 가운데 구멍
            holeRadius = 58f
            transparentCircleRadius = 61f
            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = false // 범례 비활성화
            setDrawEntryLabels(false) // 라벨 비활성화
        }
    }

    private fun observeViewModel() {
        sharedViewModel.statisticsResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                val goodCount = result.first
                val badCount = result.second
                val totalCount = goodCount + badCount

                if (totalCount > 0) {
                    val goodPercentage = (goodCount.toFloat() / totalCount) * 100
                    val badPercentage = (badCount.toFloat() / totalCount) * 100

                    // 데이터 설정
                    val entries = ArrayList<PieEntry>()
                    entries.add(PieEntry(goodPercentage, "좋은 자세"))
                    entries.add(PieEntry(badPercentage, "나쁜 자세"))

                    val dataSet = PieDataSet(entries, "자세 결과")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f

                    val pieData = PieData(dataSet)
                    pieData.setValueFormatter(PercentFormatter(pieChart)) // 퍼센트 포맷 지정
                    
                    pieChart.data = pieData
                    pieChart.invalidate() // 차트 갱신

                    // 텍스트 뷰 업데이트
                    goodPosturePercentageText.text = "- 좋은 자세: ${String.format("%.1f", goodPercentage)}%"
                    badPosturePercentageText.text = "- 나쁜 자세: ${String.format("%.1f", badPercentage)}%"
                }
            }
        }
    }
}