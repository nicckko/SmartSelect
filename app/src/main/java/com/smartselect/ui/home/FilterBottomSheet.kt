package com.smartselect.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartselect.databinding.BottomSheetFilterBinding
import com.smartselect.viewmodel.PhoneViewModel

class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterBinding? = null
    private val binding get() = _binding!!
    private val phoneViewModel: PhoneViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnApply.setOnClickListener {
            val minText = binding.etMinPrice.text.toString().trim()
            val maxText = binding.etMaxPrice.text.toString().trim()

            val min = minText.toDoubleOrNull() ?: 0.0
            val max = maxText.toDoubleOrNull() ?: Double.MAX_VALUE

            // Fixed: validate that min is not greater than max
            if (minText.isNotEmpty() && maxText.isNotEmpty() && min > max) {
                binding.etMinPrice.error = "Min price cannot exceed max price"
                return@setOnClickListener
            }

            phoneViewModel.searchPhones(
                query = "",
                minPrice = min,
                maxPrice = max
            )
            dismiss()
        }

        binding.btnReset.setOnClickListener {
            phoneViewModel.loadPhones()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
