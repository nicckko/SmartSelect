package com.smartselect.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartselect.data.model.Phone
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.databinding.FragmentAdminPhonesBinding
import com.smartselect.utils.Resource
import com.smartselect.utils.hide
import com.smartselect.utils.show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AdminPhonesFragment : Fragment() {

    private var _binding: FragmentAdminPhonesBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var phoneRepository: PhoneRepository
    private lateinit var adapter: AdminPhoneAdapter

    private var allPhones: List<Phone> = emptyList()
    private var currentSearch: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminPhonesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminPhoneAdapter(
            onEdit = { phone -> AddEditPhoneDialog.newInstance(phone).show(childFragmentManager, "edit") },
            onDelete = { phone ->
                // Confirm-before-delete dialog (item 19)
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Phone")
                    .setMessage("Are you sure you want to delete ${phone.brand} ${phone.model}?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            phoneRepository.deletePhone(phone.id)
                            showSnackbar("${phone.model} deleted")
                        }
                    }
                    .show()
            }
        )

        binding.rvPhones.apply {
            this.adapter = this@AdminPhonesFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
        }

        binding.btnAddPhone.setOnClickListener {
            AddEditPhoneDialog().show(childFragmentManager, "add")
        }

        setupSearch()

        lifecycleScope.launch {
            phoneRepository.getPhones().collect { resource ->
                when (resource) {
                    is Resource.Loading -> binding.progressBar.show()
                    is Resource.Success -> {
                        binding.progressBar.hide()
                        allPhones = resource.data ?: emptyList()
                        applySearch()
                        binding.tvEmpty.visibility = if (allPhones.isEmpty()) View.VISIBLE else View.GONE
                    }
                    is Resource.Error -> {
                        binding.progressBar.hide()
                        showSnackbar(resource.message ?: "Error loading phones")
                    }
                }
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearch = s?.toString()?.trim()?.lowercase() ?: ""
                applySearch()
            }
        })
    }

    private fun applySearch() {
        val filtered = if (currentSearch.isEmpty()) allPhones
        else allPhones.filter { phone ->
            phone.brand.lowercase().contains(currentSearch) ||
            phone.model.lowercase().contains(currentSearch) ||
            phone.category.lowercase().contains(currentSearch)
        }
        adapter.submitList(filtered)
        // Live phone count (item 18)
        binding.tvCount.text = if (currentSearch.isEmpty()) "${allPhones.size} phones"
        else "${filtered.size} of ${allPhones.size} phones"
    }

    private fun showSnackbar(msg: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
