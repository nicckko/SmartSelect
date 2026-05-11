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
    private val onRemove: (String) -> Unit,
    private val onQuantityChange: (String, Int) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(DiffCallback()) {

    inner class CartViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            val phone = item.phone
            val quantity = item.quantity
            val pricePerUnit = phone.price
            val totalPrice = pricePerUnit * quantity

            binding.tvPhoneName.text = "${phone.brand} ${phone.model}"
            binding.tvPricePerUnit.text = "${pricePerUnit.toPeso()} each"
            binding.tvTotalPrice.text = totalPrice.toPeso()
            binding.tvQuantity.text = quantity.toString()

            Glide.with(binding.root.context)
                .load(phone.imageUrl)
                .placeholder(R.drawable.placeholder_phone)
                .error(R.drawable.placeholder_phone)
                .into(binding.ivPhone)

            // Decrease quantity button
            binding.btnDecrease.setOnClickListener {
                if (quantity > 1) {
                    onQuantityChange(phone.id, quantity - 1)
                } else {
                    // If quantity is 1, confirm before removing
                    onRemove(phone.id)
                }
            }

            // Increase quantity button
            binding.btnIncrease.setOnClickListener {
                onQuantityChange(phone.id, quantity + 1)
            }

            // Remove button
            binding.btnRemove.setOnClickListener { onRemove(phone.id) }
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