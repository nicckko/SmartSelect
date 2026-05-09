package com.smartselect.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.smartselect.R
import com.smartselect.databinding.ActivityMainBinding
import com.smartselect.ui.auth.LoginActivity
import com.smartselect.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var navController: NavController
    private var adminSetupDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!authViewModel.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupNavigation()
        observeUser()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        // FIX: Handle bottom navigation properly when user is on a nested screen
        // This ensures that clicking Home when on PhoneDetailsFragment navigates back to Home
        binding.bottomNav.setOnItemReselectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    // Pop back to home fragment if not already there
                    if (navController.currentDestination?.id != R.id.homeFragment) {
                        navController.popBackStack(R.id.homeFragment, false)
                    }
                }
                R.id.compareFragment -> {
                    if (navController.currentDestination?.id != R.id.compareFragment) {
                        navController.popBackStack(R.id.compareFragment, false)
                    }
                }
                R.id.ordersFragment -> {
                    if (navController.currentDestination?.id != R.id.ordersFragment) {
                        navController.popBackStack(R.id.ordersFragment, false)
                    }
                }
                R.id.favoritesFragment -> {
                    if (navController.currentDestination?.id != R.id.favoritesFragment) {
                        navController.popBackStack(R.id.favoritesFragment, false)
                    }
                }
                R.id.profileFragment -> {
                    if (navController.currentDestination?.id != R.id.profileFragment) {
                        navController.popBackStack(R.id.profileFragment, false)
                    }
                }
            }
        }
    }

    private fun observeUser() {
        lifecycleScope.launch {
            authViewModel.currentUser.collect { user ->
                if (isFinishing || isDestroyed) return@collect
                if (user != null && !adminSetupDone) {
                    if (user.role == "admin") {
                        setupAdminUI()
                    } else {
                        setupUserUI()
                    }
                    adminSetupDone = true
                }
            }
        }
    }

    private fun setupAdminUI() {
        if (isFinishing || isDestroyed) return
        binding.bottomNav.menu.clear()
        binding.bottomNav.inflateMenu(R.menu.bottom_nav_admin)

        binding.bottomNav.setOnItemSelectedListener { item ->
            if (isFinishing || isDestroyed) return@setOnItemSelectedListener false
            try {
                val currentId = navController.currentDestination?.id
                when (item.itemId) {
                    R.id.adminFragment -> {
                        if (currentId != R.id.adminFragment) navController.navigate(R.id.adminFragment)
                        true
                    }
                    R.id.adminPhonesFragment -> {
                        if (currentId != R.id.adminPhonesFragment) navController.navigate(R.id.adminPhonesFragment)
                        true
                    }
                    R.id.adminOrdersFragment -> {
                        if (currentId != R.id.adminOrdersFragment) navController.navigate(R.id.adminOrdersFragment)
                        true
                    }
                    R.id.profileFragment -> {
                        if (currentId != R.id.profileFragment) navController.navigate(R.id.profileFragment)
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
        }

        // Navigate to admin dashboard only on first setup
        try {
            if (navController.currentDestination?.id == R.id.homeFragment) {
                navController.navigate(R.id.adminFragment)
            }
        } catch (_: Exception) {}
    }

    private fun setupUserUI() {
        if (isFinishing || isDestroyed) return
        binding.bottomNav.menu.clear()
        binding.bottomNav.inflateMenu(R.menu.bottom_nav_menu)
        binding.bottomNav.setupWithNavController(navController)
    }
}