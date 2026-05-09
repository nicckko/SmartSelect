package com.smartselect.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartselect.data.model.Phone
import com.smartselect.data.repository.AdminLogRepository
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
    @Inject lateinit var adminLogRepository: AdminLogRepository

    private lateinit var adapter: AdminPhoneAdapter

    private var allPhones: List<Phone> = emptyList()
    private var currentSearch: String = ""

    // Multi-select state
    private val selectedIds = mutableSetOf<String>()
    private var isSelectionMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminPhonesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminPhoneAdapter(
            onEdit = { phone ->
                if (!isSelectionMode)
                    AddEditPhoneDialog.newInstance(phone).show(childFragmentManager, "edit")
            },
            onDelete = { phone ->
                if (!isSelectionMode) confirmDelete(listOf(phone))
            },
            onLongPress = { phone ->
                if (!isSelectionMode) enterSelectionMode()
                toggleSelection(phone.id)
            },
            onSelectionClick = { phone ->
                if (isSelectionMode) toggleSelection(phone.id)
            },
            selectedIds = selectedIds
        )

        binding.rvPhones.apply {
            this.adapter = this@AdminPhonesFragment.adapter
            layoutManager = GridLayoutManager(requireContext(), 3)
            setHasFixedSize(true)
        }

        // Column selector
        val options = listOf("1 Column", "2 Columns", "3 Columns")
        val colAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        binding.actvAdminColumns.setAdapter(colAdapter)
        binding.actvAdminColumns.setText("3 Columns", false)
        binding.actvAdminColumns.setOnItemClickListener { _, _, position, _ ->
            val columns = position + 1
            (binding.rvPhones.layoutManager as GridLayoutManager).spanCount = columns
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }

        binding.btnAddPhone.setOnClickListener {
            AddEditPhoneDialog().show(childFragmentManager, "add")
        }

        // Select-all checkbox
        binding.cbSelectAll.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedIds.addAll(adapter.currentList.map { it.id })
            } else {
                selectedIds.clear()
            }
            updateSelectionUI()
            adapter.notifyDataSetChanged()
        }

        // Delete selected
        binding.btnDeleteSelected.setOnClickListener {
            val selected = adapter.currentList.filter { it.id in selectedIds }
            if (selected.isNotEmpty()) confirmDelete(selected)
        }

        // Cancel selection
        binding.btnCancelSelection.setOnClickListener {
            exitSelectionMode()
        }

        setupSearch()
        loadPhones()
    }

    private fun loadPhones() {
        lifecycleScope.launch {
            phoneRepository.getPhones().collect { resource ->
                if (_binding == null) return@collect
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

    private fun enterSelectionMode() {
        isSelectionMode = true
        selectedIds.clear()
        updateSelectionUI()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedIds.clear()
        binding.cbSelectAll.isChecked = false
        updateSelectionUI()
        adapter.notifyDataSetChanged()
    }

    private fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) selectedIds.remove(id)
        else selectedIds.add(id)
        if (selectedIds.isEmpty()) exitSelectionMode()
        else updateSelectionUI()
        adapter.notifyDataSetChanged()
    }

    private fun updateSelectionUI() {
        if (_binding == null) return
        val visible = if (isSelectionMode) View.VISIBLE else View.GONE
        val gone = if (isSelectionMode) View.GONE else View.VISIBLE
        binding.layoutSelectionBar.visibility = visible
        binding.btnAddPhone.visibility = gone
        binding.tvCount.text = if (isSelectionMode) "${selectedIds.size} selected" else "${allPhones.size} phones"
        binding.btnDeleteSelected.text = "Delete (${selectedIds.size})"
    }

    private fun confirmDelete(phones: List<Phone>) {
        val msg = if (phones.size == 1)
            "Move ${phones[0].brand} ${phones[0].model} to Recently Deleted?"
        else
            "Move ${phones.size} phones to Recently Deleted?"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Phone${if (phones.size > 1) "s" else ""}")
            .setMessage("$msg\n\nDeleted phones can be restored from Recently Deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    var successCount = 0
                    phones.forEach { phone ->
                        val result = phoneRepository.deletePhone(phone.id)
                        if (result is Resource.Success) {
                            successCount++
                            adminLogRepository.logAction("Deleted Phone", "Deleted ${phone.brand} ${phone.model} to Recently Deleted")
                        }
                    }
                    if (_binding == null) return@launch
                    
                    if (successCount > 0) {
                        val msg2 = if (successCount == 1) "${phones[0].model} deleted" else "$successCount phones deleted"
                        showSnackbar(msg2)
                    } else {
                        showSnackbar("Failed to delete phone(s)")
                    }
                    exitSelectionMode()
                }
            }
            .show()
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
        if (_binding == null) return
        val filtered = if (currentSearch.isEmpty()) allPhones
        else allPhones.filter { phone ->
            phone.brand.lowercase().contains(currentSearch) ||
            phone.model.lowercase().contains(currentSearch) ||
            phone.category.lowercase().contains(currentSearch)
        }
        adapter.submitList(filtered)
        if (!isSelectionMode) {
            binding.tvCount.text = if (currentSearch.isEmpty()) "${allPhones.size} phones"
            else "${filtered.size} of ${allPhones.size} phones"
        }
    }

    private fun showSnackbar(msg: String) {
        if (_binding == null) return
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
