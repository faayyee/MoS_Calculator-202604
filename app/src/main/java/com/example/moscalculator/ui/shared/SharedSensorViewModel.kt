package com.example.moscalculator.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedSensorViewModel : ViewModel() {
    private val _sensorData = MutableLiveData<List<String>>()
    val sensorData: LiveData<List<String>> get() = _sensorData

    fun setSensorData(data: List<String>) {
        _sensorData.value = data
    }

    fun getSensorData(): List<String> {
        return sensorData.value ?: emptyList()
    }
}
