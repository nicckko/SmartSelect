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
import com.smartselect.databinding.DialogEditProfileBinding
import com.smartselect.viewmodel.AuthViewModel
import com.smartselect.utils.CloudinaryHelper
import com.smartselect.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileDialog : BottomSheetDialogFragment() {

    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String = ""

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            selectedImageUri = uri
            binding.tvAvatarInitial.visibility = View.GONE
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
        binding.etName.setText(initialName)
        binding.etUsername.setText(arguments?.getString("username") ?: "")

        existingImageUrl = arguments?.getString("imageUrl") ?: ""
        if (existingImageUrl.isNotEmpty()) {
            binding.tvAvatarInitial.visibility = View.GONE
            Glide.with(requireContext()).load(existingImageUrl).centerCrop().into(binding.ivProfilePic)
        } else {
            binding.tvAvatarInitial.text = initialName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        }

        binding.btnPickImage.setOnClickListener { imagePickerLauncher.launch("image/*") }

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (name.isEmpty()) {
                binding.tilName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            binding.tilName.error = null

            binding.btnSave.isEnabled = false
            binding.btnSave.text = "Saving..."

            lifecycleScope.launch {
                // Upload image to Cloudinary if a new one was selected
                val finalImageUrl = if (selectedImageUri != null) {
                    try {
                        val url = CloudinaryHelper.uploadImage(selectedImageUri!!)
                        Log.d(TAG, "Profile image uploaded to Cloudinary: $url")
                        url
                    } catch (e: Exception) {
                        Log.e(TAG, "Cloudinary upload failed for profile", e)
                        existingImageUrl
                    }
                } else {
                    existingImageUrl
                }

                authViewModel.updateProfile(name, username, password.ifEmpty { null }, finalImageUrl)

                authViewModel.authState.collect { state ->
                    if (state is Resource.Success) {
                        dismiss()
                    } else if (state is Resource.Error) {
                        binding.tilName.error = state.message
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Save"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}