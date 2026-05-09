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
        val cartAdapter = CartAdapter(
            onRemove = { phoneViewModel.removeFromCart(it) }
        )
        binding.rvCart.apply {
            adapter = cartAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        lifecycleScope.launch {
            phoneViewModel.cart.collect { cart ->
                if (_binding == null) return@collect
                cartAdapter.submitList(cart)
                val total = phoneViewModel.getCartTotal()
                binding.tvCartTotal.text = "₱${String.format("%,.0f", total)}"
                binding.btnCheckout.isEnabled = cart.isNotEmpty()

                // Toggle empty state — now a LinearLayout
                val isEmpty = cart.isEmpty()
                binding.tvEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.cardEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }
        }

        binding.btnCheckout.setOnClickListener {
            findNavController().navigate(R.id.action_orders_to_checkout)
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