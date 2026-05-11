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

    // Store current filter values to persist across UI events
    private val _currentSearchQuery = MutableStateFlow("")
    val currentSearchQuery: StateFlow<String> = _currentSearchQuery.asStateFlow()

    private val _currentCategory = MutableStateFlow("")
    val currentCategory: StateFlow<String> = _currentCategory.asStateFlow()

    private val _currentMinPrice = MutableStateFlow(0.0)
    val currentMinPrice: StateFlow<Double> = _currentMinPrice.asStateFlow()

    private val _currentMaxPrice = MutableStateFlow(Double.MAX_VALUE)
    val currentMaxPrice: StateFlow<Double> = _currentMaxPrice.asStateFlow()

    private val _stockError = MutableStateFlow<String?>(null)
    val stockError: StateFlow<String?> = _stockError.asStateFlow()

    init {
        loadPhones()
    }

    fun loadPhones() = viewModelScope.launch {
        phoneRepository.getPhones().collect { _phones.value = it }
    }

    fun searchPhones(query: String, category: String = "") {
        _currentSearchQuery.value = query
        _currentCategory.value = category
        applyCurrentFilters()
    }

    fun applyFilters(query: String, category: String, minPrice: Double, maxPrice: Double) {
        _currentSearchQuery.value = query
        _currentCategory.value = category
        _currentMinPrice.value = minPrice
        _currentMaxPrice.value = maxPrice
        applyCurrentFilters()
    }

    fun applyCurrentFilters() {
        viewModelScope.launch {
            phoneRepository.searchPhones(
                query = _currentSearchQuery.value,
                category = _currentCategory.value,
                minPrice = _currentMinPrice.value,
                maxPrice = _currentMaxPrice.value
            ).collect { _phones.value = it }
        }
    }

    fun resetFilters() {
        _currentSearchQuery.value = ""
        _currentCategory.value = ""
        _currentMinPrice.value = 0.0
        _currentMaxPrice.value = Double.MAX_VALUE
        loadPhones()
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

    // Cart - Basic operations
    fun addToCart(phone: Phone, quantity: Int = 1) {
        val current = _cart.value
        val existingIndex = current.indexOfFirst { it.phone.id == phone.id }

        _cart.value = if (existingIndex >= 0) {
            val currentItem = current[existingIndex]
            val newQuantity = currentItem.quantity + quantity
            // Don't allow exceeding stock
            if (newQuantity <= currentItem.phone.stock) {
                current.toMutableList().apply {
                    set(existingIndex, currentItem.copy(quantity = newQuantity))
                }
            } else {
                _stockError.value = "Cannot add more than ${currentItem.phone.stock} in stock"
                current
            }
        } else {
            if (quantity <= phone.stock) {
                current + CartItem(phone, quantity)
            } else {
                _stockError.value = "Only ${phone.stock} left in stock!"
                current
            }
        }
    }

    fun removeFromCart(phoneId: String) {
        _cart.value = _cart.value.filter { it.phone.id != phoneId }
        clearStockError()
    }

    fun updateCartQuantity(phoneId: String, newQuantity: Int) {
        val currentCart = _cart.value
        val targetItem = currentCart.find { it.phone.id == phoneId }

        if (targetItem == null) {
            _stockError.value = "Item not found in cart"
            return
        }

        if (newQuantity < 1) {
            // Remove item if quantity becomes 0 or negative
            removeFromCart(phoneId)
            return
        }

        if (newQuantity > targetItem.phone.stock) {
            _stockError.value = "Only ${targetItem.phone.stock} available in stock"
            return
        }

        val updatedCart = currentCart.map { cartItem ->
            if (cartItem.phone.id == phoneId) {
                cartItem.copy(quantity = newQuantity)
            } else {
                cartItem
            }
        }

        _cart.value = updatedCart
        clearStockError()
    }

    suspend fun checkStockAndAddToCart(phone: Phone, quantity: Int = 1): Boolean {
        val isAvailable = phoneRepository.checkStockAvailability(phone.id, quantity)
        if (!isAvailable) {
            _stockError.value = "Only ${phone.stock} left in stock!"
            return false
        }
        addToCart(phone, quantity)
        _stockError.value = null
        return true
    }

    fun clearCart() {
        _cart.value = emptyList()
        clearStockError()
    }

    fun getCartTotal(): Double = _cart.value.sumOf { it.phone.price * it.quantity }

    fun getCartItemCount(): Int = _cart.value.sumOf { it.quantity }

    fun clearStockError() {
        _stockError.value = null
    }
}