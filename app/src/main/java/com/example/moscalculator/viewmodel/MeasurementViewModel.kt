package com.example.moscalculator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.moscalculator.data.AppDatabase
import com.example.moscalculator.data.MeasurementRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val allRecords: LiveData<List<MeasurementRecord>> =
        db.measurementDao().getAll().asLiveData()
}
