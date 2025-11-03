package com.example.neckfree.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromPairList(value: List<Pair<Long, Double>>?): String? {
        val gson = Gson()
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toPairList(value: String?): List<Pair<Long, Double>>? {
        val gson = Gson()
        val type = object : TypeToken<List<Pair<Long, Double>>>() {}.type
        return value?.let { gson.fromJson(it, type) }
    }
}
