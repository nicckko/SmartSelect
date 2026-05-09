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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartselect.data.model.Order
import com.smartselect.data.repository.AdminLogRepository
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
    @Inject lateinit var adminLogRepository: AdminLogRepository
    private lateinit var adapter: AdminOrderAdapter

    private var allOrders: List<Order> = emptyList()
    private var currentFilter: String = "all"
    private var currentDateFilter: String = "all"
    private var currentSearch: String = ""

    private val selectedIds = mutableSetOf<String>()
    private var isSelectionMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminOrderAdapter(
            onStatusChange = { order, status ->
                if (!isSelectionMode) {
                    lifecycleScope.launch {
                        val result = orderRepository.updateOrderStatus(order.orderId, status)
                        if (result is Resource.Error) {
                            if (_binding != null) {
                                Snackbar.make(binding.root, "Failed to update: ${result.message}", Snackbar.LENGTH_LONG).show()
                            }
                        } else {
                            adminLogRepository.logAction("Order Status Changed", "Order #${order.orderId.take(8).uppercase()} changed to $status")
                        }
                    }
                }
            },
            onOrderClick = { order ->
                if (!isSelectionMode) {
                    OrderDetailDialog.newInstance(order).show(childFragmentManager, "order_detail")
                }
            },
            onLongPress = { order ->
                if (!isSelectionMode) enterSelectionMode()
                toggleSelection(order.orderId)
            },
            onSelectionClick = { order ->
                if (isSelectionMode) toggleSelection(order.orderId)
            },
            selectedIds = selectedIds
        )

        binding.rvOrders.apply {
            this.adapter = this@AdminOrdersFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        setupSearch()
        setupFilterChips()

        // Selection Handlers
        binding.cbSelectAll.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedIds.addAll(adapter.currentList.map { it.orderId })
            } else {
                selectedIds.clear()
            }
            updateSelectionUI()
            adapter.notifyDataSetChanged()
        }

        binding.btnDeleteSelected.setOnClickListener {
            val selected = adapter.currentList.filter { it.orderId in selectedIds }
            if (selected.isNotEmpty()) confirmDelete(selected)
        }

        binding.btnCancelSelection.setOnClickListener {
            exitSelectionMode()
        }

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

    private fun enterSelectionMode() {
        isSelectionMode = true
        selectedIds.clear()
        updateSelectionUI()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedIds.clear()
        binding.cbSelectAll.isChecked = false
        updateSelectionUI()
        adapter.notifyDataSetChanged()
    }

    private fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) selectedIds.remove(id)
        else selectedIds.add(id)
        if (selectedIds.isEmpty()) exitSelectionMode()
        else updateSelectionUI()
        adapter.notifyDataSetChanged()
    }

    private fun updateSelectionUI() {
        if (_binding == null) return
        binding.layoutSelectionBar.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        if (isSelectionMode) {
            binding.tvOrderCount.text = "${selectedIds.size} selected"
            binding.btnDeleteSelected.text = "Del (${selectedIds.size})"
        } else {
            updateCounts()
        }
    }

    private fun confirmDelete(orders: List<Order>) {
        val msg = if (orders.size == 1) "Delete this order?" else "Delete ${orders.size} orders?"
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Delete Orders")
            .setMessage("$msg\n\nThis will remove the order records permanently. This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    orders.forEach { order ->
                        orderRepository.deleteOrder(order.orderId)
                        adminLogRepository.logAction("Order Deleted", "Deleted order #${order.orderId.take(8).uppercase()}")
                    }
                    if (_binding == null) return@launch
                    val snackMsg = if (orders.size == 1) "Order deleted" else "${orders.size} orders deleted"
                    Snackbar.make(binding.root, snackMsg, Snackbar.LENGTH_SHORT).show()
                    exitSelectionMode()
                }
            }
            .show()
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

        val dates = listOf("All Time", "Today", "This Week", "This Month", "This Year")
        val dateAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, dates)
        binding.actvDateFilter.setAdapter(dateAdapter)
        binding.actvDateFilter.setOnItemClickListener { _, _, position, _ ->
            currentDateFilter = when (position) {
                1 -> "today"
                2 -> "week"
                3 -> "month"
                4 -> "year"
                else -> "all"
            }
            applyFilters()
        }
    }

    private fun updateCounts() {
        if (isSelectionMode) return
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

        // Apply date filter logic (simplified)
        val nowMs = System.currentTimeMillis()
        val dayMs = 86400000L
        if (currentDateFilter != "all") {
            filtered = filtered.filter { order ->
                val dateMs = order.timestamp?.toDate()?.time ?: 0L
                when (currentDateFilter) {
                    "today" -> (nowMs - dateMs) < dayMs
                    "week" -> (nowMs - dateMs) < dayMs * 7
                    "month" -> (nowMs - dateMs) < dayMs * 30
                    "year" -> (nowMs - dateMs) < dayMs * 365
                    else -> true
                }
            }
        }

        // Clear selection when filters change
        if (isSelectionMode) {
            exitSelectionMode()
        }

        adapter.submitList(filtered.sortedByDescending { it.timestamp?.seconds ?: 0L })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}