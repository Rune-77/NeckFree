package com.example.neckfree.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromNeckAngleList(neckAngles: List<Pair<Long, Double>>): String {
        return gson.toJson(neckAngles)
    }

    @TypeConverter
    fun toNeckAngleList(neckAnglesString: String): List<Pair<Long, Double>> {
        val listType = object : TypeToken<List<Pair<Long, Double>>>() {}.type
        return gson.fromJson(neckAnglesString, listType)
    }
}
