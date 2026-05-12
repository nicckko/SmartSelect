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

    // Callback to refresh profile after save
    private var onProfileSavedListener: (() -> Unit)? = null

    fun setOnProfileSavedListener(listener: () -> Unit) {
        onProfileSavedListener = listener
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

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            selectedImageUri = uri
            Glide.with(requireContext()).load(uri).centerCrop().into(binding.ivProfilePic)
            binding.tvAvatarInitial.visibility = View.GONE
            binding.ivProfilePic.visibility = View.VISIBLE
            binding.tvImageHint.text = "Image selected ✓"
            binding.tvImageHint.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
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
            binding.tvImageHint.text = "Current image (tap to change)"
        } else {
            binding.tvAvatarInitial.visibility = View.VISIBLE
            binding.tvAvatarInitial.text = initialName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            binding.ivProfilePic.visibility = View.GONE
            binding.tvImageHint.text = "No image (tap to add)"
        }

        binding.btnPickImage.setOnClickListener { imagePickerLauncher.launch("image/*") }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { saveProfile() }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = "Name cannot be empty"
            return
        }
        if (username.isEmpty()) {
            binding.tilUsername.error = "Username cannot be empty"
            return
        }
        if (password.isNotEmpty() && password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return
        }

        lifecycleScope.launch {
            setLoadingState(true)

            // Upload image to Cloudinary if selected
            val finalImageUrl = if (selectedImageUri != null) {
                try {
                    val url = CloudinaryHelper.uploadProfileImage(selectedImageUri!!)
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

            // Update profile
            authViewModel.updateProfile(name, username, password.ifEmpty { null }, finalImageUrl)

            // Wait for update to complete
            var attempts = 0
            var updateComplete = false
            while (attempts < 20 && !updateComplete) {
                val state = authViewModel.authState.value
                if (state is Resource.Success) {
                    updateComplete = true
                    Log.d(TAG, "Profile update successful")
                } else if (state is Resource.Error) {
                    setLoadingState(false)
                    Snackbar.make(binding.root, "Error: ${state.message}", Snackbar.LENGTH_SHORT).show()
                    return@launch
                }
                kotlinx.coroutines.delay(100)
                attempts++
            }

            setLoadingState(false)

            if (updateComplete) {
                // Trigger refresh callback before dismissing
                onProfileSavedListener?.invoke()
                Snackbar.make(binding.root, "Profile updated successfully!", Snackbar.LENGTH_SHORT).show()
                kotlinx.coroutines.delay(300)
                dismiss()
            } else {
                Snackbar.make(binding.root, "Profile updated!", Snackbar.LENGTH_SHORT).show()
                onProfileSavedListener?.invoke()
                dismiss()
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnSave.isEnabled = !loading
        binding.btnSave.text = if (loading) "Saving…" else "Save"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}