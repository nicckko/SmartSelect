package com.smartselect.ui.admin

import android.net.Uri
import android.os.Bundle
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
import com.google.firebase.storage.FirebaseStorage
import com.smartselect.R
import com.smartselect.data.PhoneSpecsData
import com.smartselect.data.model.Phone
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.data.repository.AdminLogRepository
import com.smartselect.databinding.DialogAddEditPhoneBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class AddEditPhoneDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddEditPhoneBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var phoneRepository: PhoneRepository
    @Inject lateinit var adminLogRepository: AdminLogRepository
    @Inject lateinit var storage: FirebaseStorage

    private var editPhone: Phone? = null
    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String = ""

    companion object {
        private const val ARG_PHONE = "phone"
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
        if (phone.imageUrl.isNotEmpty()) {
            binding.ivPhonePreview.visibility = View.VISIBLE
            binding.tvImageHint.text = "Current image (tap to change)"
            Glide.with(requireContext()).load(phone.imageUrl).centerCrop().into(binding.ivPhonePreview)
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
            val finalImageUrl = if (selectedImageUri != null) uploadImageToStorage(selectedImageUri!!) else existingImageUrl
            val phoneToSave = phone.copy(imageUrl = finalImageUrl)
            if (editPhone != null) {
                phoneRepository.updatePhone(phoneToSave.copy(id = editPhone!!.id))
                adminLogRepository.logAction("Updated Phone", "Updated details for ${phoneToSave.brand} ${phoneToSave.model}")
                Snackbar.make(requireActivity().findViewById(android.R.id.content), "Phone updated", Snackbar.LENGTH_SHORT).show()
            } else {
                phoneRepository.addPhone(phoneToSave)
                adminLogRepository.logAction("Added Phone", "Added new phone: ${phoneToSave.brand} ${phoneToSave.model}")
                Snackbar.make(requireActivity().findViewById(android.R.id.content), "Phone added", Snackbar.LENGTH_SHORT).show()
            }
            setLoadingState(false)
            dismiss()
        }
    }

    private suspend fun uploadImageToStorage(uri: Uri): String {
        return try {
            val filename = "phones/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(filename)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Image upload failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
            existingImageUrl
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

        return Phone(
            brand = brand, model = model,
            price = priceStr.toDoubleOrNull() ?: 0.0,
            stock = binding.etStock.text.toString().toIntOrNull() ?: 10,
            category = category,
            ram = binding.etRam.text.toString().trim(),
            storage = binding.etStorage.text.toString().trim(),
            camera = binding.etCamera.text.toString().trim(),
            battery = binding.etBattery.text.toString().trim(),
            chipset = binding.etChipset.text.toString().trim(),
            display = binding.etDisplay.text.toString().trim(),
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