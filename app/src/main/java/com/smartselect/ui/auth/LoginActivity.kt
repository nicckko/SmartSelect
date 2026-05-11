package com.smartselect.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
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

    // Store the last registered credentials for auto-fill
    private var lastRegisteredEmail: String = ""
    private var lastRegisteredUsername: String = ""

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

                // Show/hide registration fields
                binding.layoutRegisterFirstName.isVisible = isRegister
                binding.layoutRegisterLastName.isVisible = isRegister
                binding.layoutRegisterUsername.isVisible = isRegister
                binding.layoutConfirmPassword.isVisible = isRegister

                // Change email field hint and input type based on mode
                val emailLayout = binding.layoutEmail
                val emailInput = binding.etEmailOrUsername

                if (isRegister) {
                    emailLayout.hint = "Email Address"
                    emailInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    emailInput.hint = "Email Address"
                    binding.btnAction.text = "Register"
                    // Clear password fields when switching to register
                    binding.etPassword.text?.clear()
                    binding.etConfirmPassword.text?.clear()
                    // Clear email field
                    emailInput.text?.clear()
                } else {
                    emailLayout.hint = "Email or Username"
                    emailInput.inputType = android.text.InputType.TYPE_CLASS_TEXT
                    emailInput.hint = "Email or Username"
                    binding.btnAction.text = "Sign In"

                    // Auto-fill login fields if coming from registration (delay to ensure UI is ready)
                    if (lastRegisteredEmail.isNotEmpty() || lastRegisteredUsername.isNotEmpty()) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            val loginHint = if (lastRegisteredUsername.isNotEmpty()) {
                                lastRegisteredUsername
                            } else {
                                lastRegisteredEmail
                            }
                            emailInput.setText(loginHint)
                            // Don't clear the stored values until after they've been used
                        }, 100)
                    }

                    // Clear confirm password field when switching to login
                    binding.etConfirmPassword.text?.clear()
                }

                // Clear errors when switching tabs
                binding.tvError.isVisible = false
                clearErrors()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    private fun setupActions() {
        binding.btnAction.setOnClickListener {
            val identifier = binding.etEmailOrUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val isRegister = binding.tabLayout.selectedTabPosition == 1

            if (identifier.isEmpty() || password.isEmpty()) {
                showError("Please fill in all fields")
                return@setOnClickListener
            }

            if (isRegister) {
                // Registration validation
                val firstName = binding.etFirstName.text.toString().trim()
                val lastName = binding.etLastName.text.toString().trim()
                val username = binding.etUsername.text.toString().trim()
                val email = identifier // identifier is email in registration mode
                val confirmPassword = binding.etConfirmPassword.text.toString().trim()

                if (firstName.isEmpty()) {
                    showError("Please enter your first name")
                    return@setOnClickListener
                }
                if (lastName.isEmpty()) {
                    showError("Please enter your last name")
                    return@setOnClickListener
                }
                if (username.isEmpty()) {
                    showError("Please choose a username")
                    return@setOnClickListener
                }
                if (username.length < 3) {
                    showError("Username must be at least 3 characters")
                    return@setOnClickListener
                }
                if (!isValidEmail(email)) {
                    showError("Please enter a valid email address")
                    return@setOnClickListener
                }
                if (password.length < 6) {
                    showError("Password must be at least 6 characters")
                    return@setOnClickListener
                }
                if (password != confirmPassword) {
                    showError("Passwords do not match")
                    return@setOnClickListener
                }

                // Store the email and username for auto-fill after registration
                lastRegisteredEmail = email
                lastRegisteredUsername = username

                authViewModel.register(firstName, lastName, username, email, password)
            } else {
                // Login validation
                if (password.length < 6) {
                    showError("Password must be at least 6 characters")
                    return@setOnClickListener
                }
                authViewModel.login(identifier, password)
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun clearErrors() {
        binding.etFirstName.error = null
        binding.etLastName.error = null
        binding.etUsername.error = null
        binding.etEmailOrUsername.error = null
        binding.etPassword.error = null
        binding.etConfirmPassword.error = null
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

                        // Show success message
                        val loginHint = if (lastRegisteredUsername.isNotEmpty()) {
                            "username: $lastRegisteredUsername"
                        } else {
                            "email: $lastRegisteredEmail"
                        }
                        showSuccess("Registration successful! You can now login with your $loginHint")

                        // Clear registration fields first
                        clearRegistrationFields()

                        // Switch back to Login Tab (this will trigger auto-fill)
                        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))

                        // Reset register state
                        authViewModel.resetRegisterState()
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnAction.isEnabled = true
                        showError(resource.message ?: "Registration failed")
                    }
                    null -> { }
                }
            }
        }
    }

    private fun clearRegistrationFields() {
        binding.etFirstName.text?.clear()
        binding.etLastName.text?.clear()
        binding.etUsername.text?.clear()
        binding.etEmailOrUsername.text?.clear()
        binding.etPassword.text?.clear()
        binding.etConfirmPassword.text?.clear()
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