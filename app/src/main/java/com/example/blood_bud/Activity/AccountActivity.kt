package com.example.blood_bud.Activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.blood_bud.databinding.ActivityAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.blood_bud.data.model.UserType
import com.example.blood_bud.ui.auth.ForgotPasswordActivity
import com.example.blood_bud.ui.admin.AdminDashboardActivity
import com.example.blood_bud.ui.donor.DonorDashboardActivity

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore
    private val sharedPref by lazy { getSharedPreferences("BloodBudPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase first
        auth = FirebaseAuth.getInstance()
        
        // Check if user is already logged in
        if (isUserLoggedIn()) {
            // If we have a current user in Firebase Auth, use that
            auth.currentUser?.let { 
                // Small delay to ensure any pending operations complete
                Handler(Looper.getMainLooper()).postDelayed({
                    redirectBasedOnUserType()
                }, 100)
            } ?: run {
                // If no Firebase user but we think we're logged in, clear the state
                saveLoginState("", UserType.DONOR)
                showLoginScreen()
            }
            return
        }
        
        showLoginScreen()
    }
    
    private fun showLoginScreen() {
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if user was redirected due to pending approval
        val pendingApproval = intent.getBooleanExtra("pending_approval", false)
        if (pendingApproval) {
            Toast.makeText(this, "Your account is pending approval. Please contact the administrator.", 
                Toast.LENGTH_LONG).show()
        }
        
        setupClickListeners()
    }
    
    private fun isUserLoggedIn(): Boolean {
        return sharedPref.getBoolean("isLoggedIn", false) && FirebaseAuth.getInstance().currentUser != null
    }
    
    private fun redirectBasedOnUserType() {
        val userType = sharedPref.getString("userType", "") ?: return
        val intent = when (userType) {
            UserType.ADMIN.name -> Intent(this, AdminDashboardActivity::class.java)
            UserType.HOSPITAL.name -> Intent(this, HospitalDashboardActivity::class.java)
            else -> Intent(this, DonorDashboardActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupClickListeners() {
        // Handle login button click
        binding.Button.setOnClickListener {
            val email = binding.Email.text.toString().trim()
            val password = binding.Password.text.toString().trim()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Show loading
            binding.Button.isEnabled = false
            binding.Button.text = "Logging in..."
            
            // Firebase authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    authResult.user?.let { user ->
                        // Check user role and approval status
                        checkUserAccess(user.uid, email)
                    } ?: run {
                        showLoginError("Authentication failed")
                    }
                }
                .addOnFailureListener { exception ->
                    // If user doesn't exist, try to create account
                    if (exception.message?.contains("no user record") == true || 
                        exception.message?.contains("user not found") == true) {
                        // User doesn't exist, create new account
                        createNewUserAccount(email, password)
                    } else {
                        showLoginError(exception.message ?: "Login failed")
                    }
                }
        }
        
        // Handle register button click - navigate to registration type selection
        binding.mainReg.setOnClickListener {
            startActivity(Intent(this, RegistrationTypeActivity::class.java))
        }
        
        // Handle forgot password click
        binding.forgotPassTv.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        
        // Handle back button click
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun checkUserAccess(userId: String, email: String) {
        // First check if user is in admins collection
        firestore.collection("admins").document(userId)
            .get()
            .addOnSuccessListener { adminDoc ->
                if (adminDoc.exists()) {
                    // User is an admin
                    saveLoginState(email, UserType.ADMIN)
                    // Add a small delay to ensure SharedPreferences are saved before navigation
                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToMain(UserType.ADMIN)
                    }, 100)
                } else {
                    // If not admin, check regular user flow
                    checkRegularUser(userId, email)
                }
            }
            .addOnFailureListener {
                // If there's an error checking admin status, continue with regular user flow
                checkRegularUser(userId, email)
            }
    }
    
    private fun checkRegularUser(userId: String, email: String) {
        // First check if user is in the users collection
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    // User found in users collection
                    saveLoginState(email, UserType.DONOR)
                    navigateToMain()
                } else {
                    // If not a regular user, check if it's a hospital
                    checkHospitalUser(userId, email)
                }
            }
            .addOnFailureListener {
                // On error, try checking hospital collection
                checkHospitalUser(userId, email)
            }
    }
    
    private fun checkHospitalUser(userId: String, email: String) {
        firestore.collection("hospitals").document(userId)
            .get()
            .addOnSuccessListener { hospitalDoc ->
                if (hospitalDoc.exists()) {
                    // Hospital user found, check approval status
                    val status = hospitalDoc.getString("status") ?: "pending"
                    if (status == "approved" || status == "active") {
                        saveLoginState(email, UserType.HOSPITAL)
                        navigateToMain(UserType.HOSPITAL)
                    } else {
                        showApprovalPending()
                        // Show toast message for unapproved hospital
                        val message = when (status) {
                            "pending" -> "Your hospital account is pending approval. Please contact the administrator."
                            "rejected" -> "Your hospital account has been rejected. Please contact support for more information."
                            else -> "Your hospital account isn't approved yet. Please contact the administrator."
                        }
                        Toast.makeText(this@AccountActivity, message, Toast.LENGTH_LONG).show()
                    }
                } else {
                    // User not found in either collection, create as donor by default
                    createUserDocument(userId, email)
                }
            }
            .addOnFailureListener { exception ->
                showLoginError("Failed to verify user access: ${exception.message}")
            }
    }
    
    private fun createUserDocument(userId: String, email: String) {
        // Check if this is the admin email
        if (email == "swarooproyal777@gmail.com") {
            // Create admin document
            val adminData = hashMapOf(
                "email" to email,
                "isSuperAdmin" to true,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "isActive" to true
            )
            
            firestore.collection("admins").document(userId)
                .set(adminData)
                .addOnSuccessListener {
                    // Reset button state
                    binding.Button.isEnabled = true
                    binding.Button.text = "Login"
                    
                    saveLoginState(email, UserType.ADMIN)
                    navigateToMain()
                }
                .addOnFailureListener { exception ->
                    showLoginError("Failed to create admin profile: ${exception.message}")
                }
        } else {
            // Default user creation as donor
            val userData = hashMapOf(
                "email" to email,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "lastLogin" to com.google.firebase.Timestamp.now()
            )
            
            // Create user in the users collection
            firestore.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener {
                    // Reset button state
                    binding.Button.isEnabled = true
                    binding.Button.text = "Login"
                    
                    saveLoginState(email, UserType.DONOR)
                    navigateToMain()
                }
                .addOnFailureListener { exception ->
                    showLoginError("Failed to create user profile: ${exception.message}")
                }
        }
    }
    
    private fun saveLoginState(email: String, userType: UserType) {
        with(sharedPref.edit()) {
            putBoolean("isLoggedIn", true)
            putString("userEmail", email)
            putString("userType", userType.name)
            apply()
        }
    }
    
    private fun navigateToMain(userType: UserType = UserType.DONOR) {
        val intent = when (userType) {
            UserType.ADMIN -> {
                // Get the email from the email input field
                val email = binding.Email.text?.toString() ?: ""
                Intent(this, AdminDashboardActivity::class.java).apply {
                    putExtra("CURRENT_USER_EMAIL", email)
                }
            }
            UserType.HOSPITAL -> Intent(this, HospitalDashboardActivity::class.java)
            else -> Intent(this, DonorDashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
    
    private fun showApprovalPending() {
        binding.Button.isEnabled = true
        binding.Button.text = "Login"
        Toast.makeText(this, "Your hospital account is pending approval. Please contact the administrator.", 
            Toast.LENGTH_LONG).show()
    }
    
    private fun createNewUserAccount(email: String, password: String) {
        // Show creating account message
        binding.Button.text = "Creating Account..."
        
        // Create new Firebase Auth user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                authResult.user?.let { user ->
                    // Create user document in Firestore
                    createUserDocument(user.uid, email)
                } ?: run {
                    showLoginError("Failed to create user account")
                }
            }
            .addOnFailureListener { exception ->
                // Handle specific Firebase Auth errors
                val errorMessage = when {
                    exception.message?.contains("email address is already in use") == true -> 
                        "Email already registered. Please try logging in instead."
                    exception.message?.contains("password is invalid") == true -> 
                        "Password must be at least 6 characters long."
                    exception.message?.contains("network") == true -> 
                        "Network error. Please check your internet connection."
                    else -> "Failed to create account: ${exception.message}"
                }
                showLoginError(errorMessage)
            }
    }
    
    private fun showLoginError(message: String) {
        binding.Button.isEnabled = true
        binding.Button.text = "Login"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Exit the app when back button is pressed from login screen
        finishAffinity()
    }
}
