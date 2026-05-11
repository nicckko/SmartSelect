package com.smartselect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartselect.data.model.User
import com.smartselect.data.repository.AuthRepository
import com.smartselect.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<User>?>(null)
    val authState: StateFlow<Resource<User>?> = _authState.asStateFlow()

    private val _registerState = MutableStateFlow<Resource<User>?>(null)
    val registerState: StateFlow<Resource<User>?> = _registerState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _adminSetupState = MutableStateFlow<Resource<User>?>(null)
    val adminSetupState: StateFlow<Resource<User>?> = _adminSetupState.asStateFlow()

    val isLoggedIn get() = authRepository.getCurrentUser() != null

    init {
        if (isLoggedIn) loadCurrentUser()
    }

    fun login(identifier: String, password: String) = viewModelScope.launch {
        _authState.value = Resource.Loading()
        _authState.value = authRepository.login(identifier, password)
        if (_authState.value is Resource.Success) loadCurrentUser()
    }

    fun register(name: String, email: String, password: String) = viewModelScope.launch {
        _registerState.value = Resource.Loading()
        _registerState.value = authRepository.register(name, email, password)
        // Notice we DO NOT load currentUser here because the user is automatically signed out in the repository
    }
    
    fun resetRegisterState() {
        _registerState.value = null
    }

    /**
     * One-time setup: creates admin@smartselect.com with password SmartAdmin2024!
     * Call this once from the Login screen to bootstrap the admin account.
     */
    fun setupAdminAccount() = viewModelScope.launch {
        _adminSetupState.value = Resource.Loading()
        _adminSetupState.value = authRepository.createAdminAccount()
    }

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
        _authState.value = null
    }

    fun updateProfile(newName: String, newUsername: String, newPassword: String?, newProfileUrl: String?) = viewModelScope.launch {
        _authState.value = Resource.Loading()
        val result = authRepository.updateProfile(newName, newUsername, newPassword, newProfileUrl)
        _authState.value = result
        if (result is Resource.Success) {
            _currentUser.value = result.data
        }
    }

    fun register(firstName: String, lastName: String, username: String, email: String, password: String) = viewModelScope.launch {
        _registerState.value = Resource.Loading()
        _registerState.value = authRepository.register(firstName, lastName, username, email, password)
    }

    private fun loadCurrentUser() = viewModelScope.launch {
        val result = authRepository.getCurrentUserData()
        if (result is Resource.Success) _currentUser.value = result.data
    }

    fun isAdmin() = _currentUser.value?.role == "admin"
}