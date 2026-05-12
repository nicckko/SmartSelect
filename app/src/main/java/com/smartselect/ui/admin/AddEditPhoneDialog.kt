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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartselect.R
import com.smartselect.data.PhoneSpecsData
import com.smartselect.data.model.Phone
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.data.repository.AdminLogRepository
import com.smartselect.databinding.DialogAddEditPhoneBinding
import com.smartselect.utils.CloudinaryHelper
import com.smartselect.utils.Resource
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

            // If we're adding a new phone (not editing), check for duplicate
            if (editPhone == null) {
                val existingPhone = phoneRepository.findDuplicatePhone(phone.brand, phone.model)
                if (existingPhone != null) {
                    // Duplicate found — show quantity update dialog instead of blocking
                    setLoadingState(false)
                    showDuplicateStockDialog(existingPhone)
                    return@launch
                }
            } else {
                // Editing: check for duplicate with other phones (not self)
                val isDuplicate = phoneRepository.isDuplicate(phone.brand, phone.model, editPhone?.id)
                if (isDuplicate) {
                    setLoadingState(false)
                    Snackbar.make(binding.root, "Error: Another phone with same brand/model exists", Snackbar.LENGTH_LONG).show()
                    return@launch
                }
            }

            // Upload image to Cloudinary (replaces Firebase Storage)
            val finalImageUrl = if (selectedImageUri != null) {
                try {
                    val url = CloudinaryHelper.uploadPhoneImage(selectedImageUri!!)
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

    /**
     * Shows a dialog when a duplicate phone (same brand + model) is detected.
     * Instead of blocking, allows the admin to increase/decrease the quantity to add.
     */
    private fun showDuplicateStockDialog(existingPhone: Phone) {
        val ctx = context ?: return
        var quantityToAdd = 1

        // Build custom layout
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 48, 64, 16)
        }

        // Info text
        val infoText = TextView(ctx).apply {
            text = "${existingPhone.brand} ${existingPhone.model} already exists.\nCurrent stock: ${existingPhone.stock}\n\nHow many units to add?"
            textSize = 14f
            setTextColor(ctx.getColor(R.color.text_primary))
        }
        container.addView(infoText)

        // Spacer
        val spacer = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 32
            )
        }
        container.addView(spacer)

        // Quantity row: [ - ] qty [ + ]
        val qtyRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val qtyText = TextView(ctx).apply {
            text = quantityToAdd.toString()
            textSize = 24f
            setTextColor(ctx.getColor(R.color.text_primary))
            gravity = android.view.Gravity.CENTER
            minWidth = 120
        }

        val btnDecrease = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "−"
            textSize = 18f
            minWidth = 0
            minimumWidth = 0
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val btnIncrease = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "+"
            textSize = 18f
            minWidth = 0
            minimumWidth = 0
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        btnDecrease.setOnClickListener {
            if (quantityToAdd > 1) {
                quantityToAdd--
                qtyText.text = quantityToAdd.toString()
            }
        }

        btnIncrease.setOnClickListener {
            quantityToAdd++
            qtyText.text = quantityToAdd.toString()
        }

        qtyRow.addView(btnDecrease)
        qtyRow.addView(qtyText)
        qtyRow.addView(btnIncrease)
        container.addView(qtyRow)

        // Preview of new total
        val previewText = TextView(ctx).apply {
            text = "New total stock: ${existingPhone.stock + quantityToAdd}"
            textSize = 12f
            setTextColor(ctx.getColor(R.color.text_secondary))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }
        container.addView(previewText)

        // Update preview when quantity changes
        btnDecrease.setOnClickListener {
            if (quantityToAdd > 1) {
                quantityToAdd--
                qtyText.text = quantityToAdd.toString()
                previewText.text = "New total stock: ${existingPhone.stock + quantityToAdd}"
            }
        }

        btnIncrease.setOnClickListener {
            quantityToAdd++
            qtyText.text = quantityToAdd.toString()
            previewText.text = "New total stock: ${existingPhone.stock + quantityToAdd}"
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle("📦 Phone Already Exists")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Update Stock") { _, _ ->
                lifecycleScope.launch {
                    setLoadingState(true)
                    val newStock = existingPhone.stock + quantityToAdd
                    val result = phoneRepository.updatePhoneStock(existingPhone.id, newStock)
                    if (result is Resource.Success) {
                        adminLogRepository.logAction(
                            "Updated Stock",
                            "Added $quantityToAdd units to ${existingPhone.brand} ${existingPhone.model} (${existingPhone.stock} → $newStock)"
                        )
                        activity?.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Stock updated: +$quantityToAdd units for ${existingPhone.model}", Snackbar.LENGTH_SHORT).show()
                        }
                    } else {
                        activity?.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Failed to update stock", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    setLoadingState(false)
                    dismiss()
                }
            }
            .show()
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
        (binding.etStock.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = null

        if (brand.isEmpty()) {
            binding.tilBrand?.error = "Brand is required"; binding.etBrand.requestFocus(); return null
        }
        if (model.isEmpty()) {
            binding.tilModel?.error = "Model is required"; binding.etModel.requestFocus(); return null
        }

        // Price validation
        if (priceStr.isEmpty()) {
            (binding.etPrice.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Price is required"
            binding.etPrice.requestFocus(); return null
        }
        val price = priceStr.toDoubleOrNull()
        if (price == null) {
            (binding.etPrice.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Enter a valid number"
            binding.etPrice.requestFocus(); return null
        }
        if (price <= 0) {
            (binding.etPrice.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Price must be greater than 0"
            binding.etPrice.requestFocus(); return null
        }
        if (price > 999999) {
            (binding.etPrice.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Price exceeds maximum (₱999,999)"
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

        // Stock validation
        if (stockStr.isEmpty()) {
            (binding.etStock.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Stock is required"
            binding.etStock.requestFocus(); return null
        }
        val stock = stockStr.toIntOrNull()
        if (stock == null) {
            (binding.etStock.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Enter a valid whole number"
            binding.etStock.requestFocus(); return null
        }
        if (stock < 0) {
            (binding.etStock.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Stock cannot be negative"
            binding.etStock.requestFocus(); return null
        }
        if (stock > 9999) {
            (binding.etStock.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Stock exceeds maximum (9,999)"
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
            price = price,
            stock = stock,
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