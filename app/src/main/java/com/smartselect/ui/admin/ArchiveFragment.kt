package com.smartselect.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartselect.R
import com.smartselect.data.model.Phone
import com.smartselect.data.repository.AdminLogRepository
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.databinding.FragmentArchiveBinding
import com.smartselect.databinding.ItemArchivedPhoneBinding
import com.smartselect.utils.Resource
import com.smartselect.utils.toPeso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ArchiveFragment : Fragment() {

    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var phoneRepository: PhoneRepository
    @Inject lateinit var adminLogRepository: AdminLogRepository

    private lateinit var archiveAdapter: ArchivedPhoneAdapter
    private var allArchived: List<Phone> = emptyList()

    private val selectedIds = mutableSetOf<String>()
    private var isSelectionMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        archiveAdapter = ArchivedPhoneAdapter(
            onRestore = { phone ->
                if (!isSelectionMode) confirmRestore(listOf(phone))
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

        binding.rvArchived.apply {
            adapter = archiveAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Selection Handlers
        binding.cbSelectAll.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedIds.addAll(archiveAdapter.currentList.map { it.id })
            } else {
                selectedIds.clear()
            }
            updateSelectionUI()
            archiveAdapter.notifyDataSetChanged()
        }

        binding.btnRestoreSelected.setOnClickListener {
            val selected = archiveAdapter.currentList.filter { it.id in selectedIds }
            if (selected.isNotEmpty()) confirmRestore(selected)
        }

        binding.btnDeleteSelected.setOnClickListener {
            val selected = archiveAdapter.currentList.filter { it.id in selectedIds }
            if (selected.isNotEmpty()) confirmDelete(selected)
        }

        binding.btnCancelSelection.setOnClickListener {
            exitSelectionMode()
        }

        lifecycleScope.launch {
            phoneRepository.getDeletedPhones().collect { resource ->
                if (_binding == null) return@collect
                when (resource) {
                    is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        allArchived = resource.data ?: emptyList()
                        archiveAdapter.submitList(allArchived)
                        binding.layoutEmpty.visibility = if (allArchived.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvArchived.visibility = if (allArchived.isEmpty()) View.GONE else View.VISIBLE
                        if (!isSelectionMode) {
                            binding.tvCount.text = "${allArchived.size} archived phone${if (allArchived.size != 1) "s" else ""}"
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Snackbar.make(binding.root, resource.message ?: "Error", Snackbar.LENGTH_SHORT).show()
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
        archiveAdapter.notifyDataSetChanged()
    }

    private fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) selectedIds.remove(id)
        else selectedIds.add(id)
        if (selectedIds.isEmpty()) exitSelectionMode()
        else updateSelectionUI()
        archiveAdapter.notifyDataSetChanged()
    }

    private fun updateSelectionUI() {
        if (_binding == null) return
        binding.layoutSelectionBar.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        if (isSelectionMode) {
            binding.tvCount.text = "${selectedIds.size} selected"
            binding.btnDeleteSelected.text = "Del (${selectedIds.size})"
            binding.btnRestoreSelected.text = "Restore (${selectedIds.size})"
        } else {
            binding.tvCount.text = "${allArchived.size} recently deleted phone${if (allArchived.size != 1) "s" else ""}"
        }
    }

    private fun confirmRestore(phones: List<Phone>) {
        val msg = if (phones.size == 1) "Restore ${phones[0].brand} ${phones[0].model}?" else "Restore ${phones.size} phones?"
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Restore Phone")
            .setMessage("$msg\n\nThey will be active in the inventory again.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore") { _, _ ->
                lifecycleScope.launch {
                    phones.forEach { phone ->
                        phoneRepository.restorePhone(phone.id)
                        adminLogRepository.logAction("Phone Restored", "Restored ${phone.brand} ${phone.model} from recently deleted")
                    }
                    if (_binding == null) return@launch
                    val snackMsg = if (phones.size == 1) "${phones[0].model} restored" else "${phones.size} phones restored"
                    Snackbar.make(binding.root, snackMsg, Snackbar.LENGTH_SHORT).show()
                    exitSelectionMode()
                }
            }
            .show()
    }

    private fun confirmDelete(phones: List<Phone>) {
        val msg = if (phones.size == 1) "Permanently delete ${phones[0].brand} ${phones[0].model}?" else "Permanently delete ${phones.size} phones?"
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Permanent Delete")
            .setMessage("$msg\n\nThis action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete Forever") { _, _ ->
                lifecycleScope.launch {
                    phones.forEach { phone ->
                        phoneRepository.permanentlyDeletePhone(phone.id)
                        adminLogRepository.logAction("Phone Deleted", "Permanently deleted ${phone.brand} ${phone.model}")
                    }
                    if (_binding == null) return@launch
                    val snackMsg = if (phones.size == 1) "${phones[0].model} deleted forever" else "${phones.size} phones deleted forever"
                    Snackbar.make(binding.root, snackMsg, Snackbar.LENGTH_SHORT).show()
                    exitSelectionMode()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ArchivedPhoneAdapter(
    private val onRestore: (Phone) -> Unit,
    private val onDelete: (Phone) -> Unit,
    private val onLongPress: ((Phone) -> Unit)? = null,
    private val onSelectionClick: ((Phone) -> Unit)? = null,
    private val selectedIds: Set<String>? = null
) : ListAdapter<Phone, ArchivedPhoneAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemArchivedPhoneBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(phone: Phone) {
            binding.tvName.text = "${phone.brand} ${phone.model}"
            binding.tvPrice.text = phone.price.toPeso()
            Glide.with(binding.root.context)
                .load(phone.imageUrl)
                .placeholder(R.drawable.placeholder_phone)
                .into(binding.ivPhone)

            val isSelected = selectedIds?.contains(phone.id) == true
            binding.root.alpha = if (isSelected) 0.7f else 1f
            binding.root.strokeWidth = if (isSelected) 3 else 1
            binding.root.strokeColor = ContextCompat.getColor(
                binding.root.context,
                if (isSelected) R.color.accent else R.color.divider
            )

            val inSelectionMode = selectedIds != null && selectedIds.isNotEmpty()
            if (inSelectionMode) {
                binding.root.setOnClickListener { onSelectionClick?.invoke(phone) }
                binding.btnRestore.visibility = View.GONE
            } else {
                binding.root.setOnClickListener(null)
                binding.btnRestore.visibility = View.VISIBLE
                binding.btnRestore.setOnClickListener { onRestore(phone) }
                
                // Add an explicit delete button via a long-press listener as fallback,
                // or just trigger the long-press event for selection mode.
            }

            binding.root.setOnLongClickListener {
                onLongPress?.invoke(phone)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemArchivedPhoneBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Phone>() {
        override fun areItemsTheSame(a: Phone, b: Phone) = a.id == b.id
        override fun areContentsTheSame(a: Phone, b: Phone) = a == b
    }
}
