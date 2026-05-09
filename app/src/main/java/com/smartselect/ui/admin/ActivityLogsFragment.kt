package com.smartselect.ui.admin

import android.graphics.Color
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartselect.R
import com.smartselect.data.model.AdminLog
import com.smartselect.data.repository.AdminLogRepository
import com.smartselect.databinding.FragmentActivityLogsBinding
import com.smartselect.databinding.ItemActivityLogBinding
import com.smartselect.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ActivityLogsFragment : Fragment() {

    private var _binding: FragmentActivityLogsBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var adminLogRepository: AdminLogRepository

    private lateinit var logAdapter: ActivityLogAdapter
    private var allLogs: List<AdminLog> = emptyList()

    private val selectedIds = mutableSetOf<String>()
    private var isSelectionMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivityLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        logAdapter = ActivityLogAdapter(
            onLongPress = { log ->
                if (!isSelectionMode) enterSelectionMode()
                toggleSelection(log.logId)
            },
            onSelectionClick = { log ->
                if (isSelectionMode) toggleSelection(log.logId)
            },
            selectedIds = selectedIds
        )

        binding.rvLogs.apply {
            adapter = logAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Selection Handlers
        binding.cbSelectAll.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedIds.addAll(logAdapter.currentList.map { it.logId })
            } else {
                selectedIds.clear()
            }
            updateSelectionUI()
            logAdapter.notifyDataSetChanged()
        }

        binding.btnDeleteSelected.setOnClickListener {
            val selected = logAdapter.currentList.filter { it.logId in selectedIds }
            if (selected.isNotEmpty()) confirmDelete(selected)
        }

        binding.btnCancelSelection.setOnClickListener {
            exitSelectionMode()
        }

        lifecycleScope.launch {
            adminLogRepository.getLogs().collect { resource ->
                if (_binding == null) return@collect
                when (resource) {
                    is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        allLogs = resource.data ?: emptyList()
                        logAdapter.submitList(allLogs)
                        binding.layoutEmpty.visibility = if (allLogs.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvLogs.visibility = if (allLogs.isEmpty()) View.GONE else View.VISIBLE
                        if (!isSelectionMode) {
                            binding.tvCount.text = "${allLogs.size} event${if (allLogs.size != 1) "s" else ""} logged"
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
        logAdapter.notifyDataSetChanged()
    }

    private fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) selectedIds.remove(id)
        else selectedIds.add(id)
        if (selectedIds.isEmpty()) exitSelectionMode()
        else updateSelectionUI()
        logAdapter.notifyDataSetChanged()
    }

    private fun updateSelectionUI() {
        if (_binding == null) return
        binding.layoutSelectionBar.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        if (isSelectionMode) {
            binding.tvCount.text = "${selectedIds.size} selected"
            binding.btnDeleteSelected.text = "Del (${selectedIds.size})"
        } else {
            binding.tvCount.text = "${allLogs.size} event${if (allLogs.size != 1) "s" else ""} logged"
        }
    }

    private fun confirmDelete(logs: List<AdminLog>) {
        val msg = if (logs.size == 1) "Delete this log entry?" else "Delete ${logs.size} log entries?"
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Delete Logs")
            .setMessage("$msg\n\nThis action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    logs.forEach { log ->
                        adminLogRepository.deleteLog(log.logId)
                    }
                    if (_binding == null) return@launch
                    val snackMsg = if (logs.size == 1) "Log deleted" else "${logs.size} logs deleted"
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

class ActivityLogAdapter(
    private val onLongPress: ((AdminLog) -> Unit)? = null,
    private val onSelectionClick: ((AdminLog) -> Unit)? = null,
    private val selectedIds: Set<String>? = null
) : ListAdapter<AdminLog, ActivityLogAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemActivityLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(log: AdminLog) {
            binding.tvAction.text = log.action
            binding.tvDetails.text = log.details

            // Icon based on action type
            val icon = when {
                log.action.contains("Add", true) -> "➕"
                log.action.contains("Edit", true) || log.action.contains("Update", true) -> "✏️"
                log.action.contains("Delete", true) -> "🗑️"
                log.action.contains("Restore", true) -> "♻️"
                log.action.contains("Order", true) || log.action.contains("Status", true) -> "📦"
                log.action.contains("Seed", true) -> "🌱"
                log.action.contains("Login", true) -> "🔑"
                else -> "📝"
            }
            binding.tvIcon.text = icon

            // Time formatting
            log.timestamp?.toDate()?.let { date ->
                val diff = System.currentTimeMillis() - date.time
                val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
                val hrs = TimeUnit.MILLISECONDS.toHours(diff)
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                val relative = when {
                    mins < 1 -> "Just now"
                    mins < 60 -> "${mins}m ago"
                    hrs < 24 -> "${hrs}h ago"
                    days < 2 -> "Yesterday"
                    days < 7 -> "${days}d ago"
                    else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                }
                val full = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
                binding.tvTime.text = "$relative · $full"
            } ?: run {
                binding.tvTime.text = "—"
            }

            // Selection styling
            val isSelected = selectedIds?.contains(log.logId) == true
            binding.root.alpha = if (isSelected) 0.7f else 1f
            if (isSelected) {
                binding.root.setBackgroundColor(Color.parseColor("#1A3B82F6")) // Light tint
            } else {
                binding.root.setBackgroundColor(Color.TRANSPARENT)
            }

            // Interaction
            val inSelectionMode = selectedIds != null && selectedIds.isNotEmpty()
            if (inSelectionMode) {
                binding.root.setOnClickListener { onSelectionClick?.invoke(log) }
            } else {
                binding.root.setOnClickListener(null)
            }

            binding.root.setOnLongClickListener {
                onLongPress?.invoke(log)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemActivityLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<AdminLog>() {
        override fun areItemsTheSame(a: AdminLog, b: AdminLog) = a.logId == b.logId
        override fun areContentsTheSame(a: AdminLog, b: AdminLog) = a == b
    }
}
