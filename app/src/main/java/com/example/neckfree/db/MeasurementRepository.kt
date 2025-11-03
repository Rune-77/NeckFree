package com.example.neckfree.db

class MeasurementRepository(private val measurementRecordDao: MeasurementRecordDao) {

    suspend fun insertRecord(record: MeasurementRecord) {
        measurementRecordDao.insert(record)
    }
}
