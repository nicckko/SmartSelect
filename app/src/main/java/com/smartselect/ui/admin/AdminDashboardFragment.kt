package com.smartselect.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.smartselect.R
import com.smartselect.data.model.Order
import com.smartselect.data.model.Phone
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
    private var allPhones: List<Phone> = emptyList()
    private var currentPeriod = "today"
    private var currentBrand = ""
    private var currentCategory = ""

    // Brand options (Apple, Samsung, etc.)
    private val brands = listOf("All Brands", "Apple", "Samsung", "Infinix", "Tecno", "vivo", "Honor", "realme", "POCO", "Redmi")

    // Category options (iPhone, Android, Flagship, etc.)
    private val categories = listOf("All Categories", "iPhone", "Android", "Flagship", "Mid-range", "Budget", "Gaming")

    // Brand-to-category relationship
    private val brandCategories = mapOf(
        "Apple" to listOf("iPhone"),
        "Samsung" to listOf("Android"),
        "Infinix" to listOf("Android"),
        "Tecno" to listOf("Android"),
        "vivo" to listOf("Android"),
        "Honor" to listOf("Android"),
        "realme" to listOf("Android"),
        "POCO" to listOf("Android"),
        "Redmi" to listOf("Android")
    )

    // Category-to-brand relationship
    private val categoryBrands = mapOf(
        "iPhone" to listOf("Apple"),
        "Android" to listOf("Samsung", "Infinix", "Tecno", "vivo", "Honor", "realme", "POCO", "Redmi"),
        "Flagship" to listOf("Apple", "Samsung", "vivo", "Honor", "realme", "POCO"),
        "Mid-range" to listOf("Samsung", "Infinix", "Tecno", "vivo", "Honor", "realme", "POCO", "Redmi"),
        "Budget" to listOf("Infinix", "Tecno", "Redmi"),
        "Gaming" to listOf("POCO", "Infinix", "vivo")
    )

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
        loadPhones()

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

    private fun loadPhones() {
        lifecycleScope.launch {
            phoneRepository.getPhones().collect { resource ->
                if (_binding == null) return@collect
                if (resource is Resource.Success) {
                    allPhones = resource.data ?: emptyList()

                    binding.tvTotalPhones.text = allPhones.size.toString()
                    val totalStock = allPhones.sumOf { it.stock }
                    binding.tvTotalStock.text = "📦 $totalStock total units"
                    val inStock = allPhones.count { it.stock > 0 }
                    binding.tvInStock.text = "✅ $inStock in stock"

                    val outOfStock = allPhones.count { it.stock == 0 }
                    val lowStock = allPhones.count { it.stock in 1..3 }
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
    }

    private fun setupRevenueFilters() {
        // Time period filter
        val periods = listOf("Today", "This Week", "This Month", "This Year", "All Time")
        val periodAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, periods)
        binding.actvRevenueFilter.setAdapter(periodAdapter)
        binding.actvRevenueFilter.setText("Today", false)

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

        // Category filter (First)
        val categoryAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategoryFilter.setAdapter(categoryAdapter)
        binding.actvCategoryFilter.setText("All Categories", false)

        binding.actvCategoryFilter.setOnItemClickListener { _, _, position, _ ->
            currentCategory = if (position == 0) "" else categories[position]
            // When category changes, filter the brand dropdown options
            updateBrandDropdown()
            updateRevenue()
        }

        // Brand filter (Second)
        val brandAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, brands)
        binding.actvBrandFilter.setAdapter(brandAdapter)
        binding.actvBrandFilter.setText("All Brands", false)

        binding.actvBrandFilter.setOnItemClickListener { _, _, position, _ ->
            currentBrand = if (position == 0) "" else brands[position]
            // When brand changes, filter the category dropdown options
            updateCategoryDropdown()
            updateRevenue()
        }
    }

    private fun updateBrandDropdown() {
        val filteredBrands = if (currentCategory.isNotEmpty()) {
            // Get brands that match the selected category
            categoryBrands[currentCategory] ?: listOf("All Brands")
        } else {
            brands
        }

        val displayBrands = listOf("All Brands") + filteredBrands.filter { it != "All Brands" }
        val brandAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayBrands)
        binding.actvBrandFilter.setAdapter(brandAdapter)

        // Reset brand selection if current brand is not in filtered list
        if (currentBrand.isNotEmpty() && !displayBrands.contains(currentBrand)) {
            currentBrand = ""
            binding.actvBrandFilter.setText("All Brands", false)
        }
    }

    private fun updateCategoryDropdown() {
        val filteredCategories = if (currentBrand.isNotEmpty()) {
            // Get categories that match the selected brand
            brandCategories[currentBrand] ?: listOf("All Categories")
        } else {
            categories
        }

        val displayCategories = listOf("All Categories") + filteredCategories.filter { it != "All Categories" }
        val categoryAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayCategories)
        binding.actvCategoryFilter.setAdapter(categoryAdapter)

        // Reset category selection if current category is not in filtered list
        if (currentCategory.isNotEmpty() && !displayCategories.contains(currentCategory)) {
            currentCategory = ""
            binding.actvCategoryFilter.setText("All Categories", false)
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
        // Get completed orders (confirmed or picked up)
        var completedOrders = allOrders.filter {
            it.status == "confirmed" || it.status == "picked_up"
        }

        // Apply brand and category filters
        completedOrders = completedOrders.filter { order ->
            var matches = true

            // Get phones in this order
            val orderPhones = order.phoneIds.mapNotNull { phoneId ->
                allPhones.find { it.id == phoneId }
            }

            // Filter by brand
            if (matches && currentBrand.isNotEmpty()) {
                val hasBrand = orderPhones.any { it.brand == currentBrand }
                if (!hasBrand) matches = false
            }

            // Filter by category
            if (matches && currentCategory.isNotEmpty()) {
                val hasCategory = orderPhones.any { it.category == currentCategory }
                if (!hasCategory) matches = false
            }

            matches
        }

        // Apply time period filter
        val now = Calendar.getInstance()
        val filtered = when (currentPeriod) {
            "today" -> completedOrders.filter { order ->
                val ts = order.timestamp?.toDate() ?: return@filter false
                isSameDay(ts, now)
            }
            "week" -> completedOrders.filter { order ->
                val ts = order.timestamp?.toDate() ?: return@filter false
                isSameWeek(ts, now)
            }
            "month" -> completedOrders.filter { order ->
                val ts = order.timestamp?.toDate() ?: return@filter false
                isSameMonth(ts, now)
            }
            "year" -> completedOrders.filter { order ->
                val ts = order.timestamp?.toDate() ?: return@filter false
                isSameYear(ts, now)
            }
            else -> completedOrders
        }

        val revenue = filtered.sumOf { it.totalPrice }
        binding.tvTotalRevenue.text = revenue.toPeso()

        // Build subtitle with filter info
        val filterParts = mutableListOf<String>()

        val periodLabel = when (currentPeriod) {
            "today" -> "Today"
            "week" -> "This week"
            "month" -> "This month"
            "year" -> "This year"
            else -> "All time"
        }
        filterParts.add(periodLabel)

        if (currentCategory.isNotEmpty()) {
            filterParts.add(currentCategory)
        }
        if (currentBrand.isNotEmpty()) {
            filterParts.add(currentBrand)
        }

        val filterText = filterParts.joinToString(" · ")
        binding.tvRevenueSubtitle.text = "$filterText · ${filtered.size} orders"
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