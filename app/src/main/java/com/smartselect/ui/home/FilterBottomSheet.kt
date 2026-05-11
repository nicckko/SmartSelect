package com.smartselect.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
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

        // Get current price range from ViewModel if available
        val currentMin = phoneViewModel.currentMinPrice.value
        val currentMax = phoneViewModel.currentMaxPrice.value

        if (currentMin > 0) {
            binding.etMinPrice.setText(currentMin.toString())
        }
        if (currentMax < Double.MAX_VALUE) {
            binding.etMaxPrice.setText(currentMax.toString())
        }

        // Real-time validation: min price cannot exceed max price
        binding.etMinPrice.addTextChangedListener {
            validatePriceRange()
        }
        binding.etMaxPrice.addTextChangedListener {
            validatePriceRange()
        }

        binding.btnApply.setOnClickListener {
            val minText = binding.etMinPrice.text.toString().trim()
            val maxText = binding.etMaxPrice.text.toString().trim()

            val min = if (minText.isEmpty()) 0.0 else minText.toDoubleOrNull() ?: 0.0
            val max = if (maxText.isEmpty()) Double.MAX_VALUE else maxText.toDoubleOrNull() ?: Double.MAX_VALUE

            // Validate that min is not greater than max
            if (minText.isNotEmpty() && maxText.isNotEmpty() && min > max) {
                binding.etMinPrice.error = "Min price cannot exceed max price"
                return@setOnClickListener
            }

            // Apply both search query AND price filter together
            val currentQuery = phoneViewModel.currentSearchQuery.value ?: ""
            val currentCategory = phoneViewModel.currentCategory.value ?: ""

            phoneViewModel.applyFilters(
                query = currentQuery,
                category = currentCategory,
                minPrice = min,
                maxPrice = max
            )
            dismiss()
        }

        binding.btnReset.setOnClickListener {
            phoneViewModel.resetFilters()
            dismiss()
        }
    }

    private fun validatePriceRange() {
        val minText = binding.etMinPrice.text.toString().trim()
        val maxText = binding.etMaxPrice.text.toString().trim()

        if (minText.isNotEmpty() && maxText.isNotEmpty()) {
            val min = minText.toDoubleOrNull() ?: 0.0
            val max = maxText.toDoubleOrNull() ?: 0.0
            if (min > max) {
                binding.etMinPrice.error = "Min cannot exceed max"
            } else {
                binding.etMinPrice.error = null
            }
        } else {
            binding.etMinPrice.error = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}