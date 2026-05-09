package com.smartselect.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.smartselect.R
import com.smartselect.data.model.Order
import com.smartselect.data.repository.OrderRepository
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.databinding.FragmentAdminDashboardBinding
import com.smartselect.utils.Resource
import com.smartselect.utils.toPeso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var phoneRepository: PhoneRepository
    @Inject lateinit var orderRepository: OrderRepository

    private var allOrders: List<Order> = emptyList()
    private var currentPeriod = "today"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dynamic greeting
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Good morning ☀️"
            hour < 17 -> "Good afternoon 🌤️"
            else -> "Good evening 🌙"
        }

        setupRevenueFilters()

        lifecycleScope.launch {
            phoneRepository.getPhones().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    val phones = resource.data ?: emptyList()
                    binding.tvTotalPhones.text = phones.size.toString()
                    val inStock = phones.count { it.stock > 0 }
                    binding.tvInStock.text = "✅ $inStock in stock"

                    val outOfStock = phones.count { it.stock == 0 }
                    val lowStock = phones.count { it.stock in 1..3 }
                    if (outOfStock > 0 || lowStock > 0) {
                        val parts = mutableListOf<String>()
                        if (outOfStock > 0) parts.add("⚠️ $outOfStock out of stock")
                        if (lowStock > 0) parts.add("⚠️ $lowStock low stock")
                        binding.tvStockWarning.text = parts.joinToString(" · ")
                        binding.tvStockWarning.visibility = View.VISIBLE
                    } else {
                        binding.tvStockWarning.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            orderRepository.getAllOrders().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    val orders = resource.data ?: emptyList()
                    orderRepository.checkAndCancelExpiredOrders(orders)
                    allOrders = orders
                    updateDashboard()
                }
            }
        }
    }

    private fun setupRevenueFilters() {
        val periods = listOf("Today", "This Week", "This Month", "This Year", "All Time")
        val filterAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, periods)
        binding.actvRevenueFilter.setAdapter(filterAdapter)

        binding.actvRevenueFilter.setOnItemClickListener { _, _, position, _ ->
            currentPeriod = when (position) {
                0 -> "today"
                1 -> "week"
                2 -> "month"
                3 -> "year"
                else -> "all"
            }
            updateRevenue()
        }
    }

    private fun updateDashboard() {
        val orders = allOrders
        val pending = orders.count { it.status == "pending" }
        val confirmed = orders.count { it.status == "confirmed" }
        val pickedUp = orders.count { it.status == "picked_up" }
        val cancelled = orders.count { it.status == "cancelled" }

        binding.tvTotalOrders.text = orders.size.toString()
        binding.tvPendingOrders.text = "⏳ $pending pending"
        binding.tvPendingCount.text = pending.toString()
        binding.tvConfirmedOrders.text = confirmed.toString()
        binding.tvDeliveredOrders.text = pickedUp.toString()

        // Cancellation rate
        if (orders.isNotEmpty()) {
            val rate = (cancelled.toDouble() / orders.size * 100)
            binding.tvCancellationRate.text = "❌ %.0f%% cancelled".format(rate)
            val ctx = context ?: return
            binding.tvCancellationRate.setTextColor(
                ContextCompat.getColor(ctx, if (rate > 20) R.color.error else R.color.text_tertiary)
            )
            binding.tvCancellationRate.visibility = View.VISIBLE
        }

        // Pending alert
        if (pending > 0) {
            binding.cardPendingAlert.visibility = View.VISIBLE
            binding.tvPendingAlertMsg.text = "$pending order${if (pending > 1) "s" else ""} waiting for confirmation"
        } else {
            binding.cardPendingAlert.visibility = View.GONE
        }

        updateRevenue()
    }

    private fun updateRevenue() {
        val confirmedOrders = allOrders.filter {
            it.status == "confirmed" || it.status == "picked_up"
        }

        val now = Calendar.getInstance()
        val filtered = when (currentPeriod) {
            "today" -> confirmedOrders.filter { order ->
                val ts = order.timestamp?.toDate() ?: return@filter false
                isSameDay(ts, now)
            }
            "week" -> confirmedOrders.filter { order ->
                val ts = order.timestamp?.toDate() ?: return@filter false
                isSameWeek(ts, now)
            }
            "month" -> confirmedOrders.filter { order ->
                val ts = order.timestamp?.toDate() ?: return@filter false
                isSameMonth(ts, now)
            }
            "year" -> confirmedOrders.filter { order ->
                val ts = order.timestamp?.toDate() ?: return@filter false
                isSameYear(ts, now)
            }
            else -> confirmedOrders
        }

        val revenue = filtered.sumOf { it.totalPrice }
        binding.tvTotalRevenue.text = revenue.toPeso()
        
        val label = when (currentPeriod) {
            "today" -> "Today's sales · ${filtered.size} orders"
            "week" -> "This week · ${filtered.size} orders"
            "month" -> "This month · ${filtered.size} orders"
            "year" -> "This year · ${filtered.size} orders"
            else -> "All time · ${filtered.size} orders"
        }
        binding.tvRevenueSubtitle.text = label
    }

    private fun isSameDay(date: Date, cal: Calendar): Boolean {
        val c = Calendar.getInstance().apply { time = date }
        return c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(date: Date, cal: Calendar): Boolean {
        val c = Calendar.getInstance().apply { time = date }
        return c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.WEEK_OF_YEAR) == cal.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(date: Date, cal: Calendar): Boolean {
        val c = Calendar.getInstance().apply { time = date }
        return c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
    }

    private fun isSameYear(date: Date, cal: Calendar): Boolean {
        val c = Calendar.getInstance().apply { time = date }
        return c.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}