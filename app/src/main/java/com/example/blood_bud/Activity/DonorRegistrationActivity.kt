package com.example.blood_bud.Activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.blood_bud.R
import com.example.blood_bud.data.model.User
import com.example.blood_bud.data.model.UserType
import com.example.blood_bud.databinding.ActivityDonorRegistrationBinding
import com.example.blood_bud.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DonorRegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDonorRegistrationBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonorRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Set click listener for back button in toolbar
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        setupClickListeners()
        setupTextWatchers()
        setupBloodGroupDropdown()
    }

    private fun setupClickListeners() {
        // Set click listener with debug logging
        binding.btnRegister.setOnClickListener {
            Log.d("DonorRegistration", "Register button clicked")
            if (binding.btnRegister.isEnabled) {
                registerUser()
            } else {
                Log.d("DonorRegistration", "Button clicked but disabled")
                // Show which fields are invalid
                val name = binding.etName.text?.toString()?.trim()
                val email = binding.etEmail.text?.toString()?.trim()
                val phone = binding.etPhone.text?.toString()?.trim()
                val bloodGroup = binding.etBloodGroup.text?.toString()?.trim()
                val password = binding.etPassword.text?.toString()
                val confirmPassword = binding.etConfirmPassword.text?.toString()
                
                val errorMsg = buildString {
                    if (name.isNullOrBlank()) append("\n- Name is required")
                    if (email.isNullOrBlank()) append("\n- Email is required")
                    if (phone.isNullOrBlank()) append("\n- Phone is required")
                    if (bloodGroup.isNullOrBlank()) append("\n- Blood group is required")
                    if (password.isNullOrBlank()) {
                        append("\n- Password is required")
                    } else if (password.length < 6) {
                        append("\n- Password must be at least 6 characters")
                    }
                    if (password != confirmPassword) {
                        append("\n- Passwords do not match")
                    }
                }
                
                if (errorMsg.isNotBlank()) {
                    Toast.makeText(this, "Please fix the following errors:$errorMsg", 
                        Toast.LENGTH_LONG).show()
                }
            }
        }
        
        // Log initial button state
        Log.d("DonorRegistration", "Button enabled: ${binding.btnRegister.isEnabled}")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun setupTextWatchers() {
        // Add text watchers to update button state
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateRegisterButtonState()
            }
        }

        binding.etName.addTextChangedListener(textWatcher)
        binding.etEmail.addTextChangedListener(textWatcher)
        binding.etPhone.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
        binding.etConfirmPassword.addTextChangedListener(textWatcher)
    }
    
    private fun setupBloodGroupDropdown() {
        val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodGroups)
        binding.etBloodGroup.setAdapter(adapter)
    }

    private fun updateRegisterButtonState() {
        val name = binding.etName.text?.toString()?.trim()
        val email = binding.etEmail.text?.toString()?.trim()
        val phone = binding.etPhone.text?.toString()?.trim()
        val bloodGroup = binding.etBloodGroup.text?.toString()?.trim()
        val password = binding.etPassword.text?.toString()
        val confirmPassword = binding.etConfirmPassword.text?.toString()

        val isNameValid = !name.isNullOrBlank()
        val isEmailValid = !email.isNullOrBlank()
        val isPhoneValid = !phone.isNullOrBlank()
        val isBloodGroupValid = !bloodGroup.isNullOrBlank()
        val isPasswordValid = !password.isNullOrBlank() && password.length >= 6
        val isConfirmPasswordValid = password == confirmPassword && !confirmPassword.isNullOrBlank()

        val isFormValid = isNameValid && isEmailValid && isPhoneValid && 
                         isBloodGroupValid && isPasswordValid && isConfirmPasswordValid

        // Debug logging
        Log.d("DonorRegistration", "Form validation - " +
                "Name: $isNameValid, " +
                "Email: $isEmailValid, " +
                "Phone: $isPhoneValid, " +
                "BloodGroup: $isBloodGroupValid, " +
                "Password: $isPasswordValid, " +
                "Confirm: $isConfirmPasswordValid, " +
                "All valid: $isFormValid")

        // Update button state
        binding.btnRegister.isEnabled = isFormValid
        binding.btnRegister.alpha = if (isFormValid) 1.0f else 0.5f
        
        // Log button state change
        Log.d("DonorRegistration", "Button enabled: $isFormValid")
    }

    private fun registerUser() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val bloodGroup = binding.etBloodGroup.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        // Create user object
        val user = User(
            name = name,
            email = email,
            phone = phone,
            bloodGroup = bloodGroup,
            userType = UserType.DONOR
        )

        // Register user using ViewModel
        authViewModel.register(user, password) { result ->
            result.onSuccess {
                // Registration successful
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                // Navigate to main activity or login
                finish()
            }.onFailure { exception ->
                // Registration failed
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
                Toast.makeText(this, "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
                Log.e("DonorRegistration", "Registration failed", exception)
            }
        }
    }
}
