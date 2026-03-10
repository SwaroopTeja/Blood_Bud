package com.example.blood_bud.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a blood donation appointment
 *
 * @property appointmentId The unique ID of the appointment
 * @property donorId The ID of the donor who made the appointment
 * @property hospitalId The ID of the hospital where the appointment is scheduled
 * @property hospitalName The name of the hospital
 * @property hospitalAddress The address of the hospital
 * @property slotId The ID of the time slot for the appointment
 * @property date The timestamp of the appointment date
 * @property appointmentDate Formatted date string (e.g., "Oct 15, 2023")
 * @property appointmentTime Formatted time string (e.g., "10:00 AM")
 * @property status The status of the appointment (pending, confirmed, completed, cancelled)
 * @property createdAt The timestamp when the appointment was created
 * @property notes Any additional notes for the appointment
 */
data class Appointment(
    @get:PropertyName("id") var id: String = "",
    @get:PropertyName("appointmentId") var appointmentId: String = "",
    @get:PropertyName("donorId") var donorId: String = "",
    @get:PropertyName("donorName") var donorName: String = "",
    @get:PropertyName("hospitalId") var hospitalId: String = "",
    @get:PropertyName("hospitalName") var hospitalName: String = "",
    @get:PropertyName("hospitalAddress") var hospitalAddress: String = "",
    @get:PropertyName("slotId") var slotId: String = "",
    @get:PropertyName("date") var date: String = "", // Date as string (Firebase compatibility)
    @get:PropertyName("appointmentDate") var appointmentDate: String = "", // Formatted date string
    @get:PropertyName("appointmentTime") var appointmentTime: String = "",
    @get:PropertyName("status") var status: String = STATUS_PENDING, // pending, confirmed, completed, cancelled
    @get:PropertyName("createdAt") var createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("updatedAt") var updatedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("notes") var notes: String = "",
    @get:PropertyName("startTime") var startTime: String = "",
    @get:PropertyName("endTime") var endTime: String = ""
) {
    @get:Exclude
    val appointmentStatus: AppointmentStatus
        get() = AppointmentStatus.fromString(status)

    // For Firestore serialization
    @get:PropertyName("appointmentStatus")
    var statusEnum: String
        get() = status
        set(value) {
            status = value
        }

    // Helper function to create a copy with updated status
    fun withStatus(newStatus: String): Appointment {
        return this.copy(status = newStatus)
    }

    companion object {
        // Status constants for type safety
        const val STATUS_PENDING = "pending"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"
    }
}

