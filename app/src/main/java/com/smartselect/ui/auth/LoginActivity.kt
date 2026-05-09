package com.smartselect.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartselect.databinding.ActivityLoginBinding
import com.smartselect.ui.MainActivity
import com.smartselect.utils.Resource
import com.smartselect.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (authViewModel.isLoggedIn) {
            navigateToMain()
            return
        }

        setupTabs()
        setupActions()
        observeAuth()
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Login"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Register"))

        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                val isRegister = tab.position == 1
                binding.layoutRegisterName.isVisible = isRegister
                binding.btnAction.text = if (isRegister) "Register" else "Sign In"
                binding.tvError.isVisible = false
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    private fun setupActions() {
        binding.btnAction.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val isRegister = binding.tabLayout.selectedTabPosition == 1

            if (email.isEmpty() || password.isEmpty()) {
                showError("Please fill in all fields")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showError("Password must be at least 6 characters")
                return@setOnClickListener
            }

            if (isRegister) {
                val name = binding.etName.text.toString().trim()
                if (name.isEmpty()) { showError("Please enter your name"); return@setOnClickListener }
                authViewModel.register(name, email, password)
            } else {
                authViewModel.login(email, password)
            }
        }

    }
    

    private fun observeAuth() {
        // Observe Login State
        lifecycleScope.launch {
            authViewModel.authState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnAction.isEnabled = false
                        binding.tvError.isVisible = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        navigateToMain()
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnAction.isEnabled = true
                        showError(resource.message ?: "Login failed")
                    }
                    null -> {
                        binding.progressBar.isVisible = false
                        binding.btnAction.isEnabled = true
                    }
                }
            }
        }

        // Observe Register State
        lifecycleScope.launch {
            authViewModel.registerState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnAction.isEnabled = false
                        binding.tvError.isVisible = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnAction.isEnabled = true
                        
                        // Switch back to Login Tab
                        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
                        
                        // Show success message
                        showSuccess("Registration successful. Please login.")
                        
                        // Reset the register state so it doesn't trigger again on rotation
                        authViewModel.resetRegisterState()
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnAction.isEnabled = true
                        showError(resource.message ?: "Registration failed")
                    }
                    null -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvError.isVisible = true
        binding.tvError.text = msg
        binding.tvError.setTextColor(getColor(com.smartselect.R.color.error))
        binding.tvError.setBackgroundColor(getColor(com.smartselect.R.color.error_light))
    }

    private fun showSuccess(msg: String) {
        binding.tvError.isVisible = true
        binding.tvError.text = msg
        binding.tvError.setTextColor(getColor(com.smartselect.R.color.success))
        binding.tvError.setBackgroundColor(getColor(com.smartselect.R.color.success_light))
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}