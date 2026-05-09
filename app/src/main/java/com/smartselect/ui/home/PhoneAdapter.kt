package com.smartselect.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartselect.R
import com.smartselect.data.model.Phone
import com.smartselect.databinding.ItemPhoneCardBinding
import com.smartselect.utils.getStockColor
import com.smartselect.utils.getStockLabel
import com.smartselect.utils.toPeso

class PhoneAdapter(
    private val onPhoneClick: (Phone) -> Unit,
    private val onFavoriteClick: (Phone) -> Unit,
    private val onCompareClick: (Phone) -> Unit,
    private val isFavorite: (String) -> Boolean,
    private val isInCompare: (String) -> Boolean  // NEW: function to check if phone is in compare list
) : ListAdapter<Phone, PhoneAdapter.PhoneViewHolder>(DiffCallback()) {

    // Track favorite IDs so we can refresh when they change
    private var favoriteIds: Set<String> = emptySet()

    // NEW: Track compare IDs so we can refresh when compare list changes
    private var compareIds: Set<String> = emptySet()

    fun updateFavorites(ids: Set<String>) {
        val changed = favoriteIds != ids
        favoriteIds = ids
        // Notify items that changed favorite state
        if (changed) notifyItemRangeChanged(0, itemCount, PAYLOAD_FAVORITE)
    }

    // NEW: Update compare IDs and refresh affected items
    fun updateCompare(ids: Set<String>) {
        val changed = compareIds != ids
        val oldIds = compareIds
        compareIds = ids

        if (changed) {
            // Find which items changed their compare state
            val allAffectedIds = oldIds.union(ids)
            val positions = mutableListOf<Int>()
            for (i in 0 until itemCount) {
                val phone = getItem(i)
                if (allAffectedIds.contains(phone.id)) {
                    positions.add(i)
                }
            }
            positions.forEach { position ->
                notifyItemChanged(position, PAYLOAD_COMPARE)
            }
        }
    }

    inner class PhoneViewHolder(private val binding: ItemPhoneCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(phone: Phone) {
            binding.apply {
                tvBrand.text = phone.brand
                tvModel.text = phone.model
                tvPrice.text = phone.price.toPeso()
                tvStock.text = phone.stock.getStockLabel()
                tvStock.setTextColor(ContextCompat.getColor(root.context, phone.stock.getStockColor()))

                tvBestValue.visibility = if (phone.isBestValue) android.view.View.VISIBLE else android.view.View.GONE
                tvCategory.text = phone.category

                bindFavorite(phone.id)
                bindCompare(phone.id)  // NEW: bind compare highlight state

                Glide.with(root.context)
                    .load(phone.imageUrl)
                    .placeholder(R.drawable.placeholder_phone)
                    .error(R.drawable.placeholder_phone)
                    .into(ivPhone)

                root.setOnClickListener { onPhoneClick(phone) }
                ivFavorite.setOnClickListener { onFavoriteClick(phone) }
                btnCompare.setOnClickListener { onCompareClick(phone) }
            }
        }

        fun bindFavorite(phoneId: String) {
            val isFav = isFavorite(phoneId)
            binding.ivFavorite.setImageResource(
                if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
        }

        // NEW: Bind compare highlight state
        fun bindCompare(phoneId: String) {
            val isSelected = isInCompare(phoneId)
            // Change button text and style based on selection state
            if (isSelected) {
                binding.btnCompare.text = "✓ Selected"
                binding.btnCompare.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.accent)
                )
                binding.btnCompare.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.white)
                )
                // Highlight the card with a colored stroke
                binding.root.strokeColor = ContextCompat.getColor(binding.root.context, R.color.accent)
                binding.root.strokeWidth = 4
            } else {
                binding.btnCompare.text = "+ Compare"
                binding.btnCompare.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.transparent)
                )
                binding.btnCompare.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_secondary)
                )
                // Remove highlight
                binding.root.strokeColor = ContextCompat.getColor(binding.root.context, R.color.divider)
                binding.root.strokeWidth = 1
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhoneViewHolder {
        val binding = ItemPhoneCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhoneViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhoneViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: PhoneViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.contains(PAYLOAD_FAVORITE)) {
            holder.bindFavorite(getItem(position).id)
        } else if (payloads.contains(PAYLOAD_COMPARE)) {
            // NEW: Only update compare state
            val phone = getItem(position)
            holder.bindCompare(phone.id)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Phone>() {
        override fun areItemsTheSame(oldItem: Phone, newItem: Phone) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Phone, newItem: Phone) = oldItem == newItem
    }

    companion object {
        private const val PAYLOAD_FAVORITE = "favorite"
        private const val PAYLOAD_COMPARE = "compare"  // NEW
    }
}