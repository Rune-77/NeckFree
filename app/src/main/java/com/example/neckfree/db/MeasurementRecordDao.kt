package com.example.neckfree.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MeasurementRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MeasurementRecord)

    @Query("SELECT * FROM measurement_records ORDER BY timestamp DESC")
    fun getAllRecords(): LiveData<List<MeasurementRecord>>

    // ✅ [추가] ID로 특정 기록 하나만 가져오는 기능
    @Query("SELECT * FROM measurement_records WHERE id = :recordId")
    fun getRecordById(recordId: Long): LiveData<MeasurementRecord?>

}
