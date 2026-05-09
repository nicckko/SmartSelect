package com.smartselect.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.smartselect.R
import com.smartselect.databinding.FragmentFavoritesBinding
import com.smartselect.ui.home.PhoneAdapter
import com.smartselect.viewmodel.PhoneViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val phoneViewModel: PhoneViewModel by activityViewModels()
    private lateinit var adapter: PhoneAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PhoneAdapter(
            onPhoneClick = { phone ->
                phoneViewModel.setSelectedPhone(phone)
                findNavController().navigate(R.id.action_favorites_to_details)
            },
            onFavoriteClick = { phone ->
                phoneViewModel.toggleFavorite(phone)
            },
            onCompareClick = { phone ->
                // Check if phone is already in compare list
                if (phoneViewModel.isInCompare(phone.id)) {
                    // Remove from compare if already selected
                    phoneViewModel.removeFromCompare(phone)
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root, "${phone.model} removed from compare", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    // Add to compare
                    val added = phoneViewModel.addToCompare(phone)
                    if (added) {
                        com.google.android.material.snackbar.Snackbar.make(
                            binding.root, "${phone.model} added to compare!", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                        ).setAction("View") { findNavController().navigate(R.id.action_favorites_to_compare) }.show()
                    } else {
                        com.google.android.material.snackbar.Snackbar.make(
                            binding.root, "Cannot add more than 3 phones to compare", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            isFavorite = { phoneId -> phoneViewModel.isFavorite(phoneId) },
            isInCompare = { phoneId -> phoneViewModel.isInCompare(phoneId) }  // NEW: Check if phone is in compare list for highlighting
        )

        binding.rvFavorites.apply {
            this.adapter = this@FavoritesFragment.adapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            setHasFixedSize(false)
        }

        lifecycleScope.launch {
            phoneViewModel.favorites.collect { favorites ->
                adapter.submitList(favorites)
                // Fixed: update favorite icons in sync
                adapter.updateFavorites(favorites.map { it.id }.toSet())
                binding.tvEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
                binding.tvCount.text = "${favorites.size} saved phones"
            }
        }

        // NEW: Observe compare list to update highlights in favorites fragment
        lifecycleScope.launch {
            phoneViewModel.compareList.collect { compareList ->
                adapter.updateCompare(compareList.map { it.id }.toSet())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}