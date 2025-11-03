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

    fun saveViewingDirection(context: Context, direction: PoseAnalyzer.ViewingDirection) {
        getPreferences(context).edit().putString(KEY_VIEWING_DIRECTION, direction.name).apply()
    }

    fun getViewingDirection(context: Context): PoseAnalyzer.ViewingDirection {
        val savedName = getPreferences(context).getString(KEY_VIEWING_DIRECTION, PoseAnalyzer.ViewingDirection.RIGHT.name)
        return PoseAnalyzer.ViewingDirection.valueOf(savedName ?: PoseAnalyzer.ViewingDirection.RIGHT.name)
    }

    fun saveCalibrationData(context: Context, mean: Float, stdDev: Float) {
        getPreferences(context).edit()
            .putFloat(KEY_CALIBRATION_MEAN, mean)
            .putFloat(KEY_CALIBRATION_STD_DEV, stdDev)
            .apply()
    }

    fun getCalibrationData(context: Context): Pair<Float, Float> {
        val prefs = getPreferences(context)
        val mean = prefs.getFloat(KEY_CALIBRATION_MEAN, 0.0f)
        val stdDev = prefs.getFloat(KEY_CALIBRATION_STD_DEV, 7.5f) // 기본 표준편차
        return Pair(mean, stdDev)
    }
}