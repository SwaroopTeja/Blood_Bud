package com.example.blood_bud.Activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ActivityRegistrationTypeBinding
import com.google.android.material.ripple.RippleUtils

class RegistrationTypeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationTypeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up window insets for edge-to-edge display
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        // Set up back button click listener with ripple effect
        binding.btnBackToLogin.setOnClickListener {
            it.isClickable = false
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            onBackPressed()
                        }
                        .start()
                }
                .start()
        }

        setupClickListeners()
        
        // Add entrance animation for the cards
        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        slideIn.duration = 400
        slideIn.interpolator = DecelerateInterpolator(1.5f)
        binding.btnUserRegistration.startAnimation(slideIn)
        
        val slideInDelayed = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        slideInDelayed.duration = 400
        slideInDelayed.startOffset = 100
        slideInDelayed.interpolator = DecelerateInterpolator(1.5f)
        binding.btnHospitalRegistration.startAnimation(slideInDelayed)
    }

    private fun setupClickListeners() {
        // Donor/Viewer Registration
        binding.btnUserRegistration.setOnClickListener { view ->
            // Add click animation with ripple effect
            view.isClickable = false
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            // Navigate to Donor Registration with animation
                            startActivity(
                                Intent(this, DonorRegistrationActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                }
                            )
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            view.isClickable = true
                        }
                        .start()
                }
                .start()
        }

        // Hospital Registration
        binding.btnHospitalRegistration.setOnClickListener { view ->
            // Add click animation with ripple effect
            view.isClickable = false
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            // Navigate to Hospital Registration with animation
                            startActivity(
                                Intent(this, HospitalRegistrationActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                }
                            )
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            view.isClickable = true
                        }
                        .start()
                }
                .start()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
    
    override fun onResume() {
        super.onResume()
        // Re-enable click listeners when returning to this activity
        binding.btnUserRegistration.isClickable = true
        binding.btnHospitalRegistration.isClickable = true
        binding.btnBackToLogin.isClickable = true
    }
}
