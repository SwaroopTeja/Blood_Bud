package com.example.blood_bud.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blood_bud.data.model.User
import com.example.blood_bud.data.model.UserType
import com.example.blood_bud.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "AuthViewModel"
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState
    
    init {
        // Authentication will be handled explicitly by the login flow
        _authState.value = AuthState.Unauthenticated
    }
    
    /**
     * Check if there's a currently logged-in user and update the auth state accordingly.
     * Should be called on app start or when the auth state might have changed.
     */
    fun checkCurrentUser() {
        auth.currentUser?.let { firebaseUser ->
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                try {
                    // First check if user is an admin
                    val adminDoc = firestore.collection("admins")
                        .document(firebaseUser.uid)
                        .get()
                        .await()
                    
                    if (adminDoc.exists()) {
                        val adminData = adminDoc.data
                        val user = User(
                            id = firebaseUser.uid,
                            email = adminData?.get("email") as? String ?: firebaseUser.email ?: "",
                            name = adminData?.get("name") as? String ?: firebaseUser.displayName ?: "",
                            userType = UserType.ADMIN,
                            isAdmin = true,
                            isSuperAdmin = adminData?.get("isSuperAdmin") as? Boolean ?: false,
                            isActive = adminData?.get("isActive") as? Boolean ?: true
                        )
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                        return@launch
                    }
                    
                    // If not admin, check regular users
                    val user = userRepository.getUserById(firebaseUser.uid)
                    if (user != null) {
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                    } else {
                        // User data not found, sign out
                        auth.signOut()
                        _authState.value = AuthState.Unauthenticated
                    }
                } catch (e: Exception) {
                    // If Firestore access fails, show error but don't sign out
                    // This prevents users from being logged out due to temporary network issues
                    _authState.value = AuthState.Error("Failed to access user data: ${e.message}")
                    
                    // Try to get cached user data if available
                    _currentUser.value?.let { cachedUser ->
                        _authState.value = AuthState.Authenticated(cachedUser)
                    }
                }
            }
        } ?: run {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    fun login(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                authResult.user?.let { firebaseUser ->
                    viewModelScope.launch {
                        try {
                            // First, check if this is an admin user
                            val adminDoc = firestore.collection("admins")
                                .document(firebaseUser.uid)
                                .get()
                                .await()
                            
                            if (adminDoc.exists()) {
                                val adminData = adminDoc.data
                                val user = User(
                                    id = firebaseUser.uid,
                                    email = adminData?.get("email") as? String ?: firebaseUser.email ?: "",
                                    name = adminData?.get("name") as? String ?: firebaseUser.displayName ?: "",
                                    userType = UserType.ADMIN,
                                    isAdmin = true,
                                    isSuperAdmin = adminData?.get("isSuperAdmin") as? Boolean ?: false,
                                    isActive = adminData?.get("isActive") as? Boolean ?: true
                                )
                                _currentUser.value = user
                                _authState.value = AuthState.Authenticated(user)
                                onResult(Result.success(Unit))
                                return@launch
                            }
                            
                            // If not admin, check if this is a hospital user
                            val hospitalDoc = firestore.collection("hospitals")
                                .document(firebaseUser.uid)
                                .get()
                                .await()
                            
                            if (hospitalDoc.exists()) {
                                // This is a hospital user, create a minimal User object
                                val hospitalData = hospitalDoc.data ?: throw Exception("Hospital data not found")
                                val user = User(
                                    id = firebaseUser.uid,
                                    email = hospitalData["email"] as? String ?: "",
                                    name = hospitalData["name"] as? String ?: "",
                                    phone = hospitalData["phone"] as? String ?: "",
                                    userType = UserType.HOSPITAL,
                                    isSystemUser = true,  // Mark as system user to prevent saving
                                    isActive = hospitalData["isActive"] as? Boolean ?: true
                                )
                                _currentUser.value = user
                                _authState.value = AuthState.Authenticated(user)
                                onResult(Result.success(Unit))
                                return@launch
                            } 
                            
                            // This is a regular user, get from users collection
                            val user = userRepository.getUserById(firebaseUser.uid)
                            if (user != null) {
                                _currentUser.value = user
                                _authState.value = AuthState.Authenticated(user)
                                onResult(Result.success(Unit))
                                updateFcmToken(user.id)
                            } else {
                                // If user not found in either collection, sign them out
                                auth.signOut()
                                _authState.value = AuthState.Error("User data not found. Please register first.")
                                onResult(Result.failure(Exception("User data not found. Please register first.")))
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Login error", e)
                            auth.signOut()
                            _authState.value = AuthState.Error(e.message ?: "Login failed. Please try again.")
                            onResult(Result.failure(e))
                        }
                    }
                } ?: run {
                    _authState.value = AuthState.Error("Authentication failed: No user data")
                    onResult(Result.failure(Exception("Authentication failed: No user data")))
                }
            }
            .addOnFailureListener { exception ->
                val errorMessage = when {
                    exception.message?.contains("password is invalid", ignoreCase = true) == true ->
                        "Invalid email or password"
                    exception.message?.contains("no user record", ignoreCase = true) == true ->
                        "No account found with this email"
                    exception.message?.contains("network error", ignoreCase = true) == true ->
                        "Network error. Please check your internet connection"
                    else -> exception.message ?: "Login failed. Please try again."
                }
                _authState.value = AuthState.Error(errorMessage)
                onResult(Result.failure(Exception(errorMessage)))
            }
    }
    
    private fun updateFcmToken(userId: String) {
        // Get the FCM token and update it in Firestore
        // This is a placeholder - implement your FCM token update logic here
        // For example:
        // FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        //     if (task.isSuccessful) {
        //         val token = task.result
        //         userRepository.updateFcmToken(userId, token)
        //     }
        // }
    }
    
    fun register(user: User, password: String, onResult: (Result<Unit>) -> Unit) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            try {
                // 1. Check if email already exists
                if (userRepository.doesEmailExist(user.email)) {
                    val error = Exception("Email address is already in use")
                    _authState.value = AuthState.Error(error.message ?: "Registration failed")
                    onResult(Result.failure(error))
                    return@launch
                }
                
                // 2. Create authentication user
                val authResult = try {
                    auth.createUserWithEmailAndPassword(user.email, password).await()
                } catch (e: Exception) {
                    // Handle specific authentication errors
                    val error = when {
                        e.message?.contains("The email address is badly formatted") == true -> 
                            Exception("Please enter a valid email address")
                        e.message?.contains("The given password is invalid") == true ->
                            Exception("Password is too weak. Please choose a stronger password")
                        e.message?.contains("network error") == true ->
                            Exception("Network error. Please check your internet connection")
                        else -> e
                    }
                    _authState.value = AuthState.Error(error.message ?: "Registration failed")
                    onResult(Result.failure(error))
                    return@launch
                }
                
                // 3. Get the new user ID
                val userId = authResult.user?.uid ?: run {
                    val error = Exception("Failed to create user account")
                    _authState.value = AuthState.Error(error.message ?: "Registration failed")
                    onResult(Result.failure(error))
                    return@launch
                }
                
                // 4. For hospitals, we don't create a user document - just return success
                //    The hospital document will be created in the HospitalRegistrationActivity
                if (user.userType == UserType.HOSPITAL) {
                    _authState.value = AuthState.Authenticated(user.copy(id = userId))
                    onResult(Result.success(Unit))
                    return@launch
                }
                
                // 5. For donors, create a user document in the users collection
                try {
                    val newUser = user.copy(id = userId)
                    val saveResult = userRepository.saveUser(newUser)
                    
                    saveResult.onSuccess {
                        _currentUser.value = newUser
                        _authState.value = AuthState.Authenticated(newUser)
                        onResult(Result.success(Unit))
                    }.onFailure { exception ->
                        // If saving to Firestore fails, clean up the auth user
                        auth.currentUser?.delete()
                        _authState.value = AuthState.Error(exception.message ?: "Failed to save user data")
                        onResult(Result.failure(exception))
                    }
                } catch (e: Exception) {
                    // Clean up auth user if Firestore save fails
                    auth.currentUser?.delete()
                    _authState.value = AuthState.Error(e.message ?: "Failed to save user data")
                    onResult(Result.failure(e))
                }
                
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
                onResult(Result.failure(e))
            }
        }
    }
    
    /**
     * Sign out the current user and clear all auth-related data
     */
    fun logout() {
        try {
            // Clear any local data first
            _currentUser.value = null
            _authState.value = AuthState.Loading
            
            // Sign out from Firebase Auth
            auth.signOut()
            
            // Update state
            _authState.value = AuthState.Unauthenticated
            
            Log.d(TAG, "User signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
            _authState.value = AuthState.Error("Error during sign out: ${e.message}")
        }
    }
    
    fun resetAuthState() {
        _authState.value = AuthState.Unauthenticated
    }
    
    /**
     * Sends a password reset email to the specified email address
     * @param email The email address to send the reset link to
     * @param onResult Callback with success status and optional error message
     */
    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank()) {
            onResult(false, "Email cannot be empty")
            return
        }
        
        // First check if email exists in either users or hospitals collection
        viewModelScope.launch {
            try {
                val emailExists = userRepository.doesEmailExist(email)
                if (!emailExists) {
                    onResult(false, "No account found with this email address")
                    return@launch
                }
                
                // If email exists, send password reset
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        onResult(true, "Password reset email sent to $email")
                    }
                    .addOnFailureListener { exception ->
                        val errorMessage = when {
                            exception.message?.contains("invalid email", ignoreCase = true) == true ->
                                "Please enter a valid email address"
                            exception.message?.contains("network", ignoreCase = true) == true ->
                                "Network error. Please check your internet connection"
                            else -> "Failed to send password reset email. Please try again."
                        }
                        onResult(false, errorMessage)
                    }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking email existence", e)
                onResult(false, "An error occurred. Please try again.")
            }
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
