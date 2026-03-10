package com.example.blood_bud.Activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Address
import com.example.blood_bud.data.model.User
import com.example.blood_bud.data.model.UserType
import com.example.blood_bud.databinding.ActivityRegistrationBinding
import com.example.blood_bud.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding
    private var registrationType: String = "user" // Default to user registration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup UI components
        setupRegistrationTypeToggle()
        setupClickListeners()

        // Get registration type from intent
        registrationType = intent.getStringExtra("registrationType") ?: "user"

        // Initialize UI based on registration type
        updateUIForRegistrationType()

        // Load initial data
        loadHospitals()
    }

    private fun updateUIForRegistrationType() {
        when (registrationType) {
            "hospital" -> {
                binding.rbHospital.isChecked = true
                binding.tilName.visibility = View.GONE
                binding.tilEmail.visibility = View.GONE
                binding.cardHospitalInfo.visibility = View.VISIBLE
                binding.tilPhone.visibility = View.GONE
            }

            else -> {
                binding.rbViewer.isChecked = true
                binding.tilName.visibility = View.VISIBLE
                binding.tilEmail.visibility = View.VISIBLE
                binding.cardHospitalInfo.visibility = View.GONE
                binding.tilPhone.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRegistrationTypeToggle() {
        binding.rgUserType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbViewer -> {
                    // Show user fields and hide hospital fields
                    binding.tilName.visibility = View.VISIBLE
                    binding.tilEmail.visibility = View.VISIBLE
                    binding.cardHospitalInfo.visibility = View.GONE
                    binding.tilPhone.visibility = View.VISIBLE
                    registrationType = "user"
                }

                R.id.rbHospital -> {
                    // Hide user fields and show hospital fields
                    binding.tilName.visibility = View.GONE
                    binding.tilEmail.visibility = View.GONE
                    binding.cardHospitalInfo.visibility = View.VISIBLE
                    binding.tilPhone.visibility = View.GONE
                    registrationType = "hospital"
                }
            }
            // Update the register button state when user type changes
            updateRegisterButtonState()
        }
    }

    private fun loadHospitals() {
        // Set the default hospital name
        val defaultHospitalName =
            "City General Hospital" // You can fetch this from your data source
        binding.etHospitalName?.setText(defaultHospitalName)

        // Enable text watcher for the hospital name field
        binding.etHospitalName?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Update the hospital name as the user types
                updateRegisterButtonState()
            }
        })
    }

    private fun setupUI() {
        if (registrationType == "hospital") {
            binding.cardHospitalInfo.visibility = View.VISIBLE
            binding.btnRegister.text = getString(R.string.register_hospital)
            loadHospitals()
        } else {
            binding.cardHospitalInfo.visibility = View.GONE
            binding.btnRegister.text = getString(R.string.create_account)
        }
    }

    private fun setupUserTypeToggle() {
        binding.rgUserType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbViewer -> {
                    // Hide hospital info for viewer/donor
                    binding.cardHospitalInfo.visibility = View.GONE
                    updateRegisterButtonState()
                }

                R.id.rbHospital -> {
                    // Show hospital info for hospital registration
                    binding.cardHospitalInfo.visibility = View.VISIBLE
                    updateRegisterButtonState()
                }
            }
        }
    }

    private fun updateRegisterButtonState() {
        // Only update if the activity is still active
        if (isFinishing || isDestroyed) return
        
        // Get all values once to avoid multiple findViewById calls
        val name = binding.etName.text?.toString()
        val email = binding.etEmail.text?.toString()
        val phone = binding.etPhone.text?.toString()
        val password = binding.etPassword.text?.toString()
        val confirmPassword = binding.etConfirmPassword.text?.toString()
        
        val isFormValid = when (binding.rgUserType.checkedRadioButtonId) {
            R.id.rbViewer -> {
                // Basic validation for viewer
                !name.isNullOrBlank() &&
                !email.isNullOrBlank() &&
                !phone.isNullOrBlank() &&
                !password.isNullOrBlank() &&
                password == confirmPassword
            }

            R.id.rbHospital -> {
                // For hospital, only validate hospital-specific fields
                !phone.isNullOrBlank() &&
                !password.isNullOrBlank() &&
                password == confirmPassword &&
                !binding.etHospitalName.text.isNullOrBlank() &&
                !binding.etHospitalAddress.text.isNullOrBlank()
            }

            else -> false
        }

        Log.d("REGISTRATION", "Button state - Valid: $isFormValid, Type: ${if (binding.rbHospital.isChecked) "Hospital" else "Viewer"}")
        binding.btnRegister.isEnabled = isFormValid
    }


    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
            finish()
        }
        
        // Debug button state
        Log.d("REGISTRATION", "Setting up click listeners")
        Log.d("REGISTRATION", "Register button enabled: ${binding.btnRegister.isEnabled}")
        Log.d("REGISTRATION", "Register button clickable: ${binding.btnRegister.isClickable}")
        Log.d("REGISTRATION", "Register button visibility: ${binding.btnRegister.visibility} (${binding.btnRegister.visibility == View.VISIBLE})")
        
        // Log the view hierarchy for debugging
        binding.root.post {
            Log.d("VIEW_HIERARCHY", "View hierarchy for registration activity:")
            logViewHierarchy(binding.root, 0)
        }
        
        // Add a test click listener directly to the root view
        binding.root.setOnClickListener {
            Log.d("REGISTRATION", "Root view clicked")
        }
        
        // Add a test click listener to the parent of the register button
        (binding.btnRegister.parent as? View)?.setOnClickListener {
            Log.d("REGISTRATION", "Register button parent clicked")
        }
        
        // Simplified click listener with maximum debug info
        binding.btnRegister.setOnClickListener { view ->
            Log.d("REGISTRATION", "Register button CLICKED!")
            
            // Visual feedback
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(50)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(50)
                        .start()
                }.start()
            
            // Get input values
            val name = binding.etName.text?.toString()?.trim() ?: ""
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val phone = binding.etPhone.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString()?.trim() ?: ""
            val confirmPassword = binding.etConfirmPassword.text?.toString()?.trim() ?: ""
            
            Log.d("REGISTRATION", "Input values - Name: '$name', Email: '$email', Phone: '$phone'")
            
            // Process registration on main thread first (simplified)
            try {
                processRegistration(name, email, phone, password, confirmPassword)
            } catch (e: Exception) {
                Log.e("REGISTRATION", "Registration error", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun processRegistration(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {
        Log.d("REGISTRATION", "Validating inputs...")
        
        val isHospital = binding.rbHospital.isChecked
        
        if (validateInputs(
                name = if (isHospital) "Hospital" else name,
                email = if (isHospital) "" else email, // Empty email for hospital as it's not shown/required
                password = password,
                confirmPassword = confirmPassword
            )) {
            Log.d("REGISTRATION", "Inputs valid, proceeding with registration")
            
            if (isHospital) {
                // For hospital registration, get additional fields
                val hospitalName = binding.etHospitalName.text.toString().trim()
                val address = binding.etHospitalAddress.text.toString().trim()
                val contactEmail = binding.etHospitalEmail?.text?.toString()?.trim() ?: ""
                
                Log.d("REGISTRATION", "Registering hospital: $hospitalName")
                
                registerUser(
                    name = hospitalName, // Use hospital name as the name for hospital registration
                    email = contactEmail, // Use hospital email if available, otherwise empty
                    password = password,
                    hospitalName = hospitalName,
                    address = address,
                    phone = phone
                )
            } else {
                // Register regular user
                Log.d("REGISTRATION", "Registering donor: $name")
                registerUser(
                    name = name,
                    email = email,
                    password = password,
                    phone = phone
                )
            }
        } else {
            Log.d("REGISTRATION", "Validation failed")
        }

        // Toggle password visibility (using the built-in password toggle)
        // The password toggle is already handled by the TextInputLayout with app:passwordToggleEnabled

        // Handle login text click
        if (binding.tvLogin != null) {
            binding.tvLogin.setOnClickListener {
                startActivity(Intent(this, AccountActivity::class.java))
                finish()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Exit the app when back button is pressed from login screen
        finishAffinity()
    }

    private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        Log.d("VALIDATION", "Starting validation...")
        
        // Clear previous errors
        binding.tilName?.error = null
        binding.tilEmail?.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        val isHospital = binding.rbHospital.isChecked
        
        // Only validate name and email for non-hospital registration
        if (!isHospital) {
            if (name.isEmpty()) {
                binding.tilName?.error = "Name is required"
                Log.d("VALIDATION", "Name is required")
                return false
            }

            if (email.isEmpty()) {
                binding.tilEmail?.error = "Email is required"
                Log.d("VALIDATION", "Email is required")
                return false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail?.error = "Please enter a valid email"
                Log.d("VALIDATION", "Invalid email format")
                return false
            }
        }

        // Password validations for all users
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            Log.d("VALIDATION", "Password is required")
            return false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            Log.d("VALIDATION", "Password too short")
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            Log.d("VALIDATION", "Confirm password is required")
            return false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            Log.d("VALIDATION", "Passwords don't match")
            return false
        }

        // Additional validation for hospital registration
        if (isHospital) {
            Log.d("VALIDATION", "Validating hospital fields")
            
            // Clear previous errors
            binding.tilHospitalName?.error = null
            binding.tilHospitalAddress?.error = null
            binding.etHospitalPincode?.error = null
            binding.etHospitalContact?.error = null

            // Get values
            val hospitalName = binding.etHospitalName?.text?.toString()?.trim() ?: ""
            val address = binding.etHospitalAddress?.text?.toString()?.trim() ?: ""
            val pincode = binding.etHospitalPincode?.text?.toString()?.trim() ?: ""
            val contact = binding.etHospitalContact?.text?.toString()?.trim() ?: ""
            
            Log.d("VALIDATION", "Hospital fields - Name: $hospitalName, Address: $address, Pincode: $pincode, Contact: $contact")

            // Validate hospital fields
            if (hospitalName.isEmpty()) {
                binding.tilHospitalName?.error = "Hospital name is required"
                Log.d("VALIDATION", "Hospital name is required")
                return false
            }

            if (address.isEmpty()) {
                binding.tilHospitalAddress?.error = "Address is required"
                Log.d("VALIDATION", "Address is required")
                return false
            }

            if (pincode.isEmpty() || !pincode.matches("\\d{6}".toRegex())) {
                binding.etHospitalPincode?.error = "Please enter a valid 6-digit pincode"
                Log.d("VALIDATION", "Invalid pincode")
                return false
            }

            if (contact.isEmpty() || !contact.matches("^[6-9]\\d{9}$".toRegex())) {
                binding.etHospitalContact?.error = "Please enter a valid 10-digit mobile number"
                Log.d("VALIDATION", "Invalid contact number")
                return false
            }
        }

        return true
    }


    private val authViewModel: AuthViewModel by viewModels()

    private fun registerUser(
        name: String,
        email: String,
        password: String,
        hospitalName: String? = null,
        address: String? = null,
        phone: String? = null
    ) {
        showLoading(true)

        // Create user object based on registration type
        val user = if (registrationType == "hospital") {
            // Create hospital user
            User(
                name = hospitalName ?: "",
                email = email,
                phone = phone ?: "",
                userType = UserType.HOSPITAL,
                address = Address(
                    street = address ?: "",
                    city = "",
                    state = "",
                    postalCode = binding.etHospitalPincode?.text?.toString() ?: ""
                )
            )
        } else {
            // Create regular user
            User(
                name = name,
                email = email,
                phone = phone ?: "",
                userType = UserType.DONOR,
                address = Address()
            )
        }

        // Register the user using AuthViewModel
        authViewModel.register(user, password) { result ->
            showLoading(false)
            
            result.fold(
                onSuccess = {
                    // Registration successful
                    val message = if (registrationType == "hospital") {
                        "Hospital registration submitted for approval: $hospitalName"
                    } else {
                        "Registration successful! Please login."
                    }
                    
                    Toast.makeText(
                        this@RegistrationActivity,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Navigate to login screen
                    startActivity(Intent(this@RegistrationActivity, AccountActivity::class.java))
                    finish()
                },
                onFailure = { exception ->
                    // Registration failed
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Registration failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
    }
    
    /**
     * Recursively logs the view hierarchy for debugging purposes.
     * @param view The root view to start logging from
     * @param level The current indentation level
     */
    private fun logViewHierarchy(view: View, level: Int) {
        val indent = "  ".repeat(level)
        val viewInfo = "$indent${view.javaClass.simpleName} (${view.id}) " +
                      "[${view.visibility}] " +
                      "${view.width}x${view.height}@(${view.x},${view.y}) " +
                      "clickable=${view.isClickable} enabled=${view.isEnabled} " +
                      "focusable=${view.isFocusable} focused=${view.isFocused}"
        
        Log.d("VIEW_HIERARCHY", viewInfo)
        
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                logViewHierarchy(view.getChildAt(i), level + 1)
            }
        }
    }

    companion object {
        private const val TAG = "RegistrationActivity"
    }
}
