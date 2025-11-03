package com.example.neckfree.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_records")
data class MeasurementRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val goodPostureCount: Int,
    val badPostureCount: Int,
    val totalMeasurementTimeMs: Long,
    val postureBreakCount: Int,
    val averageNeckAngle: Double,
    val badPostureTimeMs: Long,
    val neckAnglesOverTime: List<Pair<Long, Double>>, // (Time, Angle)
    val aiDiagnosis: String,
    val calibratedMean: Double,
    val calibratedStdDev: Double,
    val upperThreshold: Double,
    val lowerThreshold: Double,
    val timestamp: Long = System.currentTimeMillis()
)
