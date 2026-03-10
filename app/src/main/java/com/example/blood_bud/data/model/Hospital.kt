package com.example.blood_bud.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.text.SimpleDateFormat
import java.util.Locale

@Parcelize
data class Hospital(
    val id: String = "",
    val name: String = "",
    val address: @RawValue Map<String, Any?> = emptyMap(),
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val district: String = "",
    val country: String = "India",
    val pincode: String = "",
    val postalCode: String = "",
    val phone: String = "",
    val email: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val availableSlots: Int = 0,
    val slotDuration: Int = 30, // in minutes
    val slotsPerDay: Int = 20,
    val workingHours: WorkingHours = WorkingHours(),
    val bloodInventory: Map<String, Int> = emptyMap(),
    val additionalInfo: String = "",
    val status: String = "active"
) : Parcelable {
    val fullAddress: String
        get() = buildString {
            val street = address["street"] as? String ?: ""
            val city = address["city"] as? String ?: city
            val state = address["state"] as? String ?: state
            val postalCode = address["postalCode"] as? String ?: ""
            
            if (street.isNotBlank()) append("$street, ")
            if (city.isNotBlank()) append("$city, ")
            if (state.isNotBlank()) append(state)
            if (postalCode.isNotBlank()) append(" - $postalCode")
            
            if (isEmpty()) append("Address not available")
        }
}

@Parcelize
data class HospitalWithDistance(
    val hospital: Hospital,
    val distanceKm: Double
) : Parcelable

/**
 * Represents working hours for a hospital
 */
@Parcelize
data class WorkingHours(
    val open: String = "09:00",
    val close: String = "17:00",
    val breakStart: String = "13:00",
    val breakEnd: String = "14:00"
) : Parcelable {
    fun isWithinWorkingHours(timeInMillis: Long): Boolean {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(timeInMillis)
        
        return currentTime in open..breakStart || currentTime in breakEnd..close
    }
}

/**
 * Represents a time slot for appointments
 */
@Parcelize
data class AppointmentSlot(
    val id: String = generateSlotId(),
    val hospitalId: String = "",
    val hospitalName: String = "",
    val date: String = "", // yyyy-MM-dd format
    val startTime: Long = 0, // epoch millis
    val endTime: Long = 0,   // epoch millis
    val slotDuration: Int = 30, // minutes per slot
    val capacity: Int = 10, // max donors per slot
    val currentBookings: Int = 0,
    val breakStart: Long? = null, // lunch break start time (epoch millis)
    val breakEnd: Long? = null,   // lunch break end time (epoch millis)
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val bookedBy: List<String> = emptyList() // list of donor IDs who booked this slot
) : Parcelable {

    // Helper function to check if slot is available for booking
    fun isAvailable(): Boolean {
        return isActive && currentBookings < capacity
    }

    // Helper function to get time in readable format
    fun getTimeRange(): String {
        return "${formatTime(startTime)} - ${formatTime(endTime)}"
    }

    private fun formatTime(epochMillis: Long): String {
        val time = java.util.Date(epochMillis)
        val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return formatter.format(time)
    }

    companion object {
        private fun generateSlotId(): String {
            // Generate Firebase-compatible ID using timestamp and random component
            val timestamp = System.currentTimeMillis()
            val random = (Math.random() * 1000).toInt()
            return "slot_${timestamp}_${random}"
        }
    }
}