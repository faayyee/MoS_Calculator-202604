package com.example.moscalculator.ui.notifications

import android.R
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moscalculator.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //性别选项
    private val genderOptions = listOf("男性", "女性", "その他")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupGenderDropdown()
        loadUserInfo()

        binding.buttonRegister.setOnClickListener {
            showEditForm()
        }

        binding.buttonSave.setOnClickListener {
            saveUserInfo()
        }

        binding.textEdit.setOnClickListener {
            showEditForm()
        }
        return root
    }

    //设置下拉框
    private fun setupGenderDropdown() {
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = adapter
    }

    //加载个人信息(getSharedPreferences)
    private fun loadUserInfo() {
        val prefs = requireContext().getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        val gender = prefs.getString("gender", null)
        val height = prefs.getInt("height", -1)

        if (gender != null && height > 0) {
            showUserInfo(gender, height)
        } else {
            showRegisterPage()
        }
    }

    //注册页面（最初）
    private fun showRegisterPage() {
        binding.layoutRegister.visibility = View.VISIBLE
        binding.layoutEditForm.visibility = View.GONE
        binding.layoutDisplayInfo.visibility = View.GONE
    }
    //编辑页面
    private fun showEditForm() {
        binding.layoutRegister.visibility = View.GONE
        binding.layoutEditForm.visibility = View.VISIBLE
        binding.layoutDisplayInfo.visibility = View.GONE
        binding.textEdit.visibility = View.GONE
    }
    //显示个人信息
    private fun showUserInfo(gender: String, height: Int) {
        binding.textGender.text = "性別：$gender"
        binding.textHeight.text = "身長：${height} cm"

        binding.layoutRegister.visibility = View.GONE
        binding.layoutEditForm.visibility = View.GONE
        binding.layoutDisplayInfo.visibility = View.VISIBLE
        binding.textEdit.visibility = View.VISIBLE
    }
    //保存信息(getSharedPreferences)
    private fun saveUserInfo() {
        val gender = binding.spinnerGender.selectedItem.toString()
        val heightStr = binding.editTextHeight.text.toString()

        if (heightStr.isNotBlank()) {
            val height = heightStr.toIntOrNull() ?: return
            val prefs = requireContext().getSharedPreferences("user_profile", Context.MODE_PRIVATE)
            prefs.edit().putString("gender", gender).putInt("height", height).apply()

            showUserInfo(gender, height)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}