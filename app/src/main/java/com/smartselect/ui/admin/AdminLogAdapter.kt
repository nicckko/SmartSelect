package com.smartselect.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartselect.data.model.AdminLog
import com.smartselect.databinding.ItemAdminLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdminLogAdapter : ListAdapter<AdminLog, AdminLogAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemAdminLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(log: AdminLog) {
            binding.tvLogAction.text = log.action
            binding.tvLogDetails.text = log.details
            binding.tvAdminName.text = "By ${log.adminName}"
            
            // Icon logic based on action
            val icon = when {
                log.action.contains("Order", true) -> "📦"
                log.action.contains("Delete", true) || log.action.contains("Cancel", true) -> "❌"
                log.action.contains("Add", true) -> "➕"
                log.action.contains("Update", true) -> "✏️"
                else -> "📝"
            }
            binding.tvLogIcon.text = icon
            
            log.timestamp?.toDate()?.let { date ->
                binding.tvLogTime.text = getRelativeTime(date)
            } ?: run {
                binding.tvLogTime.text = ""
            }
        }
        
        private fun getRelativeTime(date: Date): String {
            val diff = System.currentTimeMillis() - date.time
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hrs = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            return when {
                mins < 1 -> "Just now"
                mins < 60 -> "${mins}m ago"
                hrs < 24 -> "${hrs}h ago"
                days < 2 -> "Yesterday"
                days < 7 -> "${days}d ago"
                else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAdminLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<AdminLog>() {
        override fun areItemsTheSame(a: AdminLog, b: AdminLog) = a.logId == b.logId
        override fun areContentsTheSame(a: AdminLog, b: AdminLog) = a == b
    }
}
