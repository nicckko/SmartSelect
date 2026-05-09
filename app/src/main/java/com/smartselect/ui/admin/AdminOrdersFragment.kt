package com.smartselect.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
    private lateinit var adapter: AdminOrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminOrderAdapter(
            onStatusChange = { order, status ->
                lifecycleScope.launch {
                    orderRepository.updateOrderStatus(order.orderId, status)
                }
            }
        )

        binding.rvOrders.apply {
            this.adapter = this@AdminOrdersFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        lifecycleScope.launch {
            orderRepository.getAllOrders().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    val orders = resource.data ?: emptyList()

                    // Auto-cancel any expired orders
                    orderRepository.checkAndCancelExpiredOrders(orders)

                    // Sort: pending first, then confirmed, then others
                    val sorted = orders.sortedWith(
                        compareBy(
                            { statusPriority(it.status) },
                            { it.timestamp?.seconds ?: 0L }
                        )
                    )

                    adapter.submitList(sorted)

                    val pending   = orders.count { it.status == "pending" }
                    val confirmed = orders.count { it.status == "confirmed" }
                    val cancelled = orders.count { it.status == "cancelled" }

                    binding.tvOrderCount.text = "${orders.size} total"
                    binding.tvPendingCount.text =
                        "⏳ $pending pending  ·  ✅ $confirmed confirmed  ·  ❌ $cancelled cancelled"
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