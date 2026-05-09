package com.smartselect.ui.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartselect.R
import com.smartselect.data.model.CartItem
import com.smartselect.databinding.ItemCartBinding
import com.smartselect.utils.toPeso

class CartAdapter(
    private val onRemove: (String) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(DiffCallback()) {

    inner class CartViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.tvPhoneName.text = "${item.phone.brand} ${item.phone.model}"
            binding.tvPrice.text = (item.phone.price * item.quantity).toPeso()
            binding.tvQuantity.text = "Qty: ${item.quantity}"
            Glide.with(binding.root.context)
                .load(item.phone.imageUrl)
                .placeholder(R.drawable.placeholder_phone)
                .error(R.drawable.placeholder_phone)
                .into(binding.ivPhone)
            binding.btnRemove.setOnClickListener { onRemove(item.phone.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem) = oldItem.phone.id == newItem.phone.id
        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem) = oldItem == newItem
    }
}
