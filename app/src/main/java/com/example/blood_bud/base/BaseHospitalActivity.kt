package com.example.blood_bud.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.blood_bud.Activity.AppointmentsActivity
import com.example.blood_bud.Activity.HospitalDashboardActivity
import com.example.blood_bud.Activity.HospitalHistoryActivity
import com.example.blood_bud.Activity.HospitalProfileActivity
import com.example.blood_bud.R
import com.example.blood_bud.utils.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

abstract class BaseHospitalActivity<T : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: T
    private lateinit var bottomNav: BottomNavigationView

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getViewBinding()
        setContentView(binding.root)
        
        setupBottomNavigation()
    }

    abstract fun getViewBinding(): T

    protected open fun setupBottomNavigation() {
        // Bottom navigation will be set up in the child activities
    }

    protected open fun setupBottomNavigation(bottomNavId: Int) {
        bottomNav = findViewById(bottomNavId)
        bottomNav.menu.clear()
        bottomNav.inflateMenu(R.menu.hospital_bottom_nav_menu)
        
        bottomNav.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is HospitalDashboardActivity) {
                        navigateTo(HospitalDashboardActivity::class.java)
                    }
                    true
                }
                R.id.nav_donate -> {
                    // Handle donate action
                    if (this is HospitalDashboardActivity) {
                        // Show donation options or navigate to donation screen
                        showToast("Manage blood donations")
                    } else {
                        navigateTo(HospitalDashboardActivity::class.java)
                    }
                    true
                }
                R.id.nav_search -> {
                    // Handle search action
                    showToast("Search feature coming soon")
                    true
                }
                R.id.nav_appointments -> {
                    if (this !is AppointmentsActivity) {
                        navigateTo(AppointmentsActivity::class.java)
                    }
                    true
                }
                R.id.nav_history -> {
                    if (this !is HospitalHistoryActivity) {
                        navigateTo(HospitalHistoryActivity::class.java)
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is HospitalProfileActivity) {
                        startActivity(Intent(this@BaseHospitalActivity, HospitalProfileActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        })
    }
    
    private fun <T> navigateTo(activityClass: Class<T>) where T : AppCompatActivity {
        startActivity(Intent(this, activityClass))
        // Don't finish current activity to avoid navigation issues
    }

    protected fun setSelectedNavItem(itemId: Int) {
        bottomNav.selectedItemId = itemId
    }
}
