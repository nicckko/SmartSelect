package com.smartselect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartselect.data.model.CartItem
import com.smartselect.data.model.Phone
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneViewModel @Inject constructor(
    private val phoneRepository: PhoneRepository
) : ViewModel() {

    private val _phones = MutableStateFlow<Resource<List<Phone>>>(Resource.Loading())
    val phones: StateFlow<Resource<List<Phone>>> = _phones.asStateFlow()

    private val _compareList = MutableStateFlow<List<Phone>>(emptyList())
    val compareList: StateFlow<List<Phone>> = _compareList.asStateFlow()

    private val _favorites = MutableStateFlow<List<Phone>>(emptyList())
    val favorites: StateFlow<List<Phone>> = _favorites.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _selectedPhone = MutableStateFlow<Phone?>(null)
    val selectedPhone: StateFlow<Phone?> = _selectedPhone.asStateFlow()

    init {
        loadPhones()
    }

    fun loadPhones() = viewModelScope.launch {
        phoneRepository.getPhones().collect { _phones.value = it }
    }

    fun searchPhones(
        query: String,
        category: String = "",
        minPrice: Double = 0.0,
        maxPrice: Double = Double.MAX_VALUE
    ) = viewModelScope.launch {
        phoneRepository.searchPhones(query, category, minPrice, maxPrice).collect {
            _phones.value = it
        }
    }

    fun setSelectedPhone(phone: Phone) {
        _selectedPhone.value = phone
    }

    // Compare
    fun addToCompare(phone: Phone): Boolean {
        val current = _compareList.value
        if (current.any { it.id == phone.id }) return false
        if (current.size >= 3) return false // max 3 for compare
        _compareList.value = current + phone
        return true
    }

    fun removeFromCompare(phone: Phone) {
        _compareList.value = _compareList.value.filter { it.id != phone.id }
    }

    fun clearCompare() {
        _compareList.value = emptyList()
    }

    // NEW: Check if a phone is in the compare list
    fun isInCompare(phoneId: String): Boolean {
        return _compareList.value.any { it.id == phoneId }
    }

    // Favorites
    fun toggleFavorite(phone: Phone) {
        val current = _favorites.value
        _favorites.value = if (current.any { it.id == phone.id }) {
            current.filter { it.id != phone.id }
        } else {
            current + phone
        }
    }

    fun isFavorite(phoneId: String) = _favorites.value.any { it.id == phoneId }

    // Cart - Fixed: create new CartItem instead of mutating existing one
    fun addToCart(phone: Phone) {
        val current = _cart.value
        val existingIndex = current.indexOfFirst { it.phone.id == phone.id }
        _cart.value = if (existingIndex >= 0) {
            current.toMutableList().also {
                it[existingIndex] = it[existingIndex].copy(quantity = it[existingIndex].quantity + 1)
            }
        } else {
            current + CartItem(phone)
        }
    }

    fun removeFromCart(phoneId: String) {
        _cart.value = _cart.value.filter { it.phone.id != phoneId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun getCartTotal() = _cart.value.sumOf { it.phone.price * it.quantity }
}