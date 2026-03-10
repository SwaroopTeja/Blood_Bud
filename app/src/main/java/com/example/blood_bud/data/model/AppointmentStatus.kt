package com.example.blood_bud.data.model

enum class AppointmentStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromString(status: String): AppointmentStatus {
            return when (status.lowercase()) {
                "confirmed" -> CONFIRMED
                "completed" -> COMPLETED
                "cancelled" -> CANCELLED
                else -> PENDING
            }
        }
    }
}
