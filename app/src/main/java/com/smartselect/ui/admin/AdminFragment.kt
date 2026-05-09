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

    private lateinit var phoneAdapter: AdminPhoneAdapter
    private lateinit var orderAdapter: AdminOrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPhoneList()
        setupOrderList()
        setupTabs()
        setupButtons()
    }

    private fun setupButtons() {
        // ✅ Add Phone — opens the BottomSheet dialog
        binding.btnAddPhone.setOnClickListener {
            AddEditPhoneDialog().show(parentFragmentManager, "add_phone")
        }


    }

    private fun setupTabs() {
        // Default: show phones tab
        showPhones()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> showPhones()
                    1 -> showOrders()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showPhones() {
        binding.rvPhones.visibility = View.VISIBLE
        binding.rvOrders.visibility = View.GONE
        binding.btnAddPhone.visibility = View.VISIBLE
        binding.btnSeedData.visibility = View.VISIBLE
    }

    private fun showOrders() {
        binding.rvPhones.visibility = View.GONE
        binding.rvOrders.visibility = View.VISIBLE
        binding.btnAddPhone.visibility = View.GONE
        binding.btnSeedData.visibility = View.GONE
    }

    private fun setupPhoneList() {
        phoneAdapter = AdminPhoneAdapter(
            onEdit = { phone ->
                AddEditPhoneDialog.newInstance(phone)
                    .show(parentFragmentManager, "edit_phone")
            },
            onDelete = { phone ->
                lifecycleScope.launch {
                    phoneRepository.deletePhone(phone.id)
                    Snackbar.make(
                        binding.root,
                        "${phone.brand} ${phone.model} deleted",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        )

        binding.rvPhones.apply {
            adapter = phoneAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
        }

        lifecycleScope.launch {
            phoneRepository.getPhones().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    phoneAdapter.submitList(resource.data ?: emptyList())
                }
            }
        }
    }

    private fun setupOrderList() {
        orderAdapter = AdminOrderAdapter(
            onStatusChange = { order, status ->
                lifecycleScope.launch {
                    orderRepository.updateOrderStatus(order.orderId, status)
                }
            }
        )

        binding.rvOrders.apply {
            adapter = orderAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
        }

        lifecycleScope.launch {
            orderRepository.getAllOrders().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    val orders = resource.data ?: emptyList()
                    // Auto-cancel expired pickup orders
                    orderRepository.checkAndCancelExpiredOrders(orders)
                    orderAdapter.submitList(
                        orders.sortedBy { statusPriority(it.status) }
                    )
                }
            }
        }
    }

    private fun statusPriority(status: String) = when (status) {
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