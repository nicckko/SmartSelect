package com.smartselect.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.smartselect.data.repository.OrderRepository
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.databinding.FragmentAdminDashboardBinding
import com.smartselect.utils.Resource
import com.smartselect.utils.toPeso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var phoneRepository: PhoneRepository
    @Inject lateinit var orderRepository: OrderRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            phoneRepository.getPhones().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    val phones = resource.data ?: emptyList()
                    binding.tvTotalPhones.text = phones.size.toString()
                    val inStock = phones.count { it.stock > 0 }
                    binding.tvInStock.text = "$inStock in stock"
                }
            }
        }

        lifecycleScope.launch {
            orderRepository.getAllOrders().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    val orders = resource.data ?: emptyList()

                    // Auto-cancel expired pickup orders
                    orderRepository.checkAndCancelExpiredOrders(orders)

                    val revenue   = orders.filter {
                        it.status == "picked_up" || it.status == "confirmed"
                    }.sumOf { it.totalPrice }

                    val pending   = orders.count { it.status == "pending" }
                    val confirmed = orders.count { it.status == "confirmed" }
                    val pickedUp  = orders.count { it.status == "picked_up" }

                    binding.tvTotalOrders.text    = orders.size.toString()
                    binding.tvTotalRevenue.text   = revenue.toPeso()
                    binding.tvPendingOrders.text  = pending.toString()
                    binding.tvConfirmedOrders.text = confirmed.toString()
                    binding.tvDeliveredOrders.text = pickedUp.toString()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}