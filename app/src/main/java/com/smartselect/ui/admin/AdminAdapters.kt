package com.smartselect.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartselect.R
import com.smartselect.data.model.Order
import com.smartselect.data.model.Phone
import com.smartselect.databinding.ItemAdminOrderBinding
import com.smartselect.databinding.ItemAdminPhoneGridBinding
import com.smartselect.utils.GlideImageLoader
import com.smartselect.utils.getStockColor
import com.smartselect.utils.getStockLabel
import com.smartselect.utils.toPeso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdminPhoneAdapter(
    private val onEdit: (Phone) -> Unit,
    private val onDelete: (Phone) -> Unit,
    private val onLongPress: ((Phone) -> Unit)? = null,
    private val onSelectionClick: ((Phone) -> Unit)? = null,
    private val selectedIds: Set<String>? = null
) : ListAdapter<Phone, AdminPhoneAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemAdminPhoneGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(phone: Phone) {
            binding.tvPhoneName.text = "${phone.brand} ${phone.model}"
            binding.tvPrice.text = phone.price.toPeso()
            binding.tvStock.text = phone.stock.getStockLabel()
            binding.tvStock.setTextColor(ContextCompat.getColor(binding.root.context, phone.stock.getStockColor()))
            GlideImageLoader.loadImage(binding.root.context, phone.imageUrl, binding.ivPhone)

            // Selection highlight
            val isSelected = selectedIds?.contains(phone.id) == true
            binding.root.alpha = if (isSelected) 0.7f else 1f
            binding.root.strokeWidth = if (isSelected) 3 else 1
            binding.root.strokeColor = ContextCompat.getColor(
                binding.root.context,
                if (isSelected) R.color.accent else R.color.divider
            )

            // Click behavior depends on selection mode
            val inSelectionMode = selectedIds != null && selectedIds.isNotEmpty()
            if (inSelectionMode) {
                binding.root.setOnClickListener { onSelectionClick?.invoke(phone) }
                binding.btnEdit.visibility = View.GONE
                binding.btnDelete.visibility = View.GONE
            } else {
                binding.root.setOnClickListener(null)
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnEdit.setOnClickListener { onEdit(phone) }
                binding.btnDelete.setOnClickListener { onDelete(phone) }
            }

            // Long press always enters selection mode
            binding.root.setOnLongClickListener {
                onLongPress?.invoke(phone)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAdminPhoneGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Phone>() {
        override fun areItemsTheSame(a: Phone, b: Phone) = a.id == b.id
        override fun areContentsTheSame(a: Phone, b: Phone) = a == b
    }
}

class AdminOrderAdapter(
    private val onStatusChange: (Order, String) -> Unit,
    private val onOrderClick: (Order) -> Unit = {},
    private val onLongPress: ((Order) -> Unit)? = null,
    private val onSelectionClick: ((Order) -> Unit)? = null,
    private val selectedIds: Set<String>? = null
) : ListAdapter<Order, AdminOrderAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemAdminOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            val fullFmt = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault())
            val ctx = binding.root.context

            binding.tvOrderId.text = "#${order.orderId.take(8).uppercase()}"
            binding.tvCustomer.text = order.userName.ifEmpty { "Unknown" }
            binding.tvPhones.text = order.phoneNames.joinToString(" · ")
            binding.tvTotal.text = order.totalPrice.toPeso()

            // Selection styling
            val isSelected = selectedIds?.contains(order.orderId) == true
            binding.root.alpha = if (isSelected) 0.7f else 1f
            if (isSelected) {
                binding.root.setCardBackgroundColor(android.graphics.Color.parseColor("#1A3B82F6"))
                binding.root.strokeColor = ContextCompat.getColor(ctx, R.color.accent)
                binding.root.strokeWidth = 3
            } else {
                binding.root.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.surface))
                binding.root.strokeColor = ContextCompat.getColor(ctx, R.color.divider)
                binding.root.strokeWidth = 1
            }

            val inSelectionMode = selectedIds != null && selectedIds.isNotEmpty()
            if (inSelectionMode) {
                binding.root.setOnClickListener { onSelectionClick?.invoke(order) }
                binding.btnStatus.isEnabled = false
            } else {
                binding.root.setOnClickListener { onOrderClick(order) }
                binding.btnStatus.isEnabled = true
            }

            binding.root.setOnLongClickListener {
                onLongPress?.invoke(order)
                true
            }

            // Full datetime
            order.timestamp?.toDate()?.let {
                binding.tvDate.text = "Placed: ${fullFmt.format(it)}"
            }

            // Relative time
            order.timestamp?.toDate()?.let { date ->
                binding.tvRelativeTime.text = getRelativeTime(date)
                binding.tvRelativeTime.visibility = View.VISIBLE
            } ?: run { binding.tvRelativeTime.visibility = View.GONE }

            // Contact number
            if (order.contact.isNotEmpty()) {
                binding.tvContact.text = "📞 ${order.contact}"
                binding.tvContact.visibility = View.VISIBLE
            } else { binding.tvContact.visibility = View.GONE }

            // Pickup code
            if (order.pickupCode.isNotEmpty()) {
                binding.tvPickupCode.text = order.pickupCode
                binding.tvPickupCode.visibility = View.VISIBLE
            } else { binding.tvPickupCode.visibility = View.GONE }

            // Pickup date + overdue
            val pickupFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            order.pickupDate?.toDate()?.let { pickupDate ->
                val isOverdue = pickupDate.before(Date()) &&
                        order.status != "picked_up" && order.status != "cancelled"
                if (isOverdue) {
                    binding.tvPickupDate.text = "⚠️ OVERDUE · ${pickupFmt.format(pickupDate)}"
                    binding.tvPickupDate.setTextColor(ContextCompat.getColor(ctx, R.color.error))
                } else {
                    binding.tvPickupDate.text = pickupFmt.format(pickupDate)
                    binding.tvPickupDate.setTextColor(ContextCompat.getColor(ctx, R.color.accent))
                }
                binding.tvPickupDate.visibility = View.VISIBLE
            } ?: run { binding.tvPickupDate.visibility = View.GONE }

            // Status button with text + color
            val statusMap = mapOf(
                "pending" to "⏳ Pending", "confirmed" to "✅ Confirmed",
                "picked_up" to "📦 Picked Up", "cancelled" to "❌ Cancelled"
            )
            binding.btnStatus.text = statusMap[order.status] ?: order.status.replaceFirstChar { it.uppercase() }

            val colorRes = when (order.status) {
                "pending" -> R.color.warning
                "confirmed" -> R.color.info
                "picked_up" -> R.color.success
                "cancelled" -> R.color.error
                else -> R.color.text_primary
            }
            val colorVal = ContextCompat.getColor(ctx, colorRes)
            binding.btnStatus.setTextColor(colorVal)
            (binding.btnStatus as? com.google.android.material.button.MaterialButton)?.strokeColor =
                android.content.res.ColorStateList.valueOf(colorVal)

            // Left color accent strip
            binding.viewStatusStrip.setBackgroundColor(colorVal)

            // Status popup menu
            binding.btnStatus.setOnClickListener { view ->
                val popup = android.widget.PopupMenu(ctx, view)
                statusMap.values.forEachIndexed { index, label ->
                    popup.menu.add(android.view.Menu.NONE, index, index, label)
                }
                popup.setOnMenuItemClickListener { item ->
                    val sel = statusMap.keys.elementAt(item.itemId)
                    if (sel != order.status) onStatusChange(order, sel)
                    true
                }
                popup.show()
            }

            // Quick action buttons for pending
            if (order.status == "pending") {
                binding.layoutQuickActions.visibility = View.VISIBLE
                binding.btnQuickConfirm.setOnClickListener { onStatusChange(order, "confirmed") }
                binding.btnQuickCancel.setOnClickListener { onStatusChange(order, "cancelled") }
            } else {
                binding.layoutQuickActions.visibility = View.GONE
            }

            // Dim cancelled/picked_up
            binding.root.alpha = when (order.status) {
                "cancelled" -> 0.5f
                "picked_up" -> 0.8f
                else -> 1f
            }
        }

        private fun getRelativeTime(date: Date): String {
            val diff = System.currentTimeMillis() - date.time
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hrs = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            return when {
                mins < 1 -> "Just now"
                mins < 60 -> "${mins}m ago"
                hrs < 24 -> "${hrs}h ago"
                days < 2 -> "Yesterday"
                days < 7 -> "${days}d ago"
                else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAdminOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(a: Order, b: Order) = a.orderId == b.orderId
        override fun areContentsTheSame(a: Order, b: Order) = a == b
    }
}