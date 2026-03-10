package com.example.blood_bud.Activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Address
import com.example.blood_bud.data.model.LocationData
import com.example.blood_bud.data.model.User
import com.example.blood_bud.data.model.UserType
import com.example.blood_bud.databinding.ActivityHospitalRegistrationBinding
import com.example.blood_bud.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HospitalRegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHospitalRegistrationBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    
    private val locationData = LocationData.indianLocations
    private val states = LocationData.indianLocations.states.map { it.name }
    private var cities = emptyList<String>()
    private var selectedState: String? = null
    private var selectedCity: String? = null

    private var isDashboardMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHospitalRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if we're in dashboard mode
        isDashboardMode = intent.getBooleanExtra("is_dashboard", false)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set click listener for back button in toolbar
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        if (isDashboardMode) {
            setupDashboardMode()
        } else {
            setupRegistrationMode()
        }
    }

    private fun setupDashboardMode() {
        // Update UI for dashboard mode
        binding.toolbar.title = "Hospital Dashboard"
        binding.btnRegister.visibility = View.GONE
        // Remove login prompt if it exists in the layout
        // binding.tvLoginPrompt?.visibility = View.GONE
        
        // TODO: Load and display hospital data
        loadHospitalData()
    }

    private fun setupRegistrationMode() {
        // Setup UI for registration mode
        binding.toolbar.title = "Hospital Registration"
        setupStateCityDropdowns()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun loadHospitalData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        // Show loading state
        binding.progressBar.visibility = View.VISIBLE

        // Load hospital data from Firestore
        FirebaseFirestore.getInstance()
            .collection("hospitals")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Populate fields with hospital data
                    document.getString("name")?.let { binding.etHospitalName.setText(it) }
                    document.getString("email")?.let { binding.etEmail.setText(it) }
                    document.getString("phone")?.let { binding.etPhone.setText(it) }
                    document.getString("address")?.let { binding.etAddress.setText(it) }
                    document.getString("pincode")?.let { binding.etPincode.setText(it) }
                    
                    // Set state and city if available
                    val state = document.getString("state")
                    val city = document.getString("city")
                    if (!state.isNullOrEmpty()) {
                        binding.etState.setText(state)
                        selectedState = state
                        if (!city.isNullOrEmpty()) {
                            binding.etCity.setText(city)
                            selectedCity = city
                        }
                    }
                    
                    // Disable all fields in dashboard mode
                    setFieldsEnabled(false)
                } else {
                    // If no hospital data found, switch to registration mode
                    isDashboardMode = false
                    setupRegistrationMode()
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e("HospitalRegistration", "Error loading hospital data", e)
                Toast.makeText(this, "Failed to load hospital data", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        binding.etHospitalName.isEnabled = enabled
        binding.etEmail.isEnabled = enabled
        binding.etPhone.isEnabled = enabled
        binding.etAddress.isEnabled = enabled
        binding.etState.isEnabled = enabled
        binding.etCity.isEnabled = enabled
        binding.etPincode.isEnabled = enabled
        binding.etPassword.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.etConfirmPassword.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.tilPassword.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.tilConfirmPassword.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        // Set click listener with debug logging
        binding.btnRegister.setOnClickListener {
            Log.d("HospitalRegistration", "Register button clicked")
            if (binding.btnRegister.isEnabled) {
                registerHospital()
            } else {
                Log.d("HospitalRegistration", "Button clicked but disabled")
                // Show which fields are invalid
                val hospitalName = binding.etHospitalName.text?.toString()?.trim()
                val email = binding.etEmail.text?.toString()?.trim()
                val phone = binding.etPhone.text?.toString()?.trim()
                val address = binding.etAddress.text?.toString()?.trim()
                val pincode = binding.etPincode.text?.toString()?.trim()
                val password = binding.etPassword.text?.toString()
                val confirmPassword = binding.etConfirmPassword.text?.toString()

                val errorMsg = buildString {
                    if (hospitalName.isNullOrBlank()) append("\n- Hospital name is required")
                    if (email.isNullOrBlank()) append("\n- Email is required")
                    if (phone.isNullOrBlank()) append("\n- Phone is required")
                    if (address.isNullOrBlank()) append("\n- Address is required")
                    if (pincode.isNullOrBlank()) {
                        append("\n- Pincode is required")
                    } else if (pincode.length != 6) {
                        append("\n- Pincode must be 6 digits")
                    }
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
                    Toast.makeText(
                        this, "Please fix the following errors:$errorMsg",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Log initial button state
        Log.d("HospitalRegistration", "Button enabled: ${binding.btnRegister.isEnabled}")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun setupTextWatchers() {
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateRegisterButtonState()
            }
        }

        binding.etHospitalName.addTextChangedListener(textWatcher)
        binding.etEmail.addTextChangedListener(textWatcher)
        binding.etPhone.addTextChangedListener(textWatcher)
        binding.etAddress.addTextChangedListener(textWatcher)
        binding.etCity.addTextChangedListener(textWatcher)
        binding.etState.addTextChangedListener(textWatcher)
        binding.etPincode.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
        binding.etConfirmPassword.addTextChangedListener(textWatcher)
    }

    private fun setupStateCityDropdowns() {
        // State dropdown setup
        val stateAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            states
        )
        
        (binding.etState as? AutoCompleteTextView)?.apply {
            setAdapter(stateAdapter)
            setOnItemClickListener { _, _, position, _ ->
                selectedState = stateAdapter.getItem(position)
                selectedState?.let { updateCitiesForState(it) }
                selectedCity = null
                binding.etCity.text?.clear()
            }
        }
        
        // Initially disable city dropdown until state is selected
        binding.etCity.isEnabled = false
    }
    
    private fun updateCitiesForState(stateName: String) {
        val state = locationData.states.find { it.name == stateName }
        cities = state?.cities ?: emptyList()
        
        val cityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            cities
        )
        
        (binding.etCity as? AutoCompleteTextView)?.apply {
            setAdapter(cityAdapter)
            isEnabled = cities.isNotEmpty()
            setOnItemClickListener { _, _, position, _ ->
                selectedCity = cityAdapter.getItem(position)
            }
        }
    }

    private fun updateRegisterButtonState() {
        val hospitalName = binding.etHospitalName.text?.toString()?.trim()
        val email = binding.etEmail.text?.toString()?.trim()
        val phone = binding.etPhone.text?.toString()?.trim()
        val address = binding.etAddress.text?.toString()?.trim()
        val city = binding.etCity.text?.toString()?.trim()
        val state = binding.etState.text?.toString()?.trim()
        val pincode = binding.etPincode.text?.toString()?.trim()
        val password = binding.etPassword.text?.toString()
        val confirmPassword = binding.etConfirmPassword.text?.toString()

        val isHospitalNameValid = !hospitalName.isNullOrBlank()
        val isEmailValid = !email.isNullOrBlank()
        val isPhoneValid = !phone.isNullOrBlank()
        val isAddressValid = !address.isNullOrBlank()
        val isCityValid = !city.isNullOrBlank() && selectedCity != null
        val isStateValid = !state.isNullOrBlank() && selectedState != null
        val isPincodeValid = !pincode.isNullOrBlank() && pincode.length == 6
        val isPasswordValid = !password.isNullOrBlank() && password.length >= 6
        val isConfirmPasswordValid = password == confirmPassword && !confirmPassword.isNullOrBlank()

        val isFormValid = isHospitalNameValid && isEmailValid && isPhoneValid &&
                isAddressValid && isCityValid && isStateValid &&
                isPincodeValid && isPasswordValid && isConfirmPasswordValid

        // Debug logging
        Log.d(
            "HospitalRegistration", "Form validation - " +
                    "HospitalName: $isHospitalNameValid, " +
                    "Email: $isEmailValid, " +
                    "Phone: $isPhoneValid, " +
                    "Address: $isAddressValid, " +
                    "Pincode: $isPincodeValid, " +
                    "Password: $isPasswordValid, " +
                    "Confirm: $isConfirmPasswordValid, " +
                    "All valid: $isFormValid"
        )

        // Update button state
        binding.btnRegister.isEnabled = isFormValid
        binding.btnRegister.alpha = if (isFormValid) 1.0f else 0.5f

        // Log button state change
        Log.d("HospitalRegistration", "Button enabled: $isFormValid")
    }

    private fun registerHospital() {
        val hospitalName = binding.etHospitalName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val street = binding.etAddress.text.toString().trim()
        val city = selectedCity ?: ""
        val state = selectedState ?: ""
        val pincode = binding.etPincode.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        // Log the address data being saved
        Log.d(
            "HospitalRegistration", "Saving hospital address: " +
                    "street=$street, city=$city, state=$state, pincode=$pincode"
        )

        // Create hospital data map with all required fields
        val hospitalData = hashMapOf<String, Any>(
            "name" to hospitalName,
            "email" to email,
            "phone" to phone,
            "registrationNumber" to "",                 // Can be added later
            "status" to "pending",                      // Required: Must be pending/approved/rejected
            "address" to mapOf(
                "street" to street,
                "city" to city,
                "state" to state,
                "postalCode" to pincode,
                "country" to "India",
                "latitude" to 0.0,
                "longitude" to 0.0
            ),
            "operatingHours" to "9:00 AM - 5:00 PM",
            "additionalInfo" to "",
            "bloodInventory" to mapOf(
                "A+" to 0, "A-" to 0,
                "B+" to 0, "B-" to 0,
                "AB+" to 0, "AB-" to 0,
                "O+" to 0, "O-" to 0
            ),
            "isApproved" to false,                      // Required: Boolean field
            "isActive" to true,                         // Required: Boolean field
            "userType" to "HOSPITAL",                   // Required: Must be HOSPITAL
            "createdAt" to com.google.firebase.Timestamp.now(),  // Required: Timestamp
            "updatedAt" to com.google.firebase.Timestamp.now()   // Required: Timestamp
        )

        // Create a minimal user object for registration
        val user = User(
            name = hospitalName,
            email = email,
            phone = phone,
            userType = UserType.HOSPITAL,
            address = Address()
        )

        // Use AuthViewModel to register the user (which will handle the auth user creation)
        authViewModel.register(user, password) { result ->
            result.onSuccess {
                // After successful auth, save hospital details to Firestore
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Set adminId to current user's UID
                    hospitalData["id"] = currentUser.uid                    // Required: User's UID
                    hospitalData["adminId"] = currentUser.uid               // Required: User's UID as admin
                    hospitalData["isApproved"] = false
                    hospitalData["isActive"] = true
                    hospitalData["userType"] = "HOSPITAL"
                    hospitalData["createdAt"] = com.google.firebase.Timestamp.now()
                    hospitalData["updatedAt"] = com.google.firebase.Timestamp.now()

                    // Save hospital details to Firestore
                    FirebaseFirestore.getInstance()
                        .collection("hospitals")
                        .document(currentUser.uid)
                        .set(hospitalData, SetOptions.merge())
                        .addOnSuccessListener {
                            // Registration successful
                            binding.progressBar.visibility = View.GONE
                            binding.btnRegister.isEnabled = true
                            Toast.makeText(
                                this@HospitalRegistrationActivity,
                                "Hospital registration successful!",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Navigate to hospital dashboard
                            finish()
                        }
                        .addOnFailureListener { e ->
                            // If saving hospital fails, delete the auth user to clean up
                            currentUser.delete()
                                .addOnSuccessListener {
                                    handleRegistrationError(
                                        e,
                                        "Failed to save hospital details. Please try again."
                                    )
                                }
                                .addOnFailureListener { deleteError ->
                                    handleRegistrationError(
                                        e,
                                        "Failed to save hospital details: ${e.message}"
                                    )
                                    Log.e(
                                        "HospitalRegistration",
                                        "Also failed to clean up auth user",
                                        deleteError
                                    )
                                }
                        }
                } else {
                    handleRegistrationError(
                        java.lang.Exception("No current user"),
                        "User registration failed"
                    )
                }
            }.onFailure { exception ->
                handleRegistrationError(
                    exception,
                    "Registration failed: ${exception.message ?: "Unknown error"}"
                )
            }
        }
    }

    private fun handleRegistrationError(exception: Throwable?, message: String) {
        try {
            // Run on UI thread to ensure we can update the UI
            runOnUiThread {
                try {
                    // Update UI elements
                    if (this::binding.isInitialized) {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                    }
                    
                    // Create error message
                    val errorMessage = exception?.message?.let { "$message: $it" } ?: message
                    
                    // Show toast with application context
                    val appContext = applicationContext
                    Toast.makeText(appContext, errorMessage, Toast.LENGTH_LONG).show()
                    
                    // Log the error
                    Log.e("HospitalRegistration", message, exception)
                } catch (e: Exception) {
                    Log.e("HospitalRegistration", "Error in UI update: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("HospitalRegistration", "Error in handleRegistrationError: ${e.message}", e)
        }
    }
}
