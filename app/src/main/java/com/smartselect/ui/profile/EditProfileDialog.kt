package com.smartselect.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartselect.databinding.DialogEditProfileBinding
import com.smartselect.viewmodel.AuthViewModel
import com.smartselect.utils.Resource
import kotlinx.coroutines.launch

class EditProfileDialog : BottomSheetDialogFragment() {

    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    companion object {
        fun newInstance(currentName: String) = EditProfileDialog().apply {
            arguments = Bundle().apply {
                putString("name", currentName)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etName.setText(arguments?.getString("name") ?: "")

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.tilName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            binding.tilName.error = null

            binding.btnSave.isEnabled = false
            binding.btnSave.text = "Saving..."

            authViewModel.updateProfile(name)
            
            lifecycleScope.launch {
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
