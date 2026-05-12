package com.smartselect.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartselect.R
import com.smartselect.data.repository.OrderRepository
import com.smartselect.databinding.FragmentOrdersBinding
import com.smartselect.utils.Resource
import com.smartselect.utils.hide
import com.smartselect.utils.show
import com.smartselect.viewmodel.PhoneViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private val phoneViewModel: PhoneViewModel by activityViewModels()

    @Inject lateinit var orderRepository: OrderRepository

    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCart()
        loadOrderHistory()
    }

    private fun setupCart() {
        cartAdapter = CartAdapter(
            onRemove = { phoneId ->
                phoneViewModel.removeFromCart(phoneId)
            },
            onQuantityChange = { phoneId, newQuantity ->
                phoneViewModel.updateCartQuantity(phoneId, newQuantity)
            },
            onSelectionChange = { _, _ ->
                updateSelectedTotal()
            }
        )
        binding.rvCart.apply {
            adapter = cartAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        lifecycleScope.launch {
            phoneViewModel.cart.collect { cart ->
                if (_binding == null) return@collect
                cartAdapter.submitList(cart)
                updateSelectedTotal()

                // Toggle empty state
                val isEmpty = cart.isEmpty()
                binding.tvEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.cardEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }
        }

        binding.btnCheckout.setOnClickListener {
            // Store selected IDs in ViewModel before navigating
            val selectedIds = cartAdapter.getSelectedIds()
            if (selectedIds.isEmpty()) {
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "Please select at least one item to checkout",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            phoneViewModel.setSelectedCheckoutIds(selectedIds)
            findNavController().navigate(R.id.action_orders_to_checkout)
        }
    }

    private fun updateSelectedTotal() {
        if (_binding == null) return
        val cart = phoneViewModel.cart.value
        val selectedIds = cartAdapter.getSelectedIds()
        val selectedTotal = cart.filter { it.phone.id in selectedIds }
            .sumOf { it.phone.price * it.quantity }
        val selectedCount = selectedIds.size
        val totalCount = cart.size

        binding.tvCartTotal.text = "₱${String.format("%,.0f", selectedTotal)}"
        binding.btnCheckout.isEnabled = selectedIds.isNotEmpty()
        binding.btnCheckout.text = if (selectedCount < totalCount && totalCount > 0) {
            "Checkout ($selectedCount/$totalCount) →"
        } else {
            "Checkout →"
        }
    }

    private fun loadOrderHistory() {
        val orderAdapter = OrderHistoryAdapter(
            onOrderClick = { order ->
                CustomerOrderDetailDialog.newInstance(order).show(childFragmentManager, "customer_order_detail")
            }
        )
        binding.rvOrders.apply {
            adapter = orderAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        lifecycleScope.launch {
            orderRepository.getUserOrders().collect { resource ->
                if (_binding == null) return@collect
                when (resource) {
                    is Resource.Loading -> binding.ordersProgress.show()
                    is Resource.Success -> {
                        binding.ordersProgress.hide()
                        // Sort: pending first, then by pickup date
                        val sorted = (resource.data ?: emptyList()).sortedWith(
                            compareBy(
                                { statusOrder(it.status) },
                                { -(it.timestamp?.seconds ?: 0L) }
                            )
                        )
                        orderAdapter.submitList(sorted)
                    }
                    is Resource.Error -> {
                        binding.ordersProgress.hide()
                        com.google.android.material.snackbar.Snackbar.make(
                            binding.root,
                            "Error loading orders: ${resource.message}",
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun statusOrder(status: String) = when (status) {
        "pending"   -> 0
        "confirmed" -> 1
        "picked_up" -> 2
        "cancelled" -> 3
        else        -> 4
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}