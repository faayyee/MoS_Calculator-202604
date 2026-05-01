package com.example.moscalculator.ui.result

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.moscalculator.R
import com.example.moscalculator.databinding.FragmentResultBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.moscalculator.ui.shared.SharedSensorViewModel
import com.github.mikephil.charting.components.LimitLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

class ResultFragment : Fragment() {
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedSensorViewModel by activityViewModels()
    private lateinit var accChart: LineChart
    private lateinit var gyrChart: LineChart

    private fun showChart(dataList: List<String>) {
        val accX = ArrayList<Entry>()
        val accY = ArrayList<Entry>()
        val accZ = ArrayList<Entry>()

        val gyrX = ArrayList<Entry>()
        val gyrY = ArrayList<Entry>()
        val gyrZ = ArrayList<Entry>()

        var accIndex = 0f
        var gyrIndex = 0f

        accChart.description.isEnabled = false
        gyrChart.description.isEnabled = false

        for (line in dataList) {
            val parts = line.split(",")
            if (parts.size != 5) continue

            val type = parts[0]
            val x = parts[2].toFloatOrNull() ?: continue
            val y = parts[3].toFloatOrNull() ?: continue
            val z = parts[4].toFloatOrNull() ?: continue

            when (type) {
                "ACC" -> {
                    accX.add(Entry(accIndex, x))
                    accY.add(Entry(accIndex, y))
                    accZ.add(Entry(accIndex, z))
                    accIndex++
                }
                "GYR" -> {
                    gyrX.add(Entry(gyrIndex, x))
                    gyrY.add(Entry(gyrIndex, y))
                    gyrZ.add(Entry(gyrIndex, z))
                    gyrIndex++
                }
            }
        }

        drawLinesOnChart(accChart, accX, accY, accZ, "加速度")
        drawLinesOnChart(gyrChart, gyrX, gyrY, gyrZ, "角速度")
    }

    private fun drawLinesOnChart(
        chart: LineChart,
        xData: List<Entry>,
        yData: List<Entry>,
        zData: List<Entry>,
        labelPrefix: String
    ) {
        val xSet = LineDataSet(xData, "${labelPrefix} X").apply {
            color = Color.RED
            setDrawCircles(false)
        }
        val ySet = LineDataSet(yData, "${labelPrefix} Y").apply {
            color = Color.BLUE
            setDrawCircles(false)
        }
        val zSet = LineDataSet(zData, "${labelPrefix} Z").apply {
            color = Color.GREEN
            setDrawCircles(false)
        }

        val lineData = LineData(xSet, ySet, zSet)
        chart.data = lineData
        chart.invalidate()
    }

    //绘制竖线
    private fun drawSegmentLines(chart: LineChart, startIndexes: List<Int>) {
        val xAxis = chart.xAxis
        xAxis.removeAllLimitLines()  // 可选：每次重新绘制前清除旧的线
        for (index in startIndexes) {
            val limitLine = LimitLine(index.toFloat(), "")
            limitLine.lineColor = Color.BLACK
            limitLine.lineWidth = 1f
            xAxis.addLimitLine(limitLine)
        }

        chart.invalidate()
    }


    //读sample数据
    private fun loadSampleFromAssets(context: Context): List<Float> {
        return context.assets.open("avg_sample.csv").bufferedReader().readLines()
            .mapNotNull { it.toFloatOrNull() }
    }

    //数据切割
    data class SegmentResult(val segment: List<Float>, val startIndex: Int)

    private fun cutCyclesFromData(
        sample: List<Float>,
        fullData: List<Float>,
        rangeScale: List<Float> = generateFloatRange(0.8f, 1.4f, 0.02f),
        thresholdCorr: Float = 0.6f
    ): List<SegmentResult> {
        val L = sample.size
        val minAdvance = (L * 0.4f).toInt()
        val segments = mutableListOf<SegmentResult>()

        var i = 0
        while (i + (L * rangeScale.max()).toInt() < fullData.size) {
            var bestCorr = -1f
            var bestSeg: List<Float>? = null
            var bestLen = 0

            for (alpha in rangeScale) {
                val segLen = (L * alpha).toInt()
                if (i + segLen >= fullData.size) continue

                val segment = fullData.subList(i, i + segLen)
                val stretchedSample = interpolate(sample, segLen)
                val corr = correlation(stretchedSample, segment)

                if (corr > bestCorr) {
                    bestCorr = corr
                    bestSeg = segment
                    bestLen = segLen
                }
            }

            if (bestCorr > thresholdCorr && bestSeg != null) {
                segments.add(SegmentResult(bestSeg, i))  // ← 记录起始索引
                i += bestLen
            } else {
                i += 1
            }
        }

        return segments
    }


    private fun generateFloatRange(start: Float, end: Float, step: Float): List<Float> {
        val result = mutableListOf<Float>()
        var current = start
        while (current <= end) {
            result.add(current)
            current += step
        }
        return result
    }


    // 插值函数，将 sample 拉伸为目标长度
    private fun interpolate(data: List<Float>, targetLen: Int): List<Float> {
        val xOld = data.indices.map { it.toFloat() / (data.size - 1) }
        val xNew = (0 until targetLen).map { it.toFloat() / (targetLen - 1) }

        return xNew.map { xi ->
            val idx = xOld.indexOfLast { it <= xi }
            val x0 = xOld.getOrElse(idx) { 0f }
            val x1 = xOld.getOrElse(idx + 1) { 1f }
            val y0 = data.getOrElse(idx) { 0f }
            val y1 = data.getOrElse(idx + 1) { 0f }

            if (x1 - x0 == 0f) y0 else y0 + (xi - x0) * (y1 - y0) / (x1 - x0)
        }
    }

    // 皮尔逊相关系数
    private fun correlation(x: List<Float>, y: List<Float>): Float {
        val n = x.size
        val meanX = x.average().toFloat()
        val meanY = y.average().toFloat()

        val numerator = x.indices.sumOf  { (x[it] - meanX) * (y[it] - meanY).toDouble() }.toFloat()
        val denominator = sqrt(
            x.sumOf { ((it - meanX).toDouble()).pow(2) } *
                    y.sumOf { ((it - meanY).toDouble()).pow(2) }
        ).toFloat()

        return if (denominator != 0f) (numerator / denominator).coerceIn(-1f, 1f) else 0f
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    //保存
    private fun exportSensorDataToCSV(context: Context, dataList: List<String>) {
        // 1. 获取当前时间戳并格式化
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "sensor_data_$timeStamp.csv"

        // 2. 保存路径保持不变
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(path, fileName)

        val csvHeader = "type,time_s,x,y,z\n"
        val csvBody = buildString {
            for (line in dataList) {
                val parts = line.split(",")
                if (parts.size != 5) continue

                val type = parts[0]
                val timeSec = parts[1]
                val x = parts[2]
                val y = parts[3]
                val z = parts[4]

                append("$type,$timeSec,$x,$y,$z\n")
            }
        }

        val fullText = csvHeader + csvBody

        try {
            file.writeText(fullText)

            android.widget.Toast.makeText(
                context,
                "CSVファイルが保存されました：${file.absolutePath}",
                android.widget.Toast.LENGTH_LONG
            ).show()
            Log.d("CSV_EXPORT", "Saved to: ${file.absolutePath}")

        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "CSV保存中にエラーが発生しました", android.widget.Toast.LENGTH_SHORT).show()
            Log.e("CSV_EXPORT", "Error saving CSV: ${e.message}")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)

        //返回
        binding.textBack.setOnClickListener {
            findNavController().navigateUp()  // 返回到上一个 Fragment
        }


        //显示图表
        binding.buttonShowGraph.setOnClickListener {
            showLoading(true)  // 显示遮罩

            lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    // 获取共享数据
                    val sensorDataList = sharedViewModel.getSensorData()

                    // 切割数据
                    val accX = sensorDataList
                        .filter { it.startsWith("ACC") }
                        .mapNotNull { it.split(",").getOrNull(2)?.toFloatOrNull() }

                    val sample = loadSampleFromAssets(requireContext())
                    val cutResults = cutCyclesFromData(sample, accX)

                    // 提取分段起点
                    val segmentStartIndexes = cutResults.map { it.startIndex }.toMutableList()

                    // 追加最后一段的结束位置（最后一段起点 + 最后一段长度）
                    if (cutResults.isNotEmpty()) {
                        val lastSegment = cutResults.last()
                        val lastEndIndex = lastSegment.startIndex + lastSegment.segment.size
                        segmentStartIndexes.add(lastEndIndex)
                    }


                    // 保存变量用于主线程绘图
                    Pair(sensorDataList, segmentStartIndexes)
                }.let { (sensorDataList, segmentStartIndexes) ->
                    // 回到主线程更新 UI
                    binding.layoutGraphContainer.visibility = View.VISIBLE
                    accChart = binding.chartAcc
                    gyrChart = binding.chartGyr

                    showChart(sensorDataList)
                    drawSegmentLines(accChart, segmentStartIndexes)
                    drawSegmentLines(gyrChart, segmentStartIndexes)

                    showLoading(false)  // 隐藏遮罩
                }
            }
        }

        //下载csv
        binding.buttonDownloadCsv.setOnClickListener {
            val sensorDataList = sharedViewModel.getSensorData()
            exportSensorDataToCSV(requireContext(), sensorDataList)
        }








        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
