package com.smartselect.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartselect.R
import com.smartselect.data.model.Order
import com.smartselect.data.model.Phone
import com.smartselect.databinding.ItemAdminOrderBinding
import com.smartselect.databinding.ItemAdminPhoneBinding
import com.smartselect.utils.getStockColor
import com.smartselect.utils.getStockLabel
import com.smartselect.utils.toPeso
import java.text.SimpleDateFormat
import java.util.Locale

class AdminPhoneAdapter(
    private val onEdit: (Phone) -> Unit,
    private val onDelete: (Phone) -> Unit
) : ListAdapter<Phone, AdminPhoneAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemAdminPhoneBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(phone: Phone) {
            binding.tvPhoneName.text = "${phone.brand} ${phone.model}"
            binding.tvPrice.text = phone.price.toPeso()
            binding.tvStock.text = phone.stock.getStockLabel()
            binding.tvStock.setTextColor(
                ContextCompat.getColor(binding.root.context, phone.stock.getStockColor())
            )
            Glide.with(binding.root.context)
                .load(phone.imageUrl)
                .placeholder(R.drawable.placeholder_phone)
                .error(R.drawable.placeholder_phone)
                .into(binding.ivPhone)
            binding.btnEdit.setOnClickListener { onEdit(phone) }
            binding.btnDelete.setOnClickListener { onDelete(phone) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminPhoneBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Phone>() {
        override fun areItemsTheSame(oldItem: Phone, newItem: Phone) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Phone, newItem: Phone) = oldItem == newItem
    }
}

class AdminOrderAdapter(
    private val onStatusChange: (Order, String) -> Unit
) : ListAdapter<Order, AdminOrderAdapter.ViewHolder>(DiffCallback()) {

    // Updated status flow: pending → confirmed → picked_up (or cancelled)
    private val statusOptions = listOf("pending", "confirmed", "picked_up", "cancelled")

    inner class ViewHolder(private val binding: ItemAdminOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            val fmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val ctx = binding.root.context

            binding.tvOrderId.text   = "#${order.orderId.take(8).uppercase()}"
            binding.tvCustomer.text  = order.userName.ifEmpty { "Unknown" }
            binding.tvPhones.text    = order.phoneNames.joinToString(" · ")
            binding.tvTotal.text     = order.totalPrice.toPeso()

            order.timestamp?.toDate()?.let {
                binding.tvDate.text = "Placed: ${fmt.format(it)}"
            }

            // Pickup info
            if (order.pickupCode.isNotEmpty()) {
                binding.tvPickupCode.text = "Code: ${order.pickupCode}"
                binding.tvPickupCode.visibility = android.view.View.VISIBLE
            } else {
                binding.tvPickupCode.visibility = android.view.View.GONE
            }

            order.pickupDate?.toDate()?.let {
                binding.tvPickupDate.text = "Pickup: ${fmt.format(it)}"
                binding.tvPickupDate.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvPickupDate.visibility = android.view.View.GONE
            }

            // Status Button Logic
            val statusMap = mapOf(
                "pending"   to "⏳ Pending",
                "confirmed" to "✅ Confirmed",
                "picked_up" to "📦 Picked Up",
                "cancelled" to "❌ Cancelled"
            )

            binding.btnStatus.text = statusMap[order.status] ?: order.status.replaceFirstChar { it.uppercase() }

            // Set dynamic color based on status
            val colorRes = when (order.status) {
                "pending" -> android.R.color.holo_orange_dark
                "confirmed" -> com.smartselect.R.color.primary
                "picked_up" -> android.R.color.holo_green_dark
                "cancelled" -> android.R.color.holo_red_dark
                else -> com.smartselect.R.color.text_primary
            }
            val colorVal = ContextCompat.getColor(ctx, colorRes)
            binding.btnStatus.setTextColor(colorVal)
            (binding.btnStatus as? com.google.android.material.button.MaterialButton)?.strokeColor = android.content.res.ColorStateList.valueOf(colorVal)

            binding.btnStatus.setOnClickListener { view ->
                val popup = android.widget.PopupMenu(ctx, view)
                statusMap.values.forEachIndexed { index, label ->
                    popup.menu.add(android.view.Menu.NONE, index, index, label)
                }
                popup.setOnMenuItemClickListener { item ->
                    val selectedStatus = statusMap.keys.elementAt(item.itemId)
                    if (selectedStatus != order.status) {
                        onStatusChange(order, selectedStatus)
                    }
                    true
                }
                popup.show()
            }

            // Visual feedback for cancelled orders
            if (order.status == "cancelled") {
                binding.root.alpha = 0.6f
            } else {
                binding.root.alpha = 1f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminOrderBinding.inflate(
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