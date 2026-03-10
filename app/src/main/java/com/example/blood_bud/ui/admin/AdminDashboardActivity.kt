package com.example.blood_bud.ui.admin

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.blood_bud.Activity.AccountActivity
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ActivityAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = getString(R.string.admin_dashboard)
    }

    private fun setupClickListeners() {
        // Get current user's email from intent or preferences
        val currentUserEmail = intent.getStringExtra("CURRENT_USER_EMAIL") ?: ""
        
        // Logout button
        binding.fabLogout.setOnClickListener {
            // Clear login state
            val sharedPref = getSharedPreferences("BloodBudPrefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("isLoggedIn", false)
                remove("userType")
                remove("userEmail")
                apply()
            }
            
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut()
            
            // Navigate to login screen
            val intent = Intent(this, AccountActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        // Card click listeners
        binding.cardUsers.setOnClickListener {
            // Navigate to users screen
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }

        binding.cardDonations.setOnClickListener {
            // Navigate to donations screen
            startActivity(Intent(this, ViewDonationsActivity::class.java))
        }

        binding.cardHospitals.setOnClickListener {
            // Navigate to hospitals screen
            startActivity(Intent(this, ManageHospitalsActivity::class.java))
        }

        binding.cardAnalytics.setOnClickListener {
            // Navigate to analytics screen
            startActivity(Intent(this, ViewAnalyticsActivity::class.java))
        }

        binding.cardAdmins.setOnClickListener {
            // Navigate to manage admins screen with current user's email
            val intent = Intent(this, ManageAdminsActivity::class.java).apply {
                putExtra("CURRENT_USER_EMAIL", currentUserEmail)
            }
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
