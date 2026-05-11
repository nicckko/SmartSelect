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
import com.smartselect.data.model.Order
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

    private var selectedPickupDate: Date? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCartSummary()
        setupDatePicker()
        binding.btnPlaceOrder.setOnClickListener { placeOrder() }
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupCartSummary() {
        val cart = phoneViewModel.cart.value
        val summary = cart.joinToString("\n") {
            "${it.phone.brand} ${it.phone.model} x${it.quantity} — ${(it.phone.price * it.quantity).toPeso()}"
        }
        binding.tvOrderSummary.text = summary.ifEmpty { "No items in cart" }
        binding.tvTotal.text = "Total: ${phoneViewModel.getCartTotal().toPeso()}"
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

        if (name.isEmpty() || contact.isEmpty()) {
            showSnackbar("Please fill all fields")
            return
        }
        if (contact.length < 7) {
            showSnackbar("Please enter a valid contact number")
            return
        }
        if (selectedPickupDate == null) {
            showSnackbar("Please select a pickup date")
            return
        }

        val cart = phoneViewModel.cart.value
        if (cart.isEmpty()) {
            showSnackbar("Your cart is empty")
            return
        }

        lifecycleScope.launch {
            // First, check if all items have sufficient stock
            var hasStockIssue = false
            for (item in cart) {
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

            val order = Order(
                userId = auth.currentUser?.uid ?: "",
                phoneIds = cart.map { it.phone.id },
                phoneQuantities = cart.map { it.quantity },
                phoneNames = cart.map { "${it.phone.brand} ${it.phone.model} x${it.quantity}" },
                totalPrice = phoneViewModel.getCartTotal(),
                status = "pending",
                timestamp = Timestamp.now(),
                userName = name,
                contact = contact,
                pickupDate = Timestamp(selectedPickupDate!!),
                pickupCode = pickupCode
            )

            binding.btnPlaceOrder.isEnabled = false
            binding.btnPlaceOrder.text = "Placing Order..."

            // Decrease stock for each item
            val stockUpdates = cart.map { it.phone.id to it.quantity }
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
                    phoneViewModel.clearCart()

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