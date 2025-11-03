package com.example.neckfree.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neckfree.db.AppDatabase
import com.example.neckfree.db.MeasurementRecord
import com.example.neckfree.db.MeasurementRepository
import kotlinx.coroutines.launch

class LiveViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MeasurementRepository

    init {
        val measurementRecordDao = AppDatabase.getDatabase(application).measurementRecordDao()
        repository = MeasurementRepository(measurementRecordDao)
    }

    fun insert(record: MeasurementRecord) = viewModelScope.launch {
        repository.insertRecord(record)
    }
}
