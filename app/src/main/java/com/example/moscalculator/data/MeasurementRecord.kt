package com.example.moscalculator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MeasurementRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val durationSeconds: Int,
    val durationText: String ,
    val accX: String,
    val accY: String,
    val accZ: String,
    val gyrX: String,
    val gyrY: String,
    val gyrZ: String,
    val date: String,
    val memo: String? = null
)
