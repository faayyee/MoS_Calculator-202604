package com.example.moscalculator.ui.dashboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moscalculator.databinding.FragmentDashboardBinding
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.moscalculator.R
import java.util.Locale
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.example.moscalculator.data.AppDatabase
import com.example.moscalculator.data.MeasurementRecord
import com.example.moscalculator.ui.shared.SharedSensorViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var isMeasuring = false  // 按钮状态标志位

    //计时相关
    private var seconds = 0
    private var isRunning = false
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    //传感器相关
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private val sensorDataList = mutableListOf<String>() // 用于保存六轴数据
    private val sharedViewModel: SharedSensorViewModel by activityViewModels()
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val timestamp = System.currentTimeMillis()
                val values = it.values
                val data = when (it.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> "ACC,$timestamp,${values[0]},${values[1]},${values[2]}"
                    Sensor.TYPE_GYROSCOPE -> "GYR,$timestamp,${values[0]},${values[1]},${values[2]}"
                    else -> ""
                }
                if (data.isNotEmpty()) sensorDataList.add(data)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    //reset
    private fun resetUI() {
        // 重置计时器
        seconds = 0
        isRunning = false
        binding.timerText.text = "00:00"
        binding.editTextMemo.text = null

        // 重置按钮状态和可见性
        binding.buttonStart.visibility = View.VISIBLE
        binding.buttonStart.isEnabled = true
        binding.buttonMeasuring.visibility = View.GONE
        binding.buttonStop.visibility = View.GONE
        binding.buttonRestart.visibility = View.GONE
        binding.buttonShowReport.visibility = View.GONE
        binding.editTextMemo.visibility = View.VISIBLE

        // 清除停止时的闪烁动画
        binding.textStoppedStatus.clearAnimation()
        binding.textStoppedStatus.visibility = View.GONE

        // 清空传感器数据
        sensorDataList.clear()
    }



    //开始计时
    private fun startTimer() {
        seconds = 1
        binding.textStoppedStatus.clearAnimation()
        binding.textStoppedStatus.visibility = View.GONE

        binding.timerText.visibility = View.VISIBLE
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                val minutes = seconds / 60
                val secs = seconds % 60
                binding.timerText.text = String.format(Locale.US, "%02d:%02d", minutes, secs)
                seconds++
                handler.postDelayed(this, 1000)
            }
        }
        isRunning = true
        handler.post(runnable)
        accelerometer?.let { sensorManager.registerListener(sensorEventListener, it, 10000) }
        gyroscope?.let { sensorManager.registerListener(sensorEventListener, it, 10000) }

    }
    //停止计时
    private fun stopTimer() {
        isRunning = false
        handler.removeCallbacks(runnable)
        sensorManager.unregisterListener(sensorEventListener)
    }

    //保存到数据库
    private fun saveSensorDataToDatabase(sensorList: List<String>) {
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java, "sensor-db"
        ).build()
        val dao = db.measurementDao()

        // 整理数据为字符串形式（分开 acc 和 gyr）
        val accValues = mutableListOf<String>()
        val gyrValues = mutableListOf<String>()

        for (entry in sensorList) {
            val parts = entry.split(",")
            if (parts.size == 5) {
                when (parts[0]) {
                    "ACC" -> accValues.add("${parts[2]},${parts[3]},${parts[4]}")
                    "GYR" -> gyrValues.add("${parts[2]},${parts[3]},${parts[4]}")
                }
            }
        }

        val accX = accValues.map { it.split(",")[0] }.joinToString(",")
        val accY = accValues.map { it.split(",")[1] }.joinToString(",")
        val accZ = accValues.map { it.split(",")[2] }.joinToString(",")
        val gyrX = gyrValues.map { it.split(",")[0] }.joinToString(",")
        val gyrY = gyrValues.map { it.split(",")[1] }.joinToString(",")
        val gyrZ = gyrValues.map { it.split(",")[2] }.joinToString(",")

        val timestamp = System.currentTimeMillis()
        val rawDuration = seconds-1
        val durationDisplay = binding.timerText.text.toString()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val memoText = binding.editTextMemo.text.toString()

        val record = MeasurementRecord(
            timestamp = timestamp,
            durationSeconds = rawDuration,
            durationText = durationDisplay,
            accX = accX,
            accY = accY,
            accZ = accZ,
            gyrX = gyrX,
            gyrY = gyrY,
            gyrZ = gyrZ,
            date = dateStr,
            memo = memoText
        )

        // 使用协程插入
        lifecycleScope.launch {
            dao.insert(record)
        }
    }

    private fun loadSensorDataFromCsv(context: Context): List<String> {
        val sensorDataList = mutableListOf<String>()

        try {
            val inputStream = context.assets.open("wata_full_2.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            var baseTimestamp = System.currentTimeMillis()


            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size >= 6) {
                    val accX = parts[0].trim()
                    val accY = parts[1].trim()
                    val accZ = parts[2].trim()

                    val gyrX = parts[3].trim()
                    val gyrY = parts[4].trim()
                    val gyrZ = parts[5].trim()

                    val timestamp = baseTimestamp

                    val accLine = "ACC,$timestamp,$accX,$accY,$accZ"
                    val gyrLine = "GYR,$timestamp,$gyrX,$gyrY,$gyrZ"

                    sensorDataList.add(accLine)
                    sensorDataList.add(gyrLine)


                }
            }

            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sensorDataList
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textDashboard
//        dashboardViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

        //传感器相关
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)



        // 设置按钮点击逻辑
        binding.buttonStart.setOnClickListener {

            if (!isRunning) {
                startTimer()
                binding.buttonStart.isEnabled = false
                binding.buttonStart.visibility = View.GONE
                binding.editTextMemo.visibility = View.GONE
                binding.buttonMeasuring.visibility = View.VISIBLE
                binding.buttonStop.visibility = View.VISIBLE
                binding.buttonMeasuring.isEnabled = false
            }
//            isMeasuring = !isMeasuring
//
//            if (isMeasuring) {
//               binding.buttonStart.visibility = View.GONE
//                binding.editTextMemo.visibility = View.GONE
//               binding.buttonMeasuring.visibility = View.VISIBLE
//                binding.buttonStop.visibility = View.VISIBLE
//                binding.buttonMeasuring.isEnabled = false
//            } else {
//                binding.buttonStart.text = "計測開始"
//                binding.buttonStart.setTextColor(
//                    ContextCompat.getColor(requireContext(), R.color.gray_200)
//                )
//                binding.buttonStart.backgroundTintList =
//                    ColorStateList.valueOf("#D9D9D9".toColorInt()) // 灰色
//            }
        }


        binding.buttonStop.setOnClickListener {
            stopTimer()
            binding.buttonRestart.visibility = View.VISIBLE
            binding.buttonMeasuring.visibility = View.INVISIBLE
            binding.buttonStop.visibility = View.INVISIBLE
            binding.buttonShowReport.visibility = View.VISIBLE

            binding.textStoppedStatus.visibility = View.VISIBLE  // 确保它可见

            val blinkAnimation = AlphaAnimation(1.0f, 0.0f).apply {
                duration = 1000
                repeatCount = Animation.INFINITE
                repeatMode = Animation.REVERSE
            }
            binding.textStoppedStatus.startAnimation(blinkAnimation)
        }

        binding.buttonRestart.setOnClickListener {
            if (!isRunning) {
                startTimer()
                binding.buttonRestart.visibility = View.INVISIBLE
                binding.buttonMeasuring.visibility = View.VISIBLE
                binding.buttonShowReport.visibility = View.INVISIBLE
                binding.buttonStop.visibility = View.VISIBLE



            }
        }

        //显示结果按钮
        binding.buttonShowReport.setOnClickListener {
            sharedViewModel.setSensorData(sensorDataList.toList())

           // 添加保存逻辑
            saveSensorDataToDatabase(sensorDataList)

            // val mockData = loadSensorDataFromCsv(requireContext())

//            mockData.forEachIndexed { index, line ->
//                Log.d("MockData", "[$index] $line")
//            }
            // sharedViewModel.setSensorData(mockData.toList())

            findNavController().navigate(R.id.navigation_result)
        }




        return root
    }

    override fun onResume() {
        super.onResume()
        resetUI()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}