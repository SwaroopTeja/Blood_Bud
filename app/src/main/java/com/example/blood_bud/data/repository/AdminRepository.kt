package com.example.blood_bud.data.repository

import android.util.Log
import com.example.blood_bud.models.Admin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

class AdminRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val adminsCollection by lazy { firestore.collection("admins") }

    suspend fun createAdminWithEmailAndPassword(email: String, password: String, name: String): Result<Admin> {
        Log.d("AdminRepository", "Starting admin creation for email: ${email.take(3)}...")
        
        return try {
            // 1. Validate input
            if (email.isBlank() || password.isBlank() || name.isBlank()) {
                val error = "Validation failed: Email, password, and name are required"
                Log.e("AdminRepository", error)
                return Result.failure(IllegalArgumentException(error))
            }
            
            Log.d("AdminRepository", "Input validation passed")
            
            // 2. Check if admin with this email already exists
            Log.d("AdminRepository", "Checking for existing admin with email: ${email.lowercase()}")
            val querySnapshot = try {
                adminsCollection
                    .whereEqualTo("email", email.lowercase())
                    .limit(1)
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.e("AdminRepository", "Error checking for existing admin", e)
                return Result.failure(e)
            }
                
            if (!querySnapshot.isEmpty) {
                val error = "Admin with this email already exists"
                Log.e("AdminRepository", error)
                return Result.failure(Exception(error))
            }

            Log.d("AdminRepository", "No existing admin found with this email")

            // 3. Create Firebase Auth user
            Log.d("AdminRepository", "Creating Firebase Auth user")
            val authResult = try {
                auth.createUserWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                Log.e("AdminRepository", "Error creating auth user", e)
                return Result.failure(e)
            }
            
            val firebaseUser = authResult.user ?: run {
                val error = "Failed to create user: No user returned from auth"
                Log.e("AdminRepository", error)
                return@createAdminWithEmailAndPassword Result.failure(Exception(error))
            }

            Log.d("AdminRepository", "Firebase Auth user created: ${firebaseUser.uid}")

            // 4. Update user profile with display name
            try {
                Log.d("AdminRepository", "Updating user profile with display name")
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                Log.d("AdminRepository", "User profile updated successfully")
            } catch (e: Exception) {
                Log.e("AdminRepository", "Error updating user profile", e)
                // Continue even if profile update fails
            }

            // 5. Create admin document in Firestore
            val admin = Admin(
                id = firebaseUser.uid,
                email = email.lowercase(),
                name = name.trim(),
                isActive = true,
                isSuperAdmin = false,
                createdAt = System.currentTimeMillis()
            )
            
            Log.d("AdminRepository", "Prepared admin document: $admin")

            // 6. Save admin data to Firestore
            try {
                Log.d("AdminRepository", "Saving admin document to Firestore")
                val adminRef = adminsCollection.document(firebaseUser.uid)
                adminRef.set(admin.toMap()).await()
                Log.d("AdminRepository", "Admin document saved successfully")
                
                // Verify the document was saved
                val savedDoc = adminRef.get().await()
                Log.d("AdminRepository", "Document exists after save: ${savedDoc.exists()}")
                
                Result.success(admin)
            } catch (e: Exception) {
                Log.e("AdminRepository", "Error saving admin to Firestore", e)
                // Clean up auth user if Firestore update fails
                try {
                    Log.d("AdminRepository", "Cleaning up auth user due to Firestore error")
                    firebaseUser.delete().await()
                    Log.d("AdminRepository", "Auth user cleaned up successfully")
                } catch (deleteError: Exception) {
                    Log.e("AdminRepository", "Error cleaning up auth user", deleteError)
                }
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e("AdminRepository", "Unexpected error in createAdminWithEmailAndPassword", e)
            Result.failure(e)
        }
    }

    fun getAllAdmins(): Flow<List<Admin>> = callbackFlow {
        val listener = adminsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Log the error and send empty list
                    Log.e("AdminRepository", "Error fetching admins", e)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                try {
                    Log.d("AdminRepository", "Received ${snapshot?.documents?.size ?: 0} admin documents")
                    snapshot?.documents?.forEach { doc ->
                        Log.d("AdminRepository", "Doc ${doc.id} data: ${doc.data}")
                    }
                    
                    val admins = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val email = doc.getString("email") ?: ""
                            val name = doc.getString("name") ?: ""
                            val isActive = doc.getBoolean("isActive") ?: false
                            val isSuperAdmin = doc.getBoolean("isSuperAdmin") ?: false
                            val createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                            
                            Log.d("AdminRepository", "Processing admin: $email, isSuperAdmin: $isSuperAdmin, isActive: $isActive")
                            
                            Admin(
                                id = doc.id,
                                email = email,
                                name = name,
                                isActive = isActive,
                                isSuperAdmin = isSuperAdmin,
                                createdAt = createdAt
                                    ?: doc.getLong("createdAt") ?: 0L
                            )
                        } catch (e: Exception) {
                            Log.e("AdminRepository", "Error parsing admin document ${doc.id}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(admins)
                } catch (e: Exception) {
                    Log.e("AdminRepository", "Error processing admins", e)
                    trySend(emptyList())
                }
            }
            
        awaitClose { 
            try {
                listener.remove()
            } catch (e: Exception) {
                Log.e("AdminRepository", "Error removing listener", e)
            }
        }
    }
    
    suspend fun getAdminById(adminId: String): Admin? = try {
        val doc = adminsCollection.document(adminId).get().await()
        if (doc.exists()) {
            Admin(
                id = doc.id,
                email = doc.getString("email") ?: "",
                isActive = doc.getBoolean("isActive") ?: false,
                name = doc.getString("name") ?: "",
                isSuperAdmin = doc.getBoolean("isSuperAdmin") ?: false,
                createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
            )
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
    
    suspend fun updateAdminStatus(adminId: String, isActive: Boolean): Result<Unit> {
        return try {
            Log.d("AdminRepository", "Updating admin $adminId isActive to $isActive")
            val updates = hashMapOf<String, Any>(
                "isActive" to isActive,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            adminsCollection.document(adminId)
                .update(updates)
                .await()
                
            Log.d("AdminRepository", "Successfully updated admin status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error updating admin status", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAdminByEmail(email: String): Admin? = try {
        val query = adminsCollection.whereEqualTo("email", email.lowercase(Locale.getDefault()))
        val snapshot = query.get().await()
        snapshot.documents.firstOrNull()?.let { doc ->
            Admin(
                id = doc.id,
                email = doc.getString("email") ?: "",
                isActive = doc.getBoolean("isActive") ?: false,
                name = doc.getString("name") ?: "",
                isSuperAdmin = doc.getBoolean("isSuperAdmin") ?: false,
                createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
            )
        }
    } catch (e: Exception) {
        null
    }

    suspend fun deleteAdmin(adminId: String): Result<Unit> = try {
        adminsCollection.document(adminId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
