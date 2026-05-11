package com.smartselect.ui.profile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartselect.databinding.DialogEditProfileBinding
import com.smartselect.viewmodel.AuthViewModel
import com.smartselect.utils.CloudinaryHelper
import com.smartselect.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileDialog : BottomSheetDialogFragment() {

    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String = ""
    private var isUploadingImage = false

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            selectedImageUri = uri
            binding.tvAvatarInitial.visibility = View.GONE
            binding.ivProfilePic.visibility = View.VISIBLE
            Glide.with(requireContext()).load(uri).centerCrop().into(binding.ivProfilePic)
        }

    companion object {
        private const val TAG = "EditProfileDialog"
        fun newInstance(currentName: String, currentUsername: String, currentImageUrl: String) = EditProfileDialog().apply {
            arguments = Bundle().apply {
                putString("name", currentName)
                putString("username", currentUsername)
                putString("imageUrl", currentImageUrl)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val initialName = arguments?.getString("name") ?: ""
        val initialUsername = arguments?.getString("username") ?: ""

        binding.etName.setText(initialName)
        binding.etUsername.setText(initialUsername)

        existingImageUrl = arguments?.getString("imageUrl") ?: ""

        if (existingImageUrl.isNotEmpty()) {
            binding.tvAvatarInitial.visibility = View.GONE
            binding.ivProfilePic.visibility = View.VISIBLE
            Glide.with(requireContext()).load(existingImageUrl).centerCrop().into(binding.ivProfilePic)
        } else {
            binding.tvAvatarInitial.visibility = View.VISIBLE
            binding.tvAvatarInitial.text = initialName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            binding.ivProfilePic.visibility = View.GONE
        }

        binding.btnPickImage.setOnClickListener {
            if (!isUploadingImage) {
                imagePickerLauncher.launch("image/*")
            }
        }

        binding.btnCancel.setOnClickListener {
            if (!isUploadingImage) {
                dismiss()
            }
        }

        binding.btnSave.setOnClickListener {
            if (isUploadingImage) {
                Snackbar.make(binding.root, "Please wait, image is uploading...", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = binding.etName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validate name
            if (name.isEmpty()) {
                binding.tilName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            binding.tilName.error = null

            // Validate username
            if (username.isEmpty()) {
                binding.tilUsername.error = "Username cannot be empty"
                return@setOnClickListener
            }
            binding.tilUsername.error = null

            // Validate password (if provided)
            if (password.isNotEmpty() && password.length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            binding.tilPassword.error = null

            // Show confirmation dialog before saving
            showSaveConfirmation(name, username, password)
        }
    }

    private fun showSaveConfirmation(name: String, username: String, password: String) {
        val hasImageChange = selectedImageUri != null
        val imageMessage = if (hasImageChange) "\n\n📷 Profile picture will be updated." else ""

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Save Changes")
            .setMessage("Update your profile information?$imageMessage")
            .setPositiveButton("Save") { _, _ ->
                saveProfile(name, username, password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveProfile(name: String, username: String, password: String) {
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."
        isUploadingImage = true

        lifecycleScope.launch {
            // Upload image to Cloudinary if a new one was selected
            val finalImageUrl = if (selectedImageUri != null) {
                try {
                    val url = CloudinaryHelper.uploadImage(selectedImageUri!!)
                    Log.d(TAG, "Profile image uploaded to Cloudinary: $url")
                    url
                } catch (e: Exception) {
                    Log.e(TAG, "Cloudinary upload failed for profile", e)
                    showError("Image upload failed. Using existing image.")
                    existingImageUrl
                }
            } else {
                existingImageUrl
            }

            isUploadingImage = false

            authViewModel.updateProfile(name, username, password.ifEmpty { null }, finalImageUrl)

            authViewModel.authState.collect { state ->
                if (!isAdded) return@collect

                when (state) {
                    is Resource.Success -> {
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Save"
                        Snackbar.make(binding.root, "Profile updated successfully!", Snackbar.LENGTH_LONG).show()
                        dismiss()
                    }
                    is Resource.Error -> {
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Save"
                        showError(state.message ?: "Failed to update profile")
                    }
                    else -> { }
                }
            }
        }
    }

    private fun showError(message: String) {
        if (_binding != null) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}