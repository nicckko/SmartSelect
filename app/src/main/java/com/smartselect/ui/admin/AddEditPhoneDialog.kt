package com.smartselect.ui.admin

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import com.smartselect.R
import com.smartselect.data.model.Phone
import com.smartselect.data.repository.PhoneRepository
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
    @Inject lateinit var storage: FirebaseStorage

    private var editPhone: Phone? = null
    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String = ""

    // ── Category options ─────────────────────────────────────────────────────
    private val categories = listOf(
        "iPhone",
        "Android",
        "Flagship",
        "Mid-range",
        "Budget"
    )

    companion object {
        private const val ARG_PHONE = "phone"
        fun newInstance(phone: Phone) = AddEditPhoneDialog().apply {
            arguments = Bundle().apply { putParcelable(ARG_PHONE, phone) }
        }
    }

    // ── Image picker launcher ────────────────────────────────────────────────
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            selectedImageUri = uri

            // Show preview immediately
            Glide.with(requireContext())
                .load(uri)
                .centerCrop()
                .into(binding.ivPhonePreview)

            binding.ivPhonePreview.visibility = View.VISIBLE
            binding.tvImageHint.text = "Image selected ✓"
            binding.tvImageHint.setTextColor(
                requireContext().getColor(R.color.success)
            )
        }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        editPhone = arguments?.getParcelable(ARG_PHONE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditPhoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryDropdown()
        setupImagePicker()
        populateFormIfEditing()
        setupButtons()
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.actvCategory.setAdapter(adapter)
        binding.actvCategory.setOnClickListener { binding.actvCategory.showDropDown() }
        binding.actvCategory.threshold = 0
    }

    private fun setupImagePicker() {
        binding.btnPickImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun populateFormIfEditing() {
        val phone = editPhone ?: return
        binding.tvDialogTitle.text = "Edit Phone"
        binding.tvDialogSubtitle.text = "Update the phone specifications"

        binding.etBrand.setText(phone.brand)
        binding.etModel.setText(phone.model)
        binding.etPrice.setText(phone.price.toLong().toString())
        binding.etStock.setText(phone.stock.toString())
        binding.actvCategory.setText(phone.category, false)
        binding.etRam.setText(phone.ram)
        binding.etStorage.setText(phone.storage)
        binding.etCamera.setText(phone.camera)
        binding.etBattery.setText(phone.battery)
        binding.etChipset.setText(phone.chipset)
        binding.etDisplay.setText(phone.display)
        binding.switchBestValue.isChecked = phone.isBestValue

        // Load existing image
        existingImageUrl = phone.imageUrl
        if (phone.imageUrl.isNotEmpty()) {
            binding.ivPhonePreview.visibility = View.VISIBLE
            binding.tvImageHint.text = "Current image (tap to change)"
            Glide.with(requireContext())
                .load(phone.imageUrl)
                .centerCrop()
                .into(binding.ivPhonePreview)
        }
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { savePhone() }
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    private fun savePhone() {
        val phone = buildPhoneFromForm() ?: return

        lifecycleScope.launch {
            setLoadingState(true)

            val finalImageUrl = when {
                selectedImageUri != null -> uploadImageToStorage(selectedImageUri!!)
                else -> existingImageUrl
            }

            val phoneToSave = phone.copy(imageUrl = finalImageUrl)

            if (editPhone != null) {
                phoneRepository.updatePhone(phoneToSave.copy(id = editPhone!!.id))
            } else {
                phoneRepository.addPhone(phoneToSave)
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
        (binding.etBrand.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = null
        (binding.etModel.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = null
        (binding.etPrice.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = null
        (binding.actvCategory.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = null

        if (brand.isEmpty()) { 
            (binding.etBrand.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Brand is required"
            binding.etBrand.requestFocus()
            return null 
        }
        if (model.isEmpty()) { 
            (binding.etModel.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Model is required"
            binding.etModel.requestFocus()
            return null 
        }
        if (priceStr.isEmpty()) { 
            (binding.etPrice.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Price is required"
            binding.etPrice.requestFocus()
            return null 
        }
        if (category.isEmpty()) { 
            (binding.actvCategory.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Category is required"
            binding.actvCategory.requestFocus()
            return null 
        }

        return Phone(
            brand    = brand,
            model    = model,
            price    = priceStr.toDoubleOrNull() ?: 0.0,
            stock    = binding.etStock.text.toString().toIntOrNull() ?: 10,
            category = category,
            ram      = binding.etRam.text.toString().trim(),
            storage  = binding.etStorage.text.toString().trim(),
            camera   = binding.etCamera.text.toString().trim(),
            battery  = binding.etBattery.text.toString().trim(),
            chipset  = binding.etChipset.text.toString().trim(),
            display  = binding.etDisplay.text.toString().trim(),
            imageUrl = existingImageUrl,
            isBestValue = binding.switchBestValue.isChecked
        )
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnSave.isEnabled = !loading
        binding.btnSave.text = if (loading) "Saving…" else "Save Phone"
        binding.progressSave.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}