package com.example.neckfree.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.neckfree.db.AppDatabase
import com.example.neckfree.db.MeasurementRecord
import com.example.neckfree.db.MeasurementRepository
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MeasurementRepository
    val allRecords: LiveData<List<MeasurementRecord>>

    init {
        val recordDao = AppDatabase.getDatabase(application).measurementRecordDao()
        repository = MeasurementRepository(recordDao)
        allRecords = repository.allRecords
    }

    fun insert(record: MeasurementRecord) = viewModelScope.launch {
        repository.insert(record)
    }

    fun delete(record: MeasurementRecord) = viewModelScope.launch {
        repository.delete(record)
    }

    // ✅ [추가] ID로 특정 기록을 가져오는 함수
    fun getRecordById(recordId: Long): LiveData<MeasurementRecord?> {
        return repository.getRecordById(recordId)
    }
}