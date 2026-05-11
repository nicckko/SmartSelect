package com.smartselect.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartselect.R
import com.smartselect.data.model.getFullName
import com.smartselect.databinding.FragmentProfileBinding
import com.smartselect.ui.auth.LoginActivity
import com.smartselect.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fixed: restore dark mode switch state from persisted preference
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        binding.switchDarkMode.isChecked = isDark

        lifecycleScope.launch {
            authViewModel.currentUser.collect { user ->
                if (_binding == null) return@collect
                user?.let {
                    binding.tvName.text = it.getFullName()
                    binding.tvEmail.text = it.email
                    binding.tvRole.text = if (it.role == "admin") "👑 Admin" else "👤 Customer"
                    binding.btnAdminPanel.visibility =
                        if (it.role == "admin") View.VISIBLE else View.GONE

                    val orderHistoryLayout =
                        binding.root.findViewById<ViewGroup>(R.id.order_history_layout)
                    val adminLogsLayout =
                        binding.root.findViewById<ViewGroup>(R.id.admin_logs_layout)
                    val adminArchiveLayout =
                        binding.root.findViewById<ViewGroup>(R.id.admin_archive_layout)
                    if (it.role == "admin") {
                        orderHistoryLayout?.visibility = View.GONE
                        adminLogsLayout?.visibility = View.VISIBLE
                        adminArchiveLayout?.visibility = View.VISIBLE
                    } else {
                        orderHistoryLayout?.visibility = View.VISIBLE
                        adminLogsLayout?.visibility = View.GONE
                        adminArchiveLayout?.visibility = View.GONE
                    }

                    if (it.profilePictureUrl.isNotEmpty()) {
                        binding.tvAvatar.visibility = View.GONE
                        val ivAvatar =
                            binding.root.findViewById<android.widget.ImageView>(R.id.iv_avatar)
                        if (ivAvatar != null) {
                            ivAvatar.visibility = View.VISIBLE
                            com.bumptech.glide.Glide.with(requireContext())
                                .load(it.profilePictureUrl).centerCrop().into(ivAvatar)
                        }
                    } else {
                        binding.tvAvatar.visibility = View.VISIBLE
                        // FIXED: Changed it.name to it.getFullName()
                        binding.tvAvatar.text =
                            it.getFullName().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    }

                    binding.btnEditProfile.setOnClickListener { _ ->
                        // FIXED: Changed it.name to it.getFullName()
                        EditProfileDialog.newInstance(it.getFullName(), it.username, it.profilePictureUrl)
                            .show(childFragmentManager, "edit_profile")
                    }
                }
            }
        }

        binding.btnAdminPanel.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_admin)
        }

        // UPDATED: Logout with confirmation dialog
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Fixed: persist dark mode preference across app restarts
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            val mode =
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // Order History click listener
        val orderHistoryLayout = binding.root.findViewById<ViewGroup>(R.id.order_history_layout)
        orderHistoryLayout?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_orders)
        }

        // Admin Logs click listener - navigate to dedicated logs screen
        val adminLogsLayout = binding.root.findViewById<ViewGroup>(R.id.admin_logs_layout)
        adminLogsLayout?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_activity_logs)
        }

        // Admin Archive click listener
        val adminArchiveLayout = binding.root.findViewById<ViewGroup>(R.id.admin_archive_layout)
        adminArchiveLayout?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_archive)
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out? You will need to log in again to access your account.")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        val act = activity ?: return
        authViewModel.logout()
        val intent = Intent(act, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        act.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}