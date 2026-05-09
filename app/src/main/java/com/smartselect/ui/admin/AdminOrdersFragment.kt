package com.smartselect.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartselect.data.model.Order
import com.smartselect.data.repository.OrderRepository
import com.smartselect.databinding.FragmentAdminOrdersBinding
import com.smartselect.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AdminOrdersFragment : Fragment() {

    private var _binding: FragmentAdminOrdersBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var orderRepository: OrderRepository
    @Inject lateinit var adminLogRepository: com.smartselect.data.repository.AdminLogRepository
    private lateinit var adapter: AdminOrderAdapter

    private var allOrders: List<Order> = emptyList()
    private var currentFilter: String = "all"
    private var currentSearch: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminOrderAdapter(
            onStatusChange = { order, status ->
                lifecycleScope.launch {
                    val result = orderRepository.updateOrderStatus(order.orderId, status)
                    if (result is Resource.Error) {
                        com.google.android.material.snackbar.Snackbar.make(binding.root, "Failed to update: ${result.message}", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                    } else {
                        adminLogRepository.logAction("Order Status Changed", "Order #${order.orderId.take(8).uppercase()} changed to $status")
                    }
                }
            },
            onOrderClick = { order ->
                OrderDetailDialog.newInstance(order).show(childFragmentManager, "order_detail")
            }
        )

        binding.rvOrders.apply {
            this.adapter = this@AdminOrdersFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        setupSearch()
        setupFilterChips()

        lifecycleScope.launch {
            orderRepository.getAllOrders().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    val orders = resource.data ?: emptyList()
                    orderRepository.checkAndCancelExpiredOrders(orders)
                    allOrders = orders
                    updateCounts()
                    applyFilters()
                }
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearch = s?.toString()?.trim()?.lowercase() ?: ""
                applyFilters()
            }
        })
    }

    private fun setupFilterChips() {
        val statuses = listOf("All Orders", "⏳ Pending", "✅ Confirmed", "📦 Picked Up", "❌ Cancelled")
        val filterAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        binding.actvStatusFilter.setAdapter(filterAdapter)

        binding.actvStatusFilter.setOnItemClickListener { _, _, position, _ ->
            currentFilter = when (position) {
                1 -> "pending"
                2 -> "confirmed"
                3 -> "picked_up"
                4 -> "cancelled"
                else -> "all"
            }
            applyFilters()
        }
    }

    private fun updateCounts() {
        val pending = allOrders.count { it.status == "pending" }
        val confirmed = allOrders.count { it.status == "confirmed" }
        val pickedUp = allOrders.count { it.status == "picked_up" }
        val cancelled = allOrders.count { it.status == "cancelled" }

        binding.tvOrderCount.text = "${allOrders.size} total orders"
        binding.tvPendingCount.text = "⏳ $pending  ·  ✅ $confirmed  ·  📦 $pickedUp  ·  ❌ $cancelled"

        val statuses = listOf(
            "All Orders (${allOrders.size})",
            "⏳ Pending ($pending)",
            "✅ Confirmed ($confirmed)",
            "📦 Picked Up ($pickedUp)",
            "❌ Cancelled ($cancelled)"
        )
        val filterAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        binding.actvStatusFilter.setAdapter(filterAdapter)
        
        // keep current selection text updated
        val currentTextIndex = when (currentFilter) {
            "pending" -> 1
            "confirmed" -> 2
            "picked_up" -> 3
            "cancelled" -> 4
            else -> 0
        }
        binding.actvStatusFilter.setText(statuses[currentTextIndex], false)
    }

    private fun applyFilters() {
        var filtered = allOrders

        if (currentFilter != "all") {
            filtered = filtered.filter { it.status == currentFilter }
        }

        if (currentSearch.isNotEmpty()) {
            filtered = filtered.filter { order ->
                order.userName.lowercase().contains(currentSearch) ||
                order.orderId.lowercase().contains(currentSearch) ||
                order.phoneNames.any { it.lowercase().contains(currentSearch) } ||
                order.contact.lowercase().contains(currentSearch) ||
                order.pickupCode.lowercase().contains(currentSearch)
            }
        }

        val sorted = filtered.sortedWith(
            compareBy<Order> { statusPriority(it.status) }
                .thenByDescending { it.timestamp?.seconds ?: 0L }
        )
        adapter.submitList(sorted)
    }

    private fun statusPriority(status: String) = when (status) {
        "pending" -> 0; "confirmed" -> 1; "picked_up" -> 2; "cancelled" -> 3; else -> 4
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}