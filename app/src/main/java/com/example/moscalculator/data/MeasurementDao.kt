package com.example.moscalculator.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Insert
    suspend fun insert(record: MeasurementRecord)

    @Query("SELECT * FROM MeasurementRecord ORDER BY timestamp DESC")
    fun getAll(): Flow<List<MeasurementRecord>>

}
