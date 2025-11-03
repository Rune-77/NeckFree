package com.example.neckfree.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.neckfree.db.AppDatabase
import com.example.neckfree.db.MeasurementRecord
import com.example.neckfree.db.MeasurementRecordDao

class RecordDetailViewModel(application: Application, recordId: Long) : AndroidViewModel(application) {

    private val recordDao: MeasurementRecordDao
    val record: LiveData<MeasurementRecord?>

    init {
        recordDao = AppDatabase.getDatabase(application).measurementRecordDao()
        record = recordDao.getRecordById(recordId)
    }
}
