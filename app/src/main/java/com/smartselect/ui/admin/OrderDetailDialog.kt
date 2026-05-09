package com.smartselect.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartselect.R
import com.smartselect.data.model.Order
import com.smartselect.databinding.DialogOrderDetailBinding
import com.smartselect.utils.toPeso
import java.text.SimpleDateFormat
import java.util.Locale

class OrderDetailDialog : BottomSheetDialogFragment() {

    private var _binding: DialogOrderDetailBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_ORDER_ID = "orderId"
        private const val ARG_USER_NAME = "userName"
        private const val ARG_CONTACT = "contact"
        private const val ARG_PHONE_NAMES = "phoneNames"
        private const val ARG_TOTAL = "total"
        private const val ARG_STATUS = "status"
        private const val ARG_PICKUP_CODE = "pickupCode"
        private const val ARG_PICKUP_DATE = "pickupDate"
        private const val ARG_TIMESTAMP = "timestamp"

        fun newInstance(order: Order) = OrderDetailDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_ORDER_ID, order.orderId)
                putString(ARG_USER_NAME, order.userName)
                putString(ARG_CONTACT, order.contact)
                putStringArrayList(ARG_PHONE_NAMES, ArrayList(order.phoneNames))
                putDouble(ARG_TOTAL, order.totalPrice)
                putString(ARG_STATUS, order.status)
                putString(ARG_PICKUP_CODE, order.pickupCode)
                putLong(ARG_PICKUP_DATE, order.pickupDate?.seconds ?: 0L)
                putLong(ARG_TIMESTAMP, order.timestamp?.seconds ?: 0L)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val orderId = args.getString(ARG_ORDER_ID, "")
        val userName = args.getString(ARG_USER_NAME, "Unknown")
        val contact = args.getString(ARG_CONTACT, "")
        val phoneNames = args.getStringArrayList(ARG_PHONE_NAMES) ?: arrayListOf()
        val total = args.getDouble(ARG_TOTAL, 0.0)
        val status = args.getString(ARG_STATUS, "pending")
        val pickupCode = args.getString(ARG_PICKUP_CODE, "")
        val pickupDateSec = args.getLong(ARG_PICKUP_DATE, 0L)
        val timestampSec = args.getLong(ARG_TIMESTAMP, 0L)

        val fmt = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        binding.tvOrderId.text = "#${orderId.take(8).uppercase()}"
        binding.tvCustomerName.text = userName
        binding.tvCustomerContact.text = if (contact.isNotEmpty()) "📞 $contact" else "No contact provided"

        // Items
        binding.tvPhonesList.text = phoneNames.mapIndexed { i, name -> "${i + 1}. $name" }.joinToString("\n")
        binding.tvTotalPrice.text = total.toPeso()

        // Pickup
        binding.tvPickupCode.text = if (pickupCode.isNotEmpty()) pickupCode else "N/A"
        binding.tvPickupDate.text = if (pickupDateSec > 0) dateFmt.format(java.util.Date(pickupDateSec * 1000)) else "Not set"
        binding.tvOrderDate.text = if (timestampSec > 0) "Ordered: ${fmt.format(java.util.Date(timestampSec * 1000))}" else ""

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

        val (label, bgColor) = statusMap[status] ?: Pair(status, R.color.surface_variant)
        binding.tvStatus.text = label
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), statusTextColorMap[status] ?: R.color.text_primary))
        binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), bgColor))

        binding.btnClose.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
