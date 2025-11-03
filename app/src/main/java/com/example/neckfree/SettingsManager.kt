package com.example.neckfree

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "NeckFree_Settings"
    private const val KEY_VIEWING_DIRECTION = "viewing_direction"

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
}