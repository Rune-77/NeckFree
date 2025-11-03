package com.example.neckfree.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "measurement_records")
@TypeConverters(Converters::class)
data class MeasurementRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timestamp: Long = System.currentTimeMillis(), // 측정 완료 시점

    // 통계 데이터
    val goodPostureCount: Int,
    val badPostureCount: Int,
    val totalMeasurementTimeMs: Long,
    val postureBreakCount: Int,
    val averageNeckAngle: Double,
    val badPostureTimeMs: Long,
    val neckAnglesOverTime: List<Pair<Long, Double>>, // 그래프 데이터
    val aiDiagnosis: String, // AI 진단 결과

    // ✅ [추가] 보정 데이터
    val calibratedMean: Double,
    val calibratedStdDev: Double,
    val upperThreshold: Double,
    val lowerThreshold: Double
)