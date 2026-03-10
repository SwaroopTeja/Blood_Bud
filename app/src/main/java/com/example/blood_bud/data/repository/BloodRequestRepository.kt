package com.example.blood_bud.data.repository

import com.example.blood_bud.data.model.BloodRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloodRequestRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val bloodRequestsCollection = db.collection("blood_requests")

    // Create a new blood request
    suspend fun createBloodRequest(request: BloodRequest): Result<String> {
        return try {
            val docRef = bloodRequestsCollection.add(request).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get requests by status
    fun getRequestsByStatus(status: String): Flow<List<BloodRequest>> = flow {
        val snapshot = bloodRequestsCollection
            .whereEqualTo("status", status)
            .get()
            .await()
        val requests = snapshot.toObjects(BloodRequest::class.java)
        emit(requests)
    }

    // Get requests by user ID
    fun getRequestsByUserId(userId: String): Flow<List<BloodRequest>> = flow {
        val snapshot = bloodRequestsCollection
            .whereEqualTo("requesterId", userId)
            .get()
            .await()
        val requests = snapshot.toObjects(BloodRequest::class.java)
        emit(requests)
    }

    // Update request status
    suspend fun updateRequestStatus(requestId: String, status: String, notes: String = ""): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to status,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            // Add the appropriate notes field based on status
            if (status == "approved") {
                updates["adminNotes"] = notes
            } else {
                updates["hospitalNotes"] = notes
            }
            
            bloodRequestsCollection.document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}