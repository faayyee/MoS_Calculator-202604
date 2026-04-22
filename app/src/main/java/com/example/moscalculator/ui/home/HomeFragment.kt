package com.example.moscalculator.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.moscalculator.R
import com.example.moscalculator.data.AppDatabase
import com.example.moscalculator.databinding.FragmentHomeBinding
import com.example.moscalculator.viewmodel.MeasurementViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: MeasurementViewModel
    private lateinit var adapter: MeasurementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[MeasurementViewModel::class.java]

        adapter = MeasurementAdapter(emptyList()) { record ->
            val bundle = Bundle().apply {
                putString("accX", record.accX)
                putString("accY", record.accY)
                putString("accZ", record.accZ)
                putString("gyrX", record.gyrX)
                putString("gyrY", record.gyrY)
                putString("gyrZ", record.gyrZ)
                putString("duration", record.durationText)
                putString("date", record.date)
                putString("memo", record.memo ?: "")
            }


        }

        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewHistory.adapter = adapter

        // 监听数据库数据变动
        viewModel.allRecords.observe(viewLifecycleOwner) { records ->
            adapter.updateData(records)
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}