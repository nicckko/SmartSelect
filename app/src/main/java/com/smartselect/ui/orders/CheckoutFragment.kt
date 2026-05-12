package com.smartselect.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.smartselect.data.model.CartItem
import com.smartselect.data.model.Order
import com.smartselect.data.repository.AuthRepository
import com.smartselect.data.repository.OrderRepository
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.databinding.FragmentCheckoutBinding
import com.smartselect.utils.Resource
import com.smartselect.utils.toPeso
import com.smartselect.viewmodel.PhoneViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!
    private val phoneViewModel: PhoneViewModel by activityViewModels()

    @Inject lateinit var orderRepository: OrderRepository
    @Inject lateinit var phoneRepository: PhoneRepository
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var authRepository: AuthRepository

    private var selectedPickupDate: Date? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        autoFillUserName()
        setupCartSummary()
        setupDatePicker()
        setupContactValidation()
        binding.btnPlaceOrder.setOnClickListener { placeOrder() }
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    /**
     * Auto-fill the name field from the user's Firebase profile (registered full name).
     */
    private fun autoFillUserName() {
        // First try Firebase Auth displayName
        val displayName = auth.currentUser?.displayName
        if (!displayName.isNullOrBlank()) {
            binding.etName.setText(displayName)
        }

        // Also fetch from Firestore for more accurate data
        lifecycleScope.launch {
            val result = authRepository.getCurrentUserData()
            if (result is Resource.Success) {
                val user = result.data!!
                val fullName = "${user.firstName} ${user.lastName}".trim()
                if (fullName.isNotBlank()) {
                    binding.etName.setText(fullName)
                }
            }
        }
    }

    /**
     * Get only the cart items that were selected for checkout.
     */
    private fun getSelectedCartItems(): List<CartItem> {
        val selectedIds = phoneViewModel.selectedCheckoutIds.value
        val cart = phoneViewModel.cart.value
        return if (selectedIds.isEmpty()) {
            cart // fallback: checkout all if no selection info
        } else {
            cart.filter { it.phone.id in selectedIds }
        }
    }

    private fun setupCartSummary() {
        val selectedCart = getSelectedCartItems()
        val summary = selectedCart.joinToString("\n") {
            "${it.phone.brand} ${it.phone.model} x${it.quantity} — ${(it.phone.price * it.quantity).toPeso()}"
        }
        binding.tvOrderSummary.text = summary.ifEmpty { "No items selected" }

        val total = selectedCart.sumOf { it.phone.price * it.quantity }
        binding.tvTotal.text = total.toPeso()
    }

    /**
     * Real-time validation for contact number field.
     */
    private fun setupContactValidation() {
        binding.etContact.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s?.toString() ?: ""
                val digitsOnly = text.filter { it.isDigit() }

                // Only allow digits
                if (text != digitsOnly) {
                    binding.etContact.removeTextChangedListener(this)
                    binding.etContact.setText(digitsOnly)
                    binding.etContact.setSelection(digitsOnly.length)
                    binding.etContact.addTextChangedListener(this)
                }

                // Validate length
                when {
                    digitsOnly.isEmpty() -> {
                        binding.tilContact.error = null
                    }
                    digitsOnly.length < 11 -> {
                        binding.tilContact.error = "${11 - digitsOnly.length} more digit(s) needed"
                    }
                    digitsOnly.length == 11 -> {
                        binding.tilContact.error = null
                        binding.tilContact.helperText = "✓ Valid phone number"
                    }
                }
            }
        })
    }

    private fun setupDatePicker() {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis

        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.from(tomorrow))
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Pickup Date")
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedPickupDate = Date(selection)
            val formatted = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(selectedPickupDate!!)
            binding.tvPickupDate.text = formatted
            binding.tvPickupDate.setTextColor(
                requireContext().getColor(com.smartselect.R.color.text_primary)
            )
        }

        binding.btnPickDate.setOnClickListener {
            datePicker.show(parentFragmentManager, "date_picker")
        }
    }

    private fun placeOrder() {
        val name = binding.etName.text.toString().trim()
        val contact = binding.etContact.text.toString().trim()

        // ── Validation ──────────────────────────────────────────────
        // Name validation
        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            showSnackbar("Unable to retrieve your name. Please contact support.")
            return
        }
        binding.tilName.error = null

        // Contact validation: must be exactly 11 digits
        if (contact.isEmpty()) {
            binding.tilContact.error = "Phone number is required"
            binding.etContact.requestFocus()
            return
        }

        val digitsOnly = contact.filter { it.isDigit() }
        if (digitsOnly.length != 11) {
            binding.tilContact.error = "Phone number must be exactly 11 digits"
            binding.etContact.requestFocus()
            return
        }

        if (!digitsOnly.startsWith("09")) {
            binding.tilContact.error = "Phone number must start with 09"
            binding.etContact.requestFocus()
            return
        }
        binding.tilContact.error = null

        // Pickup date validation
        if (selectedPickupDate == null) {
            showSnackbar("Please select a pickup date")
            return
        }

        // Cart validation — use only selected items
        val selectedCart = getSelectedCartItems()
        if (selectedCart.isEmpty()) {
            showSnackbar("No items selected for checkout")
            return
        }

        lifecycleScope.launch {
            // First, check if all items have sufficient stock
            var hasStockIssue = false
            for (item in selectedCart) {
                val isAvailable = phoneRepository.checkStockAvailability(item.phone.id, item.quantity)
                if (!isAvailable) {
                    hasStockIssue = true
                    showSnackbar("${item.phone.brand} ${item.phone.model} only has ${item.phone.stock} left in stock!")
                    break
                }
            }

            if (hasStockIssue) return@launch

            // Generate unique 6-char alphanumeric pickup code
            val pickupCode = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .take(6)
                .uppercase()

            val totalPrice = selectedCart.sumOf { it.phone.price * it.quantity }

            val order = Order(
                userId = auth.currentUser?.uid ?: "",
                phoneIds = selectedCart.map { it.phone.id },
                phoneQuantities = selectedCart.map { it.quantity },
                phoneNames = selectedCart.map { "${it.phone.brand} ${it.phone.model} x${it.quantity}" },
                totalPrice = totalPrice,
                status = "pending",
                timestamp = Timestamp.now(),
                userName = name,
                contact = digitsOnly,
                pickupDate = Timestamp(selectedPickupDate!!),
                pickupCode = pickupCode
            )

            binding.btnPlaceOrder.isEnabled = false
            binding.btnPlaceOrder.text = "Placing Order..."

            // Decrease stock for each selected item
            val stockUpdates = selectedCart.map { it.phone.id to it.quantity }
            val stockResult = phoneRepository.decreaseMultipleStocks(stockUpdates)

            if (stockResult is Resource.Error) {
                showSnackbar("Stock error: ${stockResult.message}")
                binding.btnPlaceOrder.isEnabled = true
                binding.btnPlaceOrder.text = "Place Order"
                return@launch
            }

            // Place the order
            when (val result = orderRepository.placeOrder(order)) {
                is Resource.Success -> {
                    // Remove only the checked-out items from the cart
                    val selectedIds = phoneViewModel.selectedCheckoutIds.value
                    if (selectedIds.isNotEmpty() && selectedIds.size < phoneViewModel.cart.value.size) {
                        // Partial checkout — remove only selected items
                        selectedIds.forEach { id ->
                            phoneViewModel.removeFromCart(id)
                        }
                    } else {
                        // Full checkout — clear entire cart
                        phoneViewModel.clearCart()
                    }

                    val pickupDateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                        .format(selectedPickupDate!!)

                    val dialog = OrderSuccessDialog.newInstance(pickupCode, pickupDateStr)
                    dialog.show(childFragmentManager, "order_success")

                    binding.btnPlaceOrder.isEnabled = true
                    binding.btnPlaceOrder.text = "Place Order"
                }
                is Resource.Error -> {
                    // If order fails, restore the stock
                    val restoreUpdates = stockUpdates.map { it.first to -it.second }
                    phoneRepository.decreaseMultipleStocks(restoreUpdates)
                    showSnackbar("Order failed: ${result.message ?: "Unknown error"}")
                    binding.btnPlaceOrder.isEnabled = true
                    binding.btnPlaceOrder.text = "Place Order"
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    private fun showSnackbar(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}