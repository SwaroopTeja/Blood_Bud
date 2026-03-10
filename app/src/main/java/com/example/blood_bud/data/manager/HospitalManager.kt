package com.example.blood_bud.data.manager

import android.util.Log
import com.example.blood_bud.data.model.AppointmentSlot
import com.example.blood_bud.data.model.Hospital
import com.example.blood_bud.data.model.WorkingHours
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class HospitalManager @Inject constructor(
    private val database: FirebaseDatabase) {
    private val databaseRef: DatabaseReference = database.reference
    private val hospitalsRef: DatabaseReference = databaseRef.child("hospitals")
    private val slotsRef: DatabaseReference = databaseRef.child("appointment_slots")

    suspend fun createOrUpdateHospital(hospital: Hospital): Result<Unit> {
        return try {
            hospitalsRef.child(hospital.id).setValue(hospital).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHospital(hospitalId: String): Result<Hospital> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = hospitalsRef.child(hospitalId).get().await()
            if (!snapshot.exists()) {
                return@withContext Result.failure(NoSuchElementException("Hospital not found"))
            }

            val data = snapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                ?: return@withContext Result.failure(IllegalStateException("Invalid hospital data"))

            val workingHoursData =
                (data["workingHours"] as? Map<*, *>)?.mapValues { it.value?.toString() ?: "" }
            val workingHours = workingHoursData?.let { wh ->
                WorkingHours(
                    open = wh["open"] ?: "09:00",
                    close = wh["close"] ?: "17:00",
                    breakStart = wh["breakStart"] ?: "13:00",
                    breakEnd = wh["breakEnd"] ?: "14:00"
                )
            } ?: WorkingHours()

            // Create address map from the data
            val addressMap = mutableMapOf<String, Any?>()
            listOf("street", "city", "state", "district", "country", "pincode", "postalCode")
                .forEach { key ->
                    data[key]?.toString()?.takeIf { it.isNotBlank() }?.let {
                        addressMap[key] = it
                    }
                }

            // Create phone and email from contact info
            val phone = data["phone"]?.toString() ?: data["contact"]?.toString() ?: ""
            val email = data["email"]?.toString() ?: ""

            val hospital = Hospital(
                id = hospitalId,
                name = data["name"]?.toString() ?: "",
                address = addressMap,
                street = addressMap["street"]?.toString() ?: "",
                city = addressMap["city"]?.toString() ?: "",
                state = addressMap["state"]?.toString() ?: "",
                district = addressMap["district"]?.toString() ?: "",
                country = addressMap["country"]?.toString() ?: "India",
                pincode = addressMap["pincode"]?.toString() ?: "",
                postalCode = addressMap["postalCode"]?.toString() ?: "",
                phone = phone,
                email = email,
                latitude = (data["latitude"] as? Double) ?: 0.0,
                longitude = (data["longitude"] as? Double) ?: 0.0,
                availableSlots = (data["availableSlots"] as? Number)?.toInt() ?: 0,
                slotDuration = (data["slotDuration"] as? Number)?.toInt() ?: 30,
                slotsPerDay = (data["slotsPerDay"] as? Number)?.toInt() ?: 20,
                workingHours = workingHours
            )

            if (hospital != null) {
                Result.success(hospital)
            } else {
                Result.failure(NoSuchElementException("Hospital not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeHospital(hospitalId: String): Flow<Hospital> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    close(NoSuchElementException("Hospital not found"))
                    return
                }

                val data = snapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                    ?: return

                val workingHoursData =
                    (data["workingHours"] as? Map<*, *>)?.mapValues { it.value?.toString() ?: "" }
                val workingHours = workingHoursData?.let { wh ->
                    WorkingHours(
                        open = wh["open"] ?: "09:00",
                        close = wh["close"] ?: "17:00",
                        breakStart = wh["breakStart"] ?: "13:00",
                        breakEnd = wh["breakEnd"] ?: "14:00"
                    )
                } ?: WorkingHours()

                // Create address map from the data
                val addressMap = mutableMapOf<String, Any?>()
                listOf("street", "city", "state", "district", "country", "pincode", "postalCode")
                    .forEach { key ->
                        data[key]?.toString()?.takeIf { it.isNotBlank() }?.let {
                            addressMap[key] = it
                        }
                    }

                // Create phone and email from contact info
                val phone = data["phone"]?.toString() ?: data["contact"]?.toString() ?: ""
                val email = data["email"]?.toString() ?: ""

                val hospital = Hospital(
                    id = hospitalId,
                    name = data["name"]?.toString() ?: "",
                    address = addressMap,
                    street = addressMap["street"]?.toString() ?: "",
                    city = addressMap["city"]?.toString() ?: "",
                    state = addressMap["state"]?.toString() ?: "",
                    district = addressMap["district"]?.toString() ?: "",
                    country = addressMap["country"]?.toString() ?: "India",
                    pincode = addressMap["pincode"]?.toString() ?: "",
                    postalCode = addressMap["postalCode"]?.toString() ?: "",
                    phone = phone,
                    email = email,
                    latitude = (data["latitude"] as? Double) ?: 0.0,
                    longitude = (data["longitude"] as? Double) ?: 0.0,
                    availableSlots = (data["availableSlots"] as? Number)?.toInt() ?: 0,
                    slotDuration = (data["slotDuration"] as? Number)?.toInt() ?: 30,
                    slotsPerDay = (data["slotsPerDay"] as? Number)?.toInt() ?: 20,
                    workingHours = workingHours,
                    bloodInventory = (data["bloodInventory"] as? Map<String, Int>) ?: emptyMap(),
                    additionalInfo = data["additionalInfo"]?.toString() ?: "",
                    status = data["status"]?.toString() ?: "active"
                )

                trySend(hospital)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = hospitalsRef.child(hospitalId)
        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    fun observeAvailableSlots(hospitalId: String, date: String): Flow<List<AppointmentSlot>> =
        callbackFlow {
            val slotsRef = slotsRef.child(date).child(hospitalId)

            val valueListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val slots = mutableListOf<AppointmentSlot>()

                    snapshot.children.forEach { slotSnapshot ->
                        try {
                            val slotId = slotSnapshot.key ?: return@forEach
                            val slotData = slotSnapshot.getValue(object :
                                GenericTypeIndicator<Map<String, Any>>() {}) ?: return@forEach

                            val bookedBy =
                                (slotData["bookedBy"] as? List<*>)?.filterIsInstance<String>()
                                    ?: emptyList()

                            // Convert timestamps to Long if they're strings
                            val startTime = when (val st = slotData["startTime"]) {
                                is String -> st.toLongOrNull() ?: 0L
                                is Number -> st.toLong()
                                else -> 0L
                            }

                            val endTime = when (val et = slotData["endTime"]) {
                                is String -> et.toLongOrNull() ?: 0L
                                is Number -> et.toLong()
                                else -> 0L
                            }

                            val slot = AppointmentSlot(
                                id = slotId,
                                hospitalId = hospitalId,
                                date = date,
                                startTime = startTime,
                                endTime = endTime,
                                capacity = (slotData["capacity"] as? Number)?.toInt() ?: 1,
                                currentBookings = (slotData["currentBookings"] as? Number)?.toInt()
                                    ?: 0,
                                bookedBy = bookedBy,
                                isActive = slotData["isActive"] as? Boolean ?: true
                            )

                            slots.add(slot)
                        } catch (e: Exception) {
                            Log.e("HospitalManager", "Error parsing slot data: ${e.message}")
                        }
                    }

                    trySend(slots)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            slotsRef.addValueEventListener(valueListener)

            awaitClose {
                slotsRef.removeEventListener(valueListener)
            }
        }


    suspend fun generateSlotsForDateRange(
        hospitalId: String,
        startDate: Date,
        endDate: Date,
        workingHours: WorkingHours = WorkingHours(),
        slotDuration: Int = 30
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val slotsRef = databaseRef.child("appointment_slots")
        return@withContext try {
            val calendar = Calendar.getInstance()
            calendar.time = startDate

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            while (calendar.time <= endDate) {
                val dateStr = dateFormat.format(calendar.time)
                val dateSlotsRef = slotsRef.child(dateStr).child(hospitalId)

                // Check if slots already exist for this date
                val existingSlots = dateSlotsRef.get().await()
                if (!existingSlots.exists()) {
                    // Generate time slots for this date
                    val slots = generateTimeSlots(calendar.time, workingHours, slotDuration)
                    dateSlotsRef.setValue(slots).await()
                }

                // Move to next day
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateTimeSlots(
        date: Date,
        workingHours: WorkingHours,
        slotDuration: Int
    ): Map<String, Any> {
        val slots = mutableMapOf<String, Any>()
        val calendar = Calendar.getInstance()
        calendar.time = date

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Parse working hours
        val (startHour, startMinute) = parseTime(workingHours.open)
        val (endHour, endMinute) = parseTime(workingHours.close)
        val (breakStartHour, breakStartMinute) = parseTime(workingHours.breakStart)
        val (breakEndHour, breakEndMinute) = parseTime(workingHours.breakEnd)

        // Set initial time
        calendar.set(Calendar.HOUR_OF_DAY, startHour)
        calendar.set(Calendar.MINUTE, startMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val endTime = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val breakStart = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, breakStartHour)
            set(Calendar.MINUTE, breakStartMinute)
        }

        val breakEnd = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, breakEndHour)
            set(Calendar.MINUTE, breakEndMinute)
        }

        // Generate slots
        var slotNumber = 1
        while (calendar.time.before(endTime.time)) {
            // Skip break time
            if (calendar.time.after(breakStart.time) && calendar.time.before(breakEnd.time)) {
                calendar.add(Calendar.MINUTE, slotDuration)
                continue
            }

            val slotTime = calendar.time
            val slotKey = "slot_${String.format("%02d", slotNumber)}"

            slots[slotKey] = mapOf(
                "startTime" to timeFormat.format(slotTime),
                "endTime" to timeFormat.format(Date(slotTime.time + slotDuration * 60 * 1000)),
                "maxCapacity" to 5,
                "currentBookings" to 0,
                "isActive" to true
            )

            slotNumber++
            calendar.add(Calendar.MINUTE, slotDuration)
        }

        return slots
    }

    private fun parseTime(time: String): Pair<Int, Int> {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val parsedTime = timeFormat.parse(time) ?: return 0 to 0
        val calendar = Calendar.getInstance()
        calendar.time = parsedTime
        return calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
    }

    suspend fun updateSlotCapacity(
        date: String,
        hospitalId: String,
        slotId: String,
        change: Int,
        donorId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val slotRef = database.reference.child("appointment_slots")
                .child(date)
                .child(hospitalId)
                .child(slotId)

            suspendCoroutine<Result<Unit>> { continuation ->
                val transactionHandler = object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        try {
                            val slot = currentData.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})?.toMutableMap()
                                ?: return Transaction.success(currentData)

                            // Update current bookings count
                            val currentBookings = (slot["currentBookings"] as? Number)?.toInt() ?: 0
                            val newBookings = currentBookings + change
                            val capacity = (slot["capacity"] as? Number)?.toInt() ?: 1

                            if (newBookings < 0 || newBookings > capacity) {
                                return Transaction.abort()
                            }

                            // Update bookedBy map if donorId is provided
                            if (donorId != null) {
                                val bookedBy = (slot["bookedBy"] as? MutableMap<String, Any>)?.toMutableMap() ?: mutableMapOf()
                                if (change > 0) {
                                    // Add/update the booking with timestamp
                                    bookedBy[donorId] = System.currentTimeMillis()
                                } else {
                                    // Remove the booking
                                    bookedBy.remove(donorId)
                                }
                                slot["bookedBy"] = bookedBy
                            }

                            slot["currentBookings"] = newBookings
                            currentData.value = slot
                            return Transaction.success(currentData)
                        } catch (e: Exception) {
                            Log.e("HospitalManager", "Transaction error: ${e.message}")
                            return@doTransaction Transaction.abort()
                        }
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (error != null) {
                            Log.e("HospitalManager", "Transaction failed: ${error.message}")
                            continuation.resume(Result.failure(Exception(error.message)))
                        } else if (!committed) {
                            Log.e("HospitalManager", "Transaction not committed")
                            continuation.resume(Result.failure(Exception("Failed to update slot capacity")))
                        } else {
                            continuation.resume(Result.success(Unit))
                        }
                    }
                }

                // Run the transaction
                database.reference.runTransaction(transactionHandler)
            }
        } catch (e: Exception) {
            Log.e("HospitalManager", "Error in updateSlotCapacity: ${e.message}", e)
            Result.failure(e)
        }
    }


}