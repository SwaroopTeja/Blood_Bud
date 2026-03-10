package com.example.blood_bud.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.blood_bud.R
import com.example.blood_bud.data.model.UserType
import com.example.blood_bud.databinding.ActivityIntroBinding
import com.example.blood_bud.ui.admin.AdminDashboardActivity
import com.example.blood_bud.ui.donor.DonorDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth
        auth = Firebase.auth
        
        // Check if user is already logged in
        auth.currentUser?.let { user ->
            // User is already logged in, check their role and redirect accordingly
            checkUserRoleAndNavigate(user.uid)
            return
        }
        
        // If no user is logged in, show the intro screen
        showIntroScreen()
    }
    
    private fun checkUserRoleAndNavigate(userId: String) {
        // Check if user is admin first
        firestore.collection("admins").document(userId)
            .get()
            .addOnSuccessListener { adminDoc ->
                if (adminDoc.exists()) {
                    // User is an admin, go to admin dashboard with email
                    val email = auth.currentUser?.email ?: ""
                    startActivity(
                        Intent(this, AdminDashboardActivity::class.java).apply {
                            putExtra("CURRENT_USER_EMAIL", email)
                        }
                    )
                    finish()
                } else {
                    // Check if user is a regular user or hospital
                    firestore.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                // Regular user, go to donor dashboard
                                startActivity(Intent(this, DonorDashboardActivity::class.java))
                            } else {
                                // Check if hospital
                                firestore.collection("hospitals").document(userId)
                                    .get()
                                    .addOnSuccessListener { hospitalDoc ->
                                        if (hospitalDoc.exists() && hospitalDoc.getBoolean("isApproved") == true) {
                                            // Approved hospital user
                                            startActivity(Intent(this, HospitalDashboardActivity::class.java))
                                        } else {
                                            // User not found or not approved, show login
                                            showIntroScreen()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Log.e("IntroActivity", "Error checking hospital user", it)
                                        showIntroScreen()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Log.e("IntroActivity", "Error checking user data", it)
                            showIntroScreen()
                        }
                }
                finish()
            }
            .addOnFailureListener {
                Log.e("IntroActivity", "Error checking admin status", it)
                showIntroScreen()
            }
    }
    
    private fun showIntroScreen() {
        // Initialize ViewBinding
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up click listener for Get Started button
        binding.startBtn.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
            finish()
        }
    }
}
