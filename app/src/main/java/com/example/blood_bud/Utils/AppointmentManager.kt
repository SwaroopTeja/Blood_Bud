package com.example.blood_bud.utils

import android.util.Log
import com.example.blood_bud.data.manager.HospitalManager
import com.example.blood_bud.data.model.AppointmentSlot
import com.example.blood_bud.data.model.DonationStatus
import com.example.blood_bud.data.model.DonorHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentManager @Inject constructor(
    private val hospitalManager: HospitalManager,
    private val auth: FirebaseAuth
) {
    private val realtimeDb = FirebaseDatabase.getInstance()
    private val slotsRef = realtimeDb.getReference("appointment_slots")
    private val historyRef = realtimeDb.getReference("donation_history")
    private val currentUser = auth.currentUser
    private val currentUserId: String
    
    init {
        currentUserId = currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }
    
    companion object {
        private const val TAG = "AppointmentManager"
    }

    /**
     * Clean up old appointment slots (older than today) and move completed donations to history
     * This should be called daily via a scheduled task
     */
    suspend fun performDailyMaintenance(): Boolean {
        return try {
            // 1. Clean up expired slots
            val cleanupSuccess = cleanupExpiredSlots()
            
            // 2. Move completed donations to history
            val historySuccess = moveCompletedDonationsToHistory()
            
            cleanupSuccess && historySuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error during daily maintenance", e)
            false
        }
    }
    
    private suspend fun cleanupExpiredSlots(): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            
            // Get all slots
            val slotsSnapshot = slotsRef.get().await()
            
            slotsSnapshot.children.forEach { dateSnapshot ->
                val date = dateSnapshot.key ?: return@forEach
                
                // Skip today and future dates
                if (date >= today) return@forEach
                
                // For each hospital on this date
                dateSnapshot.children.forEach { hospitalSnapshot ->
                    val hospitalId = hospitalSnapshot.key ?: return@forEach
                    // For each slot in this hospital
                    hospitalSnapshot.children.forEach { slotSnapshot ->
                        val slotId = slotSnapshot.key ?: return@forEach
                        val slot = slotSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                        if (slot != null) {
                            // If the slot has bookings, move them to history
                            if ((slot["currentBookings"] as? Long ?: 0) > 0) {
                                moveSlotToHistory(date, hospitalId, slotId)
                            }
                            // Remove the slot
                            slotSnapshot.ref.removeValue().await()
                        }
                    }
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up expired slots", e)
            false
        }
    }
    
    private suspend fun moveSlotsToArchive(dateStr: String): Boolean {
        return try {
            val dateSlotsRef = slotsRef.child(dateStr)
            val dateSlots = dateSlotsRef.get().await()
            
            if (!dateSlots.exists()) return true
            
            // Move to archive/date/ instead of deleting
            val archiveRef = realtimeDb.getReference("archive/appointment_slots").child(dateStr)
            archiveRef.setValue(dateSlots.value).await()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error moving slots to archive", e)
            false
        }
    }

    /**
     * Move completed donations to history collection for both hospitals and donors
     */
    private suspend fun moveCompletedDonationsToHistory(): Boolean {
        return try {
            val today = getCurrentDate()
            val todaySlots = slotsRef.child(today).get().await()
            
            if (!todaySlots.exists()) return true

            var movedCount = 0

            todaySlots.children.forEach { hospitalSnapshot ->
                val hospitalId = hospitalSnapshot.key ?: return@forEach
                
                hospitalSnapshot.children.forEach { slotSnapshot ->
                    val slotId = slotSnapshot.key ?: return@forEach
                    val slotData = slotSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {}) ?: return@forEach
                    
                    val currentBookings = (slotData["currentBookings"] as? Long)?.toInt() ?: 0
                    val capacity = (slotData["capacity"] as? Long)?.toInt() ?: 0
                    val bookedBy = (slotData["bookedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    
                    // If slot is fully booked, move to history
                    if (currentBookings >= capacity && bookedBy.isNotEmpty()) {
                        if (moveSlotToHistory(today, hospitalId, slotId)) {
                            movedCount++
                        }
                    }
                }
            }
            
            Log.d(TAG, "Moved $movedCount completed slots to history")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error moving completed donations to history", e)
            false
        }
    }

    /**
     * Check if donor can book a slot (must be 56+ days since last donation)
     * Uses the new donation history structure
     */
    suspend fun canDonorBookSlot(donorId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            // Get donor's last completed donation from history
            val historyRef = historyRef.child("donors").child(donorId)
            val snapshot = historyRef.orderByChild("completedAt").limitToLast(1).get().await()
            
            if (!snapshot.exists()) {
                return@withContext Pair(true, "No previous donations found")
            }
            
            // Find the most recent completed donation
            var lastDonationTime: Long? = null
            for (child in snapshot.children) {
                val donation = child.getValue(object : GenericTypeIndicator<Map<String, Any>>() {}) ?: continue
                val status = donation["status"] as? String
                val completedAt = donation["completedAt"] as? Long
                
                if (status == "COMPLETED" && completedAt != null) {
                    if (lastDonationTime == null || completedAt > lastDonationTime) {
                        lastDonationTime = completedAt
                    }
                }
            }
            
            if (lastDonationTime == null) {
                return@withContext Pair(true, "No completed donations found")
            }
                
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = lastDonationTime
            calendar.add(Calendar.DAY_OF_YEAR, 56) // 56 days = 8 weeks minimum between donations
            
            val currentTime = System.currentTimeMillis()
            val canDonate = currentTime > calendar.timeInMillis
            
            val message = if (canDonate) {
                "Eligible to donate"
            } else {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val nextEligibleDate = dateFormat.format(Date(calendar.timeInMillis))
                "Next eligible donation date: $nextEligibleDate (56 days after last donation)"
            }
            
            Log.d(TAG, "Donation eligibility check - Last donation: ${Date(lastDonationTime)}, Can donate: $canDonate, Message: $message")
            Pair(canDonate, message)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking donor eligibility", e)
            Pair(false, "Error checking eligibility: ${e.message}")
        }
    }

    /**
     * Book a slot for a donor
     */
    suspend fun bookSlot(
        date: String,
        hospitalId: String,
        slotId: String,
        donorId: String = currentUserId,
        donorName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting booking process for donor: $donorId, slot: $slotId, date: $date")
            
            // Check if donor is eligible to book
            Log.d(TAG, "Checking donor eligibility...")
            val (canBook, message) = canDonorBookSlot(donorId)
            if (!canBook) {
                Log.e(TAG, "Donor not eligible: $message")
                return@withContext Result.failure(IllegalStateException(message))
            }
            
            // Update slot capacity through HospitalManager
            Log.d(TAG, "Updating slot capacity...")
            val updateResult = hospitalManager.updateSlotCapacity(
                date = date,
                hospitalId = hospitalId,
                slotId = slotId,
                change = 1,
                donorId = donorId
            )
            
            if (updateResult.isFailure) {
                val error = updateResult.exceptionOrNull() ?: Exception("Failed to update slot capacity")
                Log.e(TAG, "Error updating slot capacity: ${error.message}", error)
                return@withContext Result.failure(error)
            }
            
            // Create history entry for donor
            Log.d(TAG, "Creating history entry...")
            val historyPath = "donors/$donorId/$date/$slotId"
            Log.d(TAG, "History path: $historyPath")
            
            val historyData = mapOf(
                "hospitalId" to hospitalId,
                "slotId" to slotId,
                "donorId" to donorId,
                "donorName" to donorName,
                "date" to date,
                "bookedAt" to System.currentTimeMillis(),
                "status" to "BOOKED"
            )
            
            Log.d(TAG, "History data: $historyData")
            
            try {
                historyRef.child("donors")
                    .child(donorId)
                    .child(date)
                    .child(slotId)
                    .updateChildren(historyData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully updated history")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update history: ${e.message}", e)
                    }
                    .await()
                
                Log.d(TAG, "Booking successful!")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating history: ${e.message}", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in bookSlot: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Mark a donation as completed
     */
    suspend fun completeDonation(
        slotId: String,
        hospitalId: String,
        donorId: String,
        bloodType: String,
        units: Int,
        date: String = getCurrentDate()
    ): Boolean {
        return try {
            val now = System.currentTimeMillis()

            // Update donor's history
            val donorHistoryRef = historyRef.child("donors")
                .child(donorId)
                .child(date)
                .child(slotId)

            val donorUpdates = mapOf(
                "status" to "COMPLETED",
                "bloodType" to bloodType,
                "unitsDonated" to units,
                "completedAt" to now
            )
            donorHistoryRef.updateChildren(donorUpdates).await()

            // Update hospital's history
            val hospitalHistoryRef = historyRef.child("hospitals")
                .child(hospitalId)
                .child(date)
                .child(slotId)
                .child(donorId)

            val hospitalUpdates = mapOf(
                "status" to "COMPLETED",
                "bloodType" to bloodType,
                "unitsDonated" to units,
                "completedAt" to now
            )
            hospitalHistoryRef.updateChildren(hospitalUpdates).await()

            Log.d(TAG, "Marked donation as completed for donor $donorId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error completing donation", e)
            false
        }
    }

    private suspend fun moveSlotToHistory(
        date: String,
        hospitalId: String,
        slotId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val slotRef = slotsRef.child(date).child(hospitalId).child(slotId)
            val slotSnapshot = slotRef.get().await()
            
            if (!slotSnapshot.exists()) return@withContext false
            
            val slotData = slotSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {}) ?: 
                return@withContext false
                
            val donorIds = (slotData["bookedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            
            // Move slot data to history for each donor
            donorIds.forEach { donorId ->
                val historyRef = historyRef.child("donors")
                    .child(donorId)
                    .child(date)
                    .child(slotId)
                
                val historyData = hashMapOf<String, Any>(
                    "hospitalId" to hospitalId,
                    "slotId" to slotId,
                    "donationDate" to date,
                    "status" to "COMPLETED",
                    "completedAt" to System.currentTimeMillis()
                )
                
                historyRef.updateChildren(historyData).await()
            }
            
            // Mark the slot as inactive instead of deleting it
            slotRef.child("isActive").setValue(false).await()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error moving slot to history", e)
            false
        }
    }

    /**
     * Get donation history for a donor
     */
    fun getDonorHistory(donorId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val historyRef = historyRef.child("donors").child(donorId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val history = mutableListOf<Map<String, Any>>()

                snapshot.children.forEach { dateSnapshot ->
                    val date = dateSnapshot.key ?: return@forEach
                    dateSnapshot.children.forEach { slotSnapshot ->
                        val slotId = slotSnapshot.key ?: return@forEach
                        val entry = hashMapOf<String, Any>(
                            "date" to date,
                            "slotId" to slotId
                        )
                        
                        slotSnapshot.children.forEach { field ->
                            val key = field.key ?: return@forEach
                            val value = field.getValue(Any::class.java) ?: return@forEach
                            entry[key] = value
                        }
                        
                        history.add(entry)
                    }
                }

                // Sort by date and time (newest first)
                val sortedHistory = history.sortedByDescending {
                    (it["completedAt"] as? Long) ?: (it["bookedAt"] as? Long) ?: 0L
                }

                trySend(sortedHistory)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        historyRef.addValueEventListener(listener)

        awaitClose {
            historyRef.removeEventListener(listener)
        }
    }

    /**
     * Get donation history for a hospital
     */
    fun getHospitalHistory(hospitalId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val historyRef = historyRef.child("hospitals").child(hospitalId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val history = mutableListOf<Map<String, Any>>()

                snapshot.children.forEach { dateSnapshot ->
                    val date = dateSnapshot.key ?: return@forEach
                    dateSnapshot.children.forEach { slotSnapshot ->
                        val slotId = slotSnapshot.key ?: return@forEach
                        slotSnapshot.children.forEach { donorSnapshot ->
                            val donorId = donorSnapshot.key ?: return@forEach
                            
                            // Create a new mutable map for each entry
                            val entry = mutableMapOf<String, Any>(
                                "date" to date,
                                "slotId" to slotId,
                                "donorId" to donorId
                            )
                            
                            // Add all fields from the donor snapshot
                            donorSnapshot.children.forEach { field ->
                                val key = field.key ?: return@forEach
                                val value = field.getValue(Any::class.java) ?: return@forEach
                                entry[key] = value
                            }
                            
                            history.add(entry)
                        }
                    }
                }

                // Sort by date and time (newest first)
                val sortedHistory = history.sortedByDescending { entry ->
                    val completedAt = entry["completedAt"]
                    val bookedAt = entry["bookedAt"]
                    
                    when {
                        completedAt is Long -> completedAt
                        bookedAt is Long -> bookedAt
                        completedAt is String -> completedAt.toLongOrNull() ?: 0L
                        bookedAt is String -> bookedAt.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                }

                trySend(sortedHistory)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        historyRef.addValueEventListener(listener)
        
        awaitClose {
            historyRef.removeEventListener(listener)
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun isDateBeforeToday(dateStr: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateStr) ?: return true
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val compareDate = Calendar.getInstance()
            compareDate.time = date
            compareDate.set(Calendar.HOUR_OF_DAY, 0)
            compareDate.set(Calendar.MINUTE, 0)
            compareDate.set(Calendar.SECOND, 0)
            compareDate.set(Calendar.MILLISECOND, 0)

            compareDate.before(today)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateStr", e)
            true // Assume old if can't parse
        }
    }
}
