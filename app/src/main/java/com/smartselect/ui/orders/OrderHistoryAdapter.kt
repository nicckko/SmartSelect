package com.smartselect.ui.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartselect.R
import com.smartselect.data.model.Order
import com.smartselect.databinding.ItemOrderHistoryBinding
import com.smartselect.utils.toPeso
import java.text.SimpleDateFormat
import java.util.Locale

class OrderHistoryAdapter(
    private val onOrderClick: (Order) -> Unit = {}
) : ListAdapter<Order, OrderHistoryAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemOrderHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            val ctx = binding.root.context
            val fmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            binding.tvOrderId.text = "Order #${order.orderId.take(8).uppercase()}"
            binding.tvPhones.text   = order.phoneNames.joinToString(" · ")
            binding.tvTotal.text    = order.totalPrice.toPeso()

            // Pickup code
            if (order.pickupCode.isNotEmpty()) {
                binding.tvPickupCode.text = "Code: ${order.pickupCode}"
                binding.tvPickupCode.visibility = android.view.View.VISIBLE
            } else {
                binding.tvPickupCode.visibility = android.view.View.GONE
            }

            // Pickup date
            val pickupStr = order.pickupDate?.toDate()?.let { fmt.format(it) }
            if (!pickupStr.isNullOrEmpty()) {
                binding.tvPickupDate.text = "Pickup: $pickupStr"
                binding.tvPickupDate.visibility = android.view.View.VISIBLE
            } else {
                binding.tvPickupDate.visibility = android.view.View.GONE
            }

            // Order placed date
            order.timestamp?.toDate()?.let {
                binding.tvDate.text = "Placed: ${fmt.format(it)}"
            } ?: run { binding.tvDate.text = "" }

            // Status badge
            val statusLabel = when (order.status) {
                "pending"    -> "⏳ Pending"
                "confirmed"  -> "✅ Confirmed"
                "picked_up"  -> "📦 Picked Up"
                "cancelled"  -> "❌ Cancelled"
                else         -> order.status.replaceFirstChar { it.uppercase() }
            }
            binding.tvStatus.text = statusLabel

            val (bgColor, textColor) = when (order.status) {
                "confirmed"  -> Pair(
                    ContextCompat.getColor(ctx, R.color.info_light),
                    ContextCompat.getColor(ctx, R.color.info)
                )
                "picked_up"  -> Pair(
                    ContextCompat.getColor(ctx, R.color.success_light),
                    ContextCompat.getColor(ctx, R.color.success)
                )
                "cancelled"  -> Pair(
                    ContextCompat.getColor(ctx, R.color.error_light),
                    ContextCompat.getColor(ctx, R.color.error)
                )
                else         -> Pair( // pending
                    ContextCompat.getColor(ctx, R.color.warning_light),
                    ContextCompat.getColor(ctx, R.color.warning)
                )
            }
            binding.tvStatus.setBackgroundColor(bgColor)
            binding.tvStatus.setTextColor(textColor)
            binding.tvStatus.setPadding(20, 8, 20, 8)

            binding.root.setOnClickListener { onOrderClick(order) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order) =
            oldItem.orderId == newItem.orderId
        override fun areContentsTheSame(oldItem: Order, newItem: Order) = oldItem == newItem
    }
}