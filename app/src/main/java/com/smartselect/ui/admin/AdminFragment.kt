package com.smartselect.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.smartselect.data.repository.OrderRepository
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.databinding.FragmentAdminBinding
import com.smartselect.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var phoneRepository: PhoneRepository
    @Inject lateinit var orderRepository: OrderRepository
    @Inject lateinit var adminLogRepository: com.smartselect.data.repository.AdminLogRepository

    private lateinit var phoneAdapter: AdminPhoneAdapter
    private lateinit var orderAdapter: AdminOrderAdapter
    private lateinit var logAdapter: AdminLogAdapter

    private var allPhones: List<com.smartselect.data.model.Phone> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPhoneList()
        setupOrderList()
        setupLogsList()
        setupTabs()
        setupButtons()
    }

    private fun setupButtons() {
        binding.btnAddPhone.setOnClickListener {
            AddEditPhoneDialog().show(parentFragmentManager, "add_phone")
        }
    }

    private fun setupTabs() {
        val selectedTab = arguments?.getInt("selectedTab", 0) ?: 0
        binding.tabLayout.getTabAt(selectedTab)?.select()
        
        when (selectedTab) {
            0 -> showPhones()
            1 -> showOrders()
            2 -> showLogs()
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) { 
                    0 -> showPhones()
                    1 -> showOrders()
                    2 -> showLogs()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showPhones() {
        binding.rvPhones.visibility = View.VISIBLE
        binding.chipGroupStock.visibility = View.VISIBLE
        binding.rvOrders.visibility = View.GONE
        binding.rvLogs.visibility = View.GONE
        binding.btnAddPhone.visibility = View.VISIBLE
        binding.btnSeedData.visibility = View.VISIBLE
    }

    private fun showOrders() {
        binding.rvPhones.visibility = View.GONE
        binding.chipGroupStock.visibility = View.GONE
        binding.rvOrders.visibility = View.VISIBLE
        binding.rvLogs.visibility = View.GONE
        binding.btnAddPhone.visibility = View.GONE
        binding.btnSeedData.visibility = View.GONE
    }

    private fun showLogs() {
        binding.rvPhones.visibility = View.GONE
        binding.chipGroupStock.visibility = View.GONE
        binding.rvOrders.visibility = View.GONE
        binding.rvLogs.visibility = View.VISIBLE
        binding.btnAddPhone.visibility = View.GONE
        binding.btnSeedData.visibility = View.GONE
    }

    private fun setupPhoneList() {
        phoneAdapter = AdminPhoneAdapter(
            onEdit = { phone ->
                AddEditPhoneDialog.newInstance(phone).show(parentFragmentManager, "edit_phone")
            },
            onDelete = { phone ->
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Phone")
                    .setMessage("Are you sure you want to delete ${phone.brand} ${phone.model}?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            phoneRepository.deletePhone(phone.id)
                            adminLogRepository.logAction("Deleted Phone", "Deleted ${phone.brand} ${phone.model}")
                            Snackbar.make(binding.root, "${phone.brand} ${phone.model} deleted", Snackbar.LENGTH_SHORT).show()
                        }
                    }.show()
            }
        )

        binding.rvPhones.apply {
            adapter = phoneAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.chipGroupStock.setOnCheckedStateChangeListener { _, _ ->
            applyStockFilter()
        }

        lifecycleScope.launch {
            phoneRepository.getPhones().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    allPhones = resource.data ?: emptyList()
                    applyStockFilter()
                }
            }
        }
    }

    private fun applyStockFilter() {
        val filtered = when (binding.chipGroupStock.checkedChipId) {
            com.smartselect.R.id.chip_stock_soldout -> allPhones.filter { it.stock <= 0 }
            com.smartselect.R.id.chip_stock_low -> allPhones.filter { it.stock in 1..3 }
            com.smartselect.R.id.chip_stock_in -> allPhones.filter { it.stock > 3 }
            else -> allPhones
        }
        phoneAdapter.submitList(filtered)
    }

    private fun setupOrderList() {
        orderAdapter = AdminOrderAdapter(
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
                OrderDetailDialog.newInstance(order).show(parentFragmentManager, "order_detail")
            }
        )

        binding.rvOrders.apply {
            adapter = orderAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        lifecycleScope.launch {
            orderRepository.getAllOrders().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    val orders = resource.data ?: emptyList()
                    orderRepository.checkAndCancelExpiredOrders(orders)
                    orderAdapter.submitList(orders.sortedBy { statusPriority(it.status) })

                    // Badge on Orders tab
                    val pending = orders.count { it.status == "pending" }
                    val ordersTab = binding.tabLayout.getTabAt(1)
                    if (pending > 0) {
                        ordersTab?.orCreateBadge?.apply { number = pending; isVisible = true }
                    } else { ordersTab?.removeBadge() }
                }
            }
        }
    }

    private fun setupLogsList() {
        logAdapter = AdminLogAdapter()
        binding.rvLogs.apply {
            adapter = logAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        lifecycleScope.launch {
            adminLogRepository.getLogs().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    logAdapter.submitList(resource.data ?: emptyList())
                }
            }
        }
    }

    private fun statusPriority(status: String) = when (status) {
        "pending" -> 0; "confirmed" -> 1; "picked_up" -> 2; "cancelled" -> 3; else -> 4
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}