package com.example.blood_bud.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DonorHistory(
    val id: String = "",
    val donorId: String = "",
    val donorName: String = "",
    val donorEmail: String = "",
    val donationDate: Long = 0, // epoch millis
    val hospitalId: String = "",
    val hospitalName: String = "",
    val bloodType: String = "",
    val unitsDonated: Int = 1,
    val slotId: String = "",
    val status: DonationStatus = DonationStatus.PENDING,
    val certificateUrl: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {

    // Helper function to check if donor can book new slot (56+ days since last donation)
    fun canBookNewSlot(): Boolean {
        val daysSinceLastDonation = (System.currentTimeMillis() - donationDate) / (1000 * 60 * 60 * 24)
        return daysSinceLastDonation >= 56 // 56 days minimum gap between donations
    }

    // Helper function to get next eligible donation date
    fun getNextEligibleDate(): Long {
        return donationDate + (56 * 24 * 60 * 60 * 1000L) // 56 days in milliseconds
    }
}

enum class DonationStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}

@Parcelize
data class SlotCreationRequest(
    val hospitalId: String = "",
    val scheduleType: ScheduleType = ScheduleType.SINGLE_DAY,
    val startDate: String = "", // yyyy-MM-dd format
    val endDate: String? = null, // for weekly schedule
    val startTime: String = "", // HH:mm format
    val endTime: String = "",   // HH:mm format
    val slotDuration: Int = 30, // minutes
    val capacity: Int = 10,     // donors per slot
    val breakStart: String? = null, // HH:mm format
    val breakEnd: String? = null,   // HH:mm format
    val excludeWeekends: Boolean = false,
    val workingDays: List<Int> = listOf(1, 2, 3, 4, 5, 6) // Monday=1, Sunday=7
) : Parcelable

enum class ScheduleType {
    SINGLE_DAY,
    WEEKLY
}
