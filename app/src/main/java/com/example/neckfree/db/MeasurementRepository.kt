package com.example.neckfree.db

import androidx.lifecycle.LiveData

class MeasurementRepository(private val measurementRecordDao: MeasurementRecordDao) {

    val allRecords: LiveData<List<MeasurementRecord>> = measurementRecordDao.getAllRecords()

    suspend fun insert(record: MeasurementRecord) {
        measurementRecordDao.insert(record)
    }

    suspend fun delete(record: MeasurementRecord) {
        measurementRecordDao.delete(record)
    }

    // ✅ [추가] ID로 특정 기록을 가져오는 통로
    fun getRecordById(recordId: Long): LiveData<MeasurementRecord?> {
        return measurementRecordDao.getRecordById(recordId)
    }
}
