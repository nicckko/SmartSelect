package com.smartselect.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.smartselect.R
import com.smartselect.data.model.Phone
import com.smartselect.databinding.FragmentPhoneDetailsBinding
import com.smartselect.utils.getStockColor
import com.smartselect.utils.getStockLabel
import com.smartselect.utils.toPeso
import com.smartselect.viewmodel.PhoneViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhoneDetailsFragment : Fragment() {

    private var _binding: FragmentPhoneDetailsBinding? = null
    private val binding get() = _binding!!
    private val phoneViewModel: PhoneViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPhoneDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val phone = phoneViewModel.selectedPhone.value ?: return
        bindPhone(phone)
        setupActions(phone)
    }

    private fun bindPhone(phone: Phone) {
        binding.apply {
            toolbar.title = "${phone.brand} ${phone.model}"
            toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

            tvBrand.text = phone.brand
            tvModel.text = phone.model
            // Fixed: price now uses @color/accent in layout (was @color/primary = dark navy)
            tvPrice.text = phone.price.toPeso()
            tvCategory.text = phone.category
            tvStock.text = phone.stock.getStockLabel()
            tvStock.setTextColor(ContextCompat.getColor(requireContext(), phone.stock.getStockColor()))

            // Spec rows bound directly to TextViews
            tvChipset.text = phone.chipset
            tvRam.text = phone.ram
            tvStorage.text = phone.storage
            tvCamera.text = phone.camera
            tvBattery.text = phone.battery
            tvDisplay.text = phone.display

            tvBestValue.visibility = if (phone.isBestValue) View.VISIBLE else View.GONE

            updateFavoriteBtn(phone.id)

            Glide.with(requireContext())
                .load(phone.imageUrl)
                .placeholder(R.drawable.placeholder_phone)
                .error(R.drawable.placeholder_phone)
                .into(ivPhone)
        }
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
            phoneViewModel.addToCart(phone)
            showSnackbar("${phone.model} added to cart!", "View Cart") {
                findNavController().navigate(R.id.action_details_to_orders)
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
        com.google.android.material.snackbar.Snackbar.make(binding.root, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .apply { if (action != null && onClick != null) setAction(action) { onClick() } }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
