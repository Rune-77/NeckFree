package com.example.neckfree

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "NeckFree_Settings"
    private const val KEY_VIEWING_DIRECTION = "viewing_direction"
    private const val KEY_CALIBRATION_MEAN = "calibration_mean"
    private const val KEY_CALIBRATION_STD_DEV = "calibration_std_dev"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveViewingDirection(context: Context, direction: PoseAnalyzer.ViewingDirection, userId: Long) {
        getPreferences(context).edit().putString("${KEY_VIEWING_DIRECTION}_${userId}", direction.name).apply()
    }

    fun getViewingDirection(context: Context, userId: Long): PoseAnalyzer.ViewingDirection {
        val defaultDirection = PoseAnalyzer.ViewingDirection.RIGHT.name
        val savedName = getPreferences(context).getString("${KEY_VIEWING_DIRECTION}_${userId}", defaultDirection)
        return PoseAnalyzer.ViewingDirection.valueOf(savedName ?: defaultDirection)
    }

    fun saveCalibrationData(context: Context, mean: Float, stdDev: Float, userId: Long) {
        getPreferences(context).edit()
            .putFloat("${KEY_CALIBRATION_MEAN}_${userId}", mean)
            .putFloat("${KEY_CALIBRATION_STD_DEV}_${userId}", stdDev)
            .apply()
    }

    fun getCalibrationData(context: Context, userId: Long): Pair<Float, Float> {
        val prefs = getPreferences(context)
        val mean = prefs.getFloat("${KEY_CALIBRATION_MEAN}_${userId}", 0.0f)
        val stdDev = prefs.getFloat("${KEY_CALIBRATION_STD_DEV}_${userId}", 7.5f) // 기본 표준편차
        return Pair(mean, stdDev)
    }
}