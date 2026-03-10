package com.example.blood_bud.data.repository

import android.util.Log
import com.example.blood_bud.data.model.Address
import com.example.blood_bud.data.model.User
import com.example.blood_bud.data.model.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    internal val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    private val usersCollection = db.collection("users")

    /**
     * Check if there is a currently authenticated user
     */
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Get current user data
    suspend fun getCurrentUser(): User? {
        return try {
            val userId = auth.currentUser?.uid ?: run {
                Log.e("UserRepository", "No authenticated user found")
                return null
            }
            
            Log.d("UserRepository", "Fetching user data for ID: $userId")
            val document = usersCollection.document(userId).get().await()
            
            if (!document.exists()) {
                Log.e("UserRepository", "User document does not exist for ID: $userId")
                // Check if this is a hospital user
                val hospitalDoc = db.collection("hospitals").document(userId).get().await()
                if (hospitalDoc.exists()) {
                    Log.d("UserRepository", "Found hospital user with ID: $userId")
                    return hospitalDoc.toObject(User::class.java)?.copy(id = hospitalDoc.id)
                }
                return null
            }
            
            document.toObject(User::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting current user: ${e.message}", e)
            null
        }
    }

    // Create or update user
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            // Don't save system users or hospital users to the users collection
            if (user.isSystemUser || user.userType == UserType.HOSPITAL) {
                Log.d("UserRepository", "Skipping save for system/hospital user: ${user.id}")
                return Result.success(Unit)
            }
            
            Log.d("UserRepository", "Saving user to Firestore: ${user.id}")
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving user", e)
            Result.failure(e)
        }
    }

    // Get user by ID
    // Get all users (excluding hospitals, they are managed separately)
    suspend fun getAllUsers(): List<User> {
        return try {
            // Only get non-hospital users
            usersCollection.get().await().toObjects(User::class.java)
                .filter { it.userType != UserType.HOSPITAL }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting all users", e)
            emptyList()
        }
    }
    
    // Delete a user
    suspend fun deleteUser(userId: String): Boolean {
        return try {
            // Check if user is a hospital
            val isHospital = db.collection("hospitals").document(userId).get().await().exists()
            val collection = if (isHospital) "hospitals" else "users"
            
            // Delete the user document
            db.collection(collection).document(userId).delete().await()
            
            // Also delete the authentication record if it exists
            try {
                FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                    if (currentUser.uid == userId) {
                        currentUser.delete()?.await()
                    }
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Error deleting auth user", e)
                // Continue even if auth deletion fails
            }
            
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting user", e)
            throw e
        }
    }
    
    // Update user status
    suspend fun updateUserStatus(userId: String, isActive: Boolean): Boolean {
        return try {
            // Check if user is a hospital
            val isHospital = db.collection("hospitals").document(userId).get().await().exists()
            val collection = if (isHospital) "hospitals" else "users"
            
            db.collection(collection)
                .document(userId)
                .update("isActive", isActive)
                .await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user status", e)
            false
        }
    }
    
    // Check if the current user is an admin
    suspend fun isCurrentUserAdmin(): Boolean {
        return try {
            val currentUser = auth.currentUser ?: run {
                Log.d("UserRepository", "No authenticated user")
                return false
            }
            
            // First check users collection
            val userDoc = usersCollection.document(currentUser.uid).get().await()
            if (userDoc.exists()) {
                val user = userDoc.toObject(User::class.java)
                val isAdmin = user?.userType == UserType.ADMIN
                Log.d("UserRepository", "User is admin: $isAdmin")
                return isAdmin
            }
            
            // If not found in users, check hospitals collection
            val hospitalDoc = db.collection("hospitals").document(currentUser.uid).get().await()
            if (hospitalDoc.exists()) {
                val hospitalData = hospitalDoc.data
                val isAdmin = hospitalData?.get("isAdmin") as? Boolean ?: false
                Log.d("UserRepository", "Hospital user admin status: $isAdmin")
                return isAdmin
            }
            
            Log.d("UserRepository", "User not found in any collection")
            false
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking admin status: ${e.message}", e)
            false
        }
    }
    
    suspend fun getUserById(userId: String): User? {
        return try {
            // First check if this is a hospital user
            val hospitalDoc = db.collection("hospitals").document(userId).get().await()
            if (hospitalDoc.exists()) {
                val data = hospitalDoc.data ?: return null
                return User(
                    id = hospitalDoc.id,
                    email = data["email"] as? String ?: "",
                    name = data["name"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    userType = UserType.HOSPITAL,
                    isSystemUser = true,
                    isActive = data["isActive"] as? Boolean ?: true,
                    address = (data["address"] as? Map<String, Any?>)?.let { addressMap ->
                        Address(
                            street = addressMap["street"] as? String ?: "",
                            city = addressMap["city"] as? String ?: "",
                            state = addressMap["state"] as? String ?: "",
                            postalCode = addressMap["postalCode"] as? String ?: "",
                            country = addressMap["country"] as? String ?: "India",
                            latitude = (addressMap["latitude"] as? Number)?.toDouble() ?: 0.0,
                            longitude = (addressMap["longitude"] as? Number)?.toDouble() ?: 0.0
                        )
                    } ?: Address() // Initialize with empty address if not found
                )
            }
            
            // If not a hospital, check regular users collection
            usersCollection.document(userId).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user by ID: $userId", e)
            null
        }
    }

    // Check if email exists in either users or hospitals collection
    suspend fun doesEmailExist(email: String): Boolean {
        return try {
            // Check in users collection
            val userQuery = usersCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            if (!userQuery.isEmpty) return true
            
            // Check in hospitals collection
            val hospitalQuery = db.collection("hospitals")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
                
            !hospitalQuery.isEmpty
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking if email exists: $email", e)
            false
        }
    }
}