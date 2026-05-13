package com.smartselect.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.smartselect.R
import com.smartselect.data.model.Phone
import com.smartselect.databinding.FragmentPhoneDetailsBinding
import com.smartselect.utils.GlideImageLoader
import com.smartselect.utils.getStockColor
import com.smartselect.utils.getStockLabel
import com.smartselect.utils.toPeso
import com.smartselect.viewmodel.PhoneViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PhoneDetailsFragment : Fragment() {

    private var _binding: FragmentPhoneDetailsBinding? = null
    private val binding get() = _binding!!
    private val phoneViewModel: PhoneViewModel by activityViewModels()

    private var currentQuantity = 1
    private var currentPhone: Phone? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPhoneDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val phone = phoneViewModel.selectedPhone.value ?: return
        currentPhone = phone
        bindPhone(phone)
        setupQuantitySelector(phone)
        setupActions(phone)
    }

    private fun bindPhone(phone: Phone) {
        binding.apply {
            toolbar.title = "${phone.brand} ${phone.model}"
            toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

            tvBrand.text = phone.brand
            tvModel.text = phone.model
            tvPrice.text = phone.price.toPeso()
            tvCategory.text = phone.category
            tvStock.text = phone.stock.getStockLabel()
            tvStock.setTextColor(ContextCompat.getColor(requireContext(), phone.stock.getStockColor()))

            // Core Specs
            tvChipset.text = phone.chipset
            tvRam.text = phone.ram
            tvStorage.text = phone.storage
            tvCamera.text = phone.camera
            tvBattery.text = phone.battery
            tvDisplay.text = phone.display

            // New GSMArena Specs with empty handling
            setSpecValue(binding.layoutOsNetwork, tvOs, phone.os)
            setSpecValue(binding.layoutWeightDimensions, tvWeight, phone.weight)
            setSpecValue(binding.layoutWeightDimensions, tvDimensions, phone.dimensions)
            setSpecValue(binding.layoutBuildProtection, tvBuild, phone.build)
            setSpecValue(binding.layoutBuildProtection, tvProtection, phone.protection)
            setSpecValue(binding.layoutGpuCharging, tvGpu, phone.gpu)
            setSpecValue(binding.layoutGpuCharging, tvCharging, phone.charging)
            setSpecValue(binding.layoutSensorsColors, tvSensors, phone.sensors)
            setSpecValue(binding.layoutSensorsColors, tvColors, phone.colors)
            setSpecValue(binding.layoutReleaseDate, tvReleaseDate, phone.releaseDate)

            tvBestValue.visibility = if (phone.isBestValue) View.VISIBLE else View.GONE

            // Handle OS + Network row
            showSpecRow(binding.layoutOsNetwork, tvOs, phone.os, tvNetwork, phone.network)

            // Handle Weight + Dimensions row
            showSpecRow(binding.layoutWeightDimensions, tvWeight, phone.weight, tvDimensions, phone.dimensions)

            // Handle Build + Protection row
            showSpecRow(binding.layoutBuildProtection, tvBuild, phone.build, tvProtection, phone.protection)

            // Handle GPU + Charging row
            showSpecRow(binding.layoutGpuCharging, tvGpu, phone.gpu, tvCharging, phone.charging)

            // Handle Sensors + Colors row
            showSpecRow(binding.layoutSensorsColors, tvSensors, phone.sensors, tvColors, phone.colors)

            // Handle Release Date row (single value)
            if (phone.releaseDate.isNotEmpty()) {
                tvReleaseDate.text = phone.releaseDate
                binding.layoutReleaseDate.visibility = View.VISIBLE
            } else {
                binding.layoutReleaseDate.visibility = View.GONE
            }

            updateFavoriteBtn(phone.id)

            GlideImageLoader.loadImage(requireContext(), phone.imageUrl, binding.ivPhone)
        }
    }

    private fun setSpecValue(layout: View, textView: TextView, value: String) {
        if (value.isNotEmpty()) {
            textView.text = value
            layout.visibility = View.VISIBLE
        } else {
            layout.visibility = View.GONE
        }
    }

    private fun setSpecValue(textView: TextView, value: String) {
        if (value.isNotEmpty()) {
            textView.text = value
            // Make parent layout visible if it was hidden
            (textView.parent as? View)?.visibility = View.VISIBLE
        } else {
            // Hide the entire spec row if value is empty
            (textView.parent as? View)?.visibility = View.GONE
        }
    }

    private fun setupQuantitySelector(phone: Phone) {
        currentQuantity = 1
        binding.tvQuantity.text = "1"
        updateStockInfo(phone)

        binding.btnDecrease.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                binding.tvQuantity.text = currentQuantity.toString()
                updateStockInfo(phone)
            }
        }

        binding.btnIncrease.setOnClickListener {
            if (currentQuantity < phone.stock) {
                currentQuantity++
                binding.tvQuantity.text = currentQuantity.toString()
                updateStockInfo(phone)
            } else {
                Snackbar.make(binding.root, "Only ${phone.stock} available in stock", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStockInfo(phone: Phone) {
        val stockText = when {
            phone.stock <= 0 -> "❌ Out of stock"
            phone.stock <= 3 -> "⚠️ Only ${phone.stock} left in stock"
            else -> "✓ ${phone.stock} available"
        }
        binding.tvStockInfo.text = stockText

        val stockColor = when {
            phone.stock <= 0 -> R.color.error
            phone.stock <= 3 -> R.color.warning
            else -> R.color.success
        }
        binding.tvStockInfo.setTextColor(ContextCompat.getColor(requireContext(), stockColor))

        // Disable increase button if at max stock
        binding.btnIncrease.isEnabled = currentQuantity < phone.stock
        binding.btnIncrease.alpha = if (currentQuantity < phone.stock) 1.0f else 0.5f

        // Update button text to show quantity
        binding.btnOrder.text = "Add to Cart ($currentQuantity)  🛒"
    }

    private fun setupActions(phone: Phone) {
        binding.btnFavorite.setOnClickListener {
            phoneViewModel.toggleFavorite(phone)
            updateFavoriteBtn(phone.id)
        }

        binding.btnCompare.setOnClickListener {
            val added = phoneViewModel.addToCompare(phone)
            if (added) {
                showSnackbar("${phone.model} added to compare!", "View Compare") {
                    findNavController().navigate(R.id.action_details_to_compare)
                }
            } else {
                showSnackbar("Already in compare list (max 3)")
            }
        }

        binding.btnOrder.setOnClickListener {
            if (phone.stock <= 0) {
                showSnackbar("This phone is out of stock!")
                return@setOnClickListener
            }
            if (currentQuantity > phone.stock) {
                showSnackbar("Only ${phone.stock} available in stock")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val success = phoneViewModel.checkStockAndAddToCart(phone, currentQuantity)
                if (success) {
                    showSnackbar("${currentQuantity} x ${phone.model} added to cart!", "View Cart") {
                        findNavController().navigate(R.id.action_details_to_orders)
                    }
                } else {
                    // Error message already shown by ViewModel
                }
            }
        }
    }

    private fun updateFavoriteBtn(phoneId: String) {
        val isFav = phoneViewModel.isFavorite(phoneId)
        binding.btnFavorite.apply {
            text = if (isFav) "Unfavourite" else "Favourite"
            setIconResource(if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        }
    }

    private fun showSnackbar(msg: String, action: String? = null, onClick: (() -> Unit)? = null) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT)
            .apply { if (action != null && onClick != null) setAction(action) { onClick() } }
            .show()
    }

    private fun showSpecRow(layout: View, textView1: TextView, value1: String, textView2: TextView, value2: String) {
        val hasValue1 = value1.isNotEmpty()
        val hasValue2 = value2.isNotEmpty()

        if (hasValue1) textView1.text = value1
        if (hasValue2) textView2.text = value2

        layout.visibility = if (hasValue1 || hasValue2) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}