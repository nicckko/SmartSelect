package com.smartselect.ui.admin

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.smartselect.R
import com.smartselect.data.PhoneSpecsData
import com.smartselect.data.model.Phone
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.data.repository.AdminLogRepository
import com.smartselect.databinding.DialogAddEditPhoneBinding
import com.smartselect.utils.CloudinaryHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddEditPhoneDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddEditPhoneBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var phoneRepository: PhoneRepository
    @Inject lateinit var adminLogRepository: AdminLogRepository

    private var editPhone: Phone? = null
    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String = ""

    companion object {
        private const val ARG_PHONE = "phone"
        private const val TAG = "AddEditPhoneDialog"
        fun newInstance(phone: Phone) = AddEditPhoneDialog().apply {
            arguments = Bundle().apply { putParcelable(ARG_PHONE, phone) }
        }
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            selectedImageUri = uri
            Glide.with(requireContext()).load(uri).centerCrop().into(binding.ivPhonePreview)
            binding.ivPhonePreview.visibility = View.VISIBLE
            binding.tvImageHint.text = "Image selected ✓"
            binding.tvImageHint.setTextColor(requireContext().getColor(R.color.success))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        editPhone = arguments?.getParcelable(ARG_PHONE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddEditPhoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAllDropdowns()
        setupImagePicker()
        populateFormIfEditing()
        setupButtons()
    }

    // ── Substring-matching ArrayAdapter ──────────────────────────────────────
    private class SubstringFilterAdapter(
        context: android.content.Context,
        private val allItems: List<String>
    ) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, allItems.toMutableList()),
        Filterable {

        private var filteredItems: List<String> = allItems

        override fun getCount() = filteredItems.size
        override fun getItem(position: Int) = filteredItems[position]

        override fun getFilter(): Filter = object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase() ?: ""
                val results = if (query.isEmpty()) allItems
                else allItems.filter { it.lowercase().contains(query) }
                return FilterResults().apply { values = results; count = results.size }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = (results?.values as? List<String>) ?: allItems
                notifyDataSetChanged()
            }
        }
    }

    private fun setupSubstringDropdown(
        autoComplete: android.widget.AutoCompleteTextView,
        items: List<String>
    ) {
        val adapter = SubstringFilterAdapter(requireContext(), items)
        autoComplete.setAdapter(adapter)
        autoComplete.threshold = 1
        autoComplete.setOnClickListener { autoComplete.showDropDown() }
        autoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && autoComplete.text.isNullOrEmpty()) autoComplete.showDropDown()
        }
    }

    // ── Setup all dropdowns ──────────────────────────────────────────────────
    private fun setupAllDropdowns() {
        // Brand
        setupSubstringDropdown(binding.etBrand, PhoneSpecsData.brands)

        // Model — updates when brand changes
        binding.etBrand.setOnItemClickListener { _, _, _, _ ->
            val brand = binding.etBrand.text.toString().trim()
            val models = PhoneSpecsData.modelsByBrand[brand] ?: emptyList()
            binding.etModel.setText("")
            setupSubstringDropdown(binding.etModel, models)
        }
        // Default model list (all models combined)
        setupSubstringDropdown(binding.etModel, PhoneSpecsData.modelsByBrand.values.flatten())

        // Category
        setupSubstringDropdown(binding.actvCategory, PhoneSpecsData.categories)

        // Specs
        setupSubstringDropdown(binding.etRam, PhoneSpecsData.ram)
        setupSubstringDropdown(binding.etStorage, PhoneSpecsData.storage)
        setupSubstringDropdown(binding.etBattery, PhoneSpecsData.battery)
        setupSubstringDropdown(binding.etChipset, PhoneSpecsData.chipsets)
        setupSubstringDropdown(binding.etCamera, PhoneSpecsData.camera)
        setupSubstringDropdown(binding.etDisplay, PhoneSpecsData.display)
    }

    private fun setupImagePicker() {
        binding.btnPickImage.setOnClickListener { imagePickerLauncher.launch("image/*") }
    }

    private fun populateFormIfEditing() {
        val phone = editPhone ?: return
        binding.tvDialogTitle.text = "Edit Phone"
        binding.tvDialogSubtitle.text = "Update the phone specifications"

        binding.etBrand.setText(phone.brand, false)
        // Load brand-specific models
        val models = PhoneSpecsData.modelsByBrand[phone.brand] ?: PhoneSpecsData.modelsByBrand.values.flatten()
        setupSubstringDropdown(binding.etModel, models)
        binding.etModel.setText(phone.model, false)

        binding.etPrice.setText(phone.price.toLong().toString())
        binding.etStock.setText(phone.stock.toString())
        binding.actvCategory.setText(phone.category, false)
        binding.etRam.setText(phone.ram, false)
        binding.etStorage.setText(phone.storage, false)
        binding.etCamera.setText(phone.camera, false)
        binding.etBattery.setText(phone.battery, false)
        binding.etChipset.setText(phone.chipset, false)
        binding.etDisplay.setText(phone.display, false)
        binding.switchBestValue.isChecked = phone.isBestValue

        existingImageUrl = phone.imageUrl
        Log.d(TAG, "Existing image URL: $existingImageUrl")

        if (phone.imageUrl.isNotEmpty()) {
            binding.ivPhonePreview.visibility = View.VISIBLE
            binding.tvImageHint.text = "Current image (tap to change)"
            Glide.with(requireContext())
                .load(phone.imageUrl)
                .placeholder(R.drawable.placeholder_phone)
                .error(R.drawable.placeholder_phone)
                .into(binding.ivPhonePreview)
        }
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { savePhone() }
    }

    private fun savePhone() {
        val phone = buildPhoneFromForm() ?: return
        lifecycleScope.launch {
            setLoadingState(true)

            // Check for duplicate
            val isDuplicate = phoneRepository.isDuplicate(phone.brand, phone.model, editPhone?.id)
            if (isDuplicate) {
                setLoadingState(false)
                Snackbar.make(binding.root, "Error: This phone already exists", Snackbar.LENGTH_LONG).show()
                return@launch
            }

            // Upload image to Cloudinary (replaces Firebase Storage)
            val finalImageUrl = if (selectedImageUri != null) {
                try {
                    val url = CloudinaryHelper.uploadImage(selectedImageUri!!)
                    Log.d(TAG, "Cloudinary upload successful: $url")
                    url
                } catch (e: Exception) {
                    Log.e(TAG, "Cloudinary upload failed", e)
                    Snackbar.make(binding.root, "Image upload failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    existingImageUrl
                }
            } else {
                existingImageUrl
            }

            val phoneToSave = phone.copy(imageUrl = finalImageUrl)

            if (editPhone != null) {
                phoneRepository.updatePhone(phoneToSave.copy(id = editPhone!!.id))
                adminLogRepository.logAction("Updated Phone", "Updated details for ${phoneToSave.brand} ${phoneToSave.model}")
                activity?.findViewById<View>(android.R.id.content)?.let {
                    Snackbar.make(it, "Phone updated", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                phoneRepository.addPhone(phoneToSave)
                adminLogRepository.logAction("Added Phone", "Added new phone: ${phoneToSave.brand} ${phoneToSave.model}")
                activity?.findViewById<View>(android.R.id.content)?.let {
                    Snackbar.make(it, "Phone added", Snackbar.LENGTH_SHORT).show()
                }
            }
            setLoadingState(false)
            dismiss()
        }
    }

    private fun buildPhoneFromForm(): Phone? {
        val brand    = binding.etBrand.text.toString().trim()
        val model    = binding.etModel.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()

        // Clear previous errors
        binding.tilBrand?.error = null
        binding.tilModel?.error = null
        (binding.etPrice.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = null
        (binding.actvCategory.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = null

        if (brand.isEmpty()) {
            binding.tilBrand?.error = "Brand is required"; binding.etBrand.requestFocus(); return null
        }
        if (model.isEmpty()) {
            binding.tilModel?.error = "Model is required"; binding.etModel.requestFocus(); return null
        }
        if (priceStr.isEmpty()) {
            (binding.etPrice.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Price is required"
            binding.etPrice.requestFocus(); return null
        }
        if (category.isEmpty()) {
            (binding.actvCategory.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Category is required"
            binding.actvCategory.requestFocus(); return null
        }

        val stockStr = binding.etStock.text.toString().trim()
        val ram = binding.etRam.text.toString().trim()
        val storage = binding.etStorage.text.toString().trim()
        val camera = binding.etCamera.text.toString().trim()
        val battery = binding.etBattery.text.toString().trim()
        val chipset = binding.etChipset.text.toString().trim()
        val display = binding.etDisplay.text.toString().trim()

        if (stockStr.isEmpty()) {
            (binding.etStock.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Required"
            binding.etStock.requestFocus(); return null
        }
        if (ram.isEmpty()) {
            (binding.etRam.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Required"
            binding.etRam.requestFocus(); return null
        }
        if (storage.isEmpty()) {
            (binding.etStorage.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Required"
            binding.etStorage.requestFocus(); return null
        }
        if (camera.isEmpty()) {
            (binding.etCamera.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Required"
            binding.etCamera.requestFocus(); return null
        }
        if (battery.isEmpty()) {
            (binding.etBattery.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Required"
            binding.etBattery.requestFocus(); return null
        }
        if (chipset.isEmpty()) {
            (binding.etChipset.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Required"
            binding.etChipset.requestFocus(); return null
        }
        if (display.isEmpty()) {
            (binding.etDisplay.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Required"
            binding.etDisplay.requestFocus(); return null
        }

        return Phone(
            brand = brand, model = model,
            price = priceStr.toDoubleOrNull() ?: 0.0,
            stock = stockStr.toIntOrNull() ?: 0,
            category = category,
            ram = ram,
            storage = storage,
            camera = camera,
            battery = battery,
            chipset = chipset,
            display = display,
            imageUrl = existingImageUrl,
            isBestValue = binding.switchBestValue.isChecked
        )
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnSave.isEnabled = !loading
        binding.btnSave.text = if (loading) "Saving…" else "Save Phone"
        binding.progressSave.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}