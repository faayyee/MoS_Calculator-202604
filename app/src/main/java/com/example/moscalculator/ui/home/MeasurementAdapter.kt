package com.example.moscalculator.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moscalculator.data.MeasurementRecord
import com.example.moscalculator.databinding.ItemRecordHistoryBinding

class MeasurementAdapter(
    private var records: List<MeasurementRecord>,
    private val onItemClick: (MeasurementRecord) -> Unit
) : RecyclerView.Adapter<MeasurementAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRecordHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(record: MeasurementRecord) {
            binding.textDate.text = record.date
            binding.textWalkTime.text = "歩き時間：${record.durationText}"
            binding.textMemo.text = if (record.memo.isNullOrBlank()) "メモなし" else record.memo

            binding.root.setOnClickListener {
                onItemClick(record)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecordHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = records.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
    }

    fun updateData(newRecords: List<MeasurementRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}
