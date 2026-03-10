package com.example.blood_bud

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BloodBudApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            
            // Enable offline persistence for Firestore
            // Firebase.firestore.setPersistenceEnabled(true)
            
            // Verify Firebase Auth instance is available
            val auth = Firebase.auth
            Log.d("BloodBudApp", "Firebase Auth initialized. Current user: ${auth.currentUser?.uid ?: "none"}")
            
        } catch (e: Exception) {
            Log.e("BloodBudApp", "Error initializing Firebase", e)
        }
    }
}
