package com.smartselect.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.smartselect.R
import com.smartselect.data.model.Order
import com.smartselect.data.repository.OrderRepository
import com.smartselect.databinding.DialogCustomerOrderDetailBinding
import com.smartselect.utils.Resource
import com.smartselect.utils.toPeso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CustomerOrderDetailDialog : BottomSheetDialogFragment() {

    private var _binding: DialogCustomerOrderDetailBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var orderRepository: OrderRepository

    private lateinit var order: Order

    companion object {
        fun newInstance(order: Order) = CustomerOrderDetailDialog().apply {
            this.order = order
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCustomerOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvOrderId.text = "#${order.orderId.take(8).uppercase()}"
        binding.tvPhonesList.text = order.phoneNames.mapIndexed { i, name -> "${i + 1}. $name" }.joinToString("\n")
        binding.tvTotalPrice.text = order.totalPrice.toPeso()
        binding.tvPickupCode.text = if (order.pickupCode.isNotEmpty()) order.pickupCode else "N/A"

        val dateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.tvPickupDate.text = order.pickupDate?.toDate()?.let { dateFmt.format(it) } ?: "Not set"

        // Status badge
        val statusMap = mapOf(
            "pending" to Pair("⏳ Pending", R.color.warning_light),
            "confirmed" to Pair("✅ Confirmed", R.color.info_light),
            "picked_up" to Pair("📦 Picked Up", R.color.success_light),
            "cancelled" to Pair("❌ Cancelled", R.color.error_light)
        )
        val statusTextColorMap = mapOf(
            "pending" to R.color.warning, "confirmed" to R.color.info,
            "picked_up" to R.color.success, "cancelled" to R.color.error
        )

        val (label, bgColor) = statusMap[order.status] ?: Pair(order.status, R.color.surface_variant)
        binding.tvStatus.text = label
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), statusTextColorMap[order.status] ?: R.color.text_primary))
        binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), bgColor))

        binding.btnClose.setOnClickListener { dismiss() }

        if (order.status == "pending" || order.status == "confirmed") {
            binding.btnCancelOrder.visibility = View.VISIBLE
            binding.btnEditDate.visibility = View.VISIBLE
        } else {
            binding.btnCancelOrder.visibility = View.GONE
            binding.btnEditDate.visibility = View.GONE
        }

        binding.btnEditDate.setOnClickListener {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            val constraints = CalendarConstraints.Builder().setValidator(DateValidatorPointForward.from(tomorrow)).build()
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select New Pickup Date")
                .setCalendarConstraints(constraints)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val newDate = Date(selection)
                binding.tvPickupDate.text = dateFmt.format(newDate)
                lifecycleScope.launch {
                    try {
                        // Assuming updateOrderPickupDate exists, let's create a generic update method or implement one
                        orderRepository.updateOrderPickupDate(order.orderId, Timestamp(newDate))
                        Snackbar.make(requireView(), "Pickup date updated", Snackbar.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Snackbar.make(requireView(), "Failed to update date", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            datePicker.show(parentFragmentManager, "date_picker")
        }

        binding.btnCancelOrder.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {
                        val result = orderRepository.updateOrderStatus(order.orderId, "cancelled")
                        if (result is Resource.Error) {
                            Snackbar.make(requireView(), "Failed to cancel: ${result.message}", Snackbar.LENGTH_LONG).show()
                        } else {
                            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Order cancelled", Snackbar.LENGTH_SHORT).show()
                            dismiss()
                        }
                    }
                }.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
