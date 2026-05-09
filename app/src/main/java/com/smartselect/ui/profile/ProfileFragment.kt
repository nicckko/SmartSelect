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
import com.smartselect.R
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
                user?.let {
                    binding.tvName.text = it.name
                    binding.tvEmail.text = it.email
                    binding.tvRole.text = if (it.role == "admin") "👑 Admin" else "👤 Customer"
                    binding.btnAdminPanel.visibility = if (it.role == "admin") View.VISIBLE else View.GONE
                    
                    val orderHistoryLayout = binding.root.findViewById<ViewGroup>(R.id.order_history_layout)
                    if (it.role == "admin") {
                        orderHistoryLayout?.visibility = View.GONE
                    } else {
                        orderHistoryLayout?.visibility = View.VISIBLE
                    }

                    binding.tvAvatar.text = it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    
                    binding.btnEditProfile.setOnClickListener { _ ->
                        EditProfileDialog.newInstance(it.name).show(childFragmentManager, "edit_profile")
                    }
                }
            }
        }

        binding.btnAdminPanel.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_admin)
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            startActivity(Intent(requireActivity(), LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            requireActivity().finish()
        }

        // Fixed: persist dark mode preference across app restarts
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // NEW: Order History click listener - navigate to OrdersFragment
        val orderHistoryLayout = binding.root.findViewById<ViewGroup>(R.id.order_history_layout)
        orderHistoryLayout?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_orders)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

