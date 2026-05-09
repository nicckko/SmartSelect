package com.smartselect.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.smartselect.R
import com.smartselect.databinding.FragmentHomeBinding
import com.smartselect.utils.Resource
import com.smartselect.utils.hide
import com.smartselect.utils.show
import com.smartselect.viewmodel.PhoneViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val phoneViewModel: PhoneViewModel by activityViewModels()
    private lateinit var phoneAdapter: PhoneAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupColumnSelector()
        setupSearch()
        setupFilters()
        observePhones()
        observeFavorites()
        observeCompare()
    }

    private fun setupRecyclerView() {
        phoneAdapter = PhoneAdapter(
            onPhoneClick = { phone ->
                phoneViewModel.setSelectedPhone(phone)
                findNavController().navigate(R.id.action_home_to_details)
            },
            onFavoriteClick = { phone ->
                phoneViewModel.toggleFavorite(phone)
            },
            onCompareClick = { phone ->
                if (phoneViewModel.isInCompare(phone.id)) {
                    phoneViewModel.removeFromCompare(phone)
                    binding.root.snackbar("${phone.model} removed from compare")
                } else {
                    val added = phoneViewModel.addToCompare(phone)
                    if (added) {
                        binding.root.snackbar("${phone.model} added to compare!", "View") {
                            findNavController().navigate(R.id.action_home_to_compare)
                        }
                    } else {
                        binding.root.snackbar("Cannot add more than 3 phones to compare")
                    }
                }
            },
            isFavorite = { phoneId -> phoneViewModel.isFavorite(phoneId) },
            isInCompare = { phoneId -> phoneViewModel.isInCompare(phoneId) }
        )
        binding.rvPhones.apply {
            adapter = phoneAdapter
            layoutManager = GridLayoutManager(requireContext(), 3)
            setHasFixedSize(true)
        }
    }

    private fun setupColumnSelector() {
        val options = listOf("1 Column", "2 Columns", "3 Columns")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        binding.actvColumns.setAdapter(adapter)
        binding.actvColumns.setText("3 Columns", false)

        binding.actvColumns.setOnItemClickListener { _, _, position, _ ->
            val columns = position + 1
            (binding.rvPhones.layoutManager as androidx.recyclerview.widget.GridLayoutManager).spanCount = columns
            phoneAdapter.notifyItemRangeChanged(0, phoneAdapter.itemCount)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val category = getSelectedCategory()
            phoneViewModel.searchPhones(text.toString(), category)
        }
    }

    private var currentCategory = ""

    private fun setupFilters() {
        val categories = listOf("All Categories", "Flagship", "Mid-range", "Gaming", "Budget")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)

        binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
            currentCategory = if (position == 0) "" else categories[position]
            val query = binding.etSearch.text.toString()
            phoneViewModel.searchPhones(query, currentCategory)
        }

        binding.btnFilter.setOnClickListener {
            FilterBottomSheet().show(childFragmentManager, "filter")
        }
    }

    private fun getSelectedCategory(): String = currentCategory

    private fun observePhones() {
        lifecycleScope.launch {
            phoneViewModel.phones.collect { resource ->
                // Check if binding is still valid before updating UI
                if (_binding != null) {
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.show()
                            binding.rvPhones.hide()
                        }
                        is Resource.Success -> {
                            binding.progressBar.hide()
                            binding.rvPhones.show()
                            phoneAdapter.submitList(resource.data)
                            binding.tvPhoneCount.text = "${resource.data?.size ?: 0} phones found"
                        }
                        is Resource.Error -> {
                            binding.progressBar.hide()
                            binding.root.snackbar(resource.message ?: "Error loading phones")
                        }
                    }
                }
            }
        }
    }

    private fun observeFavorites() {
        lifecycleScope.launch {
            phoneViewModel.favorites.collect { favorites ->
                // Check if binding is still valid before updating UI
                if (_binding != null) {
                    phoneAdapter.updateFavorites(favorites.map { it.id }.toSet())
                }
            }
        }
    }

    private fun observeCompare() {
        lifecycleScope.launch {
            phoneViewModel.compareList.collect { compareList ->
                // Check if binding is still valid before updating UI
                if (_binding != null) {
                    phoneAdapter.updateCompare(compareList.map { it.id }.toSet())
                }
            }
        }
    }

    private fun View.snackbar(msg: String, action: String? = null, onClick: (() -> Unit)? = null) {
        com.google.android.material.snackbar.Snackbar.make(this, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .apply { if (action != null && onClick != null) setAction(action) { onClick() } }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}