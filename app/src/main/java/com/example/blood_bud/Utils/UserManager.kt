package com.example.blood_bud.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.blood_bud.Activity.AccountActivity
import com.example.blood_bud.Activity.HospitalDashboardActivity
import com.example.blood_bud.Activity.HospitalRegistrationActivity
import com.example.blood_bud.Activity.RegistrationTypeActivity
import com.example.blood_bud.data.model.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserManager {
    private const val TAG = "UserManager"
    private const val USERS_COLLECTION = "users"
    
    /**
     * Check user authentication state and role, then redirect accordingly
     * @param context The context to start activities
     * @param onComplete Callback when the check is complete
     */
    fun checkUserRoleAndRedirect(context: Context, onComplete: (() -> Unit)? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d(TAG, "No user is signed in")
            // No user is signed in, ensure we're on the login screen
            onComplete?.invoke()
            return
        }
        
        Log.d(TAG, "User is signed in: ${currentUser.uid}")
        
        // Check if this is a hospital user first
        FirebaseFirestore.getInstance()
            .collection("hospitals")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { hospitalDoc ->
                if (hospitalDoc.exists() && hospitalDoc.getBoolean("isActive") != false) {
                    // This is an active hospital user
                    Log.d(TAG, "Hospital user logged in, redirecting to hospital dashboard")
                    redirectToHospitalDashboard(context)
                } else {
                    // Not a hospital or inactive, check regular users collection
                    checkRegularUserRole(context, currentUser.uid)
                }
                onComplete?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error checking hospital user: ${exception.message}")
                // Fall back to checking regular users collection
                checkRegularUserRole(context, currentUser.uid)
                onComplete?.invoke()
            }
    }
    
    private fun checkRegularUserRole(context: Context, userId: String) {
        FirebaseFirestore.getInstance()
            .collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userType = document.getString("userType") ?: UserType.DONOR.name
                    Log.d(TAG, "User type: $userType")
                    
                    when (userType.uppercase()) {
                        UserType.ADMIN.name -> redirectToAdminDashboard(context)
                        UserType.DONOR.name -> redirectToDefaultDashboard(context)
                        else -> redirectToDefaultDashboard(context)
                    }
                } else {
                    Log.d(TAG, "No user data found, redirecting to default dashboard")
                    redirectToDefaultDashboard(context)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting user data: ${exception.message}")
                redirectToDefaultDashboard(context)
            }
    }
    
    private fun redirectToAdminDashboard(context: Context) {
        // TODO: Replace with actual admin dashboard activity
        val intent = Intent(context, AccountActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("is_admin", true)
        }
        context.startActivity(intent)
    }
    
    private fun redirectToHospitalDashboard(context: Context) {
        val intent = Intent(context, HospitalDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
    
    private fun redirectToLogin(context: Context) {
        // Redirect to the login screen (AccountActivity)
        val intent = Intent(context, AccountActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
    
    private fun redirectToDefaultDashboard(context: Context) {
        // Redirect to the main account screen
        val intent = Intent(context, AccountActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
    
    private fun redirectToHospitalRegistration(context: Context) {
        // For hospital users, redirect to hospital dashboard
        redirectToHospitalDashboard(context)
    }
    
    /**
     * Check if a user is currently logged in
     * Note: This only checks if there's a current user in Firebase Auth
     * For a complete check including role validation, use checkUserRoleAndRedirect
     */
    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }
    
    /**
     * Sign out the current user
     * @param context The context to use for any UI operations
     * @param onComplete Callback when sign out is complete
     */
    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        try {
            FirebaseAuth.getInstance().signOut()
            // Clear any local data if needed
            // ...
            
            // Redirect to login screen
            val intent = Intent(context, AccountActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            
            onComplete()
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out: ${e.message}")
            onComplete()
        }
    }
}
