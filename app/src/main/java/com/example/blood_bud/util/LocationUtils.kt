package com.example.blood_bud.util

import android.location.Location
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.*

object LocationUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculate the distance between two points in kilometers using Haversine formula
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (EARTH_RADIUS_KM * c * 10).roundToInt() / 10.0 // Round to 1 decimal place
    }

    /**
     * Format distance for display (e.g., "2.3 km" or "< 1 km")
     */
    fun formatDistance(km: Double): String {
        return if (km < 1) "< 1 km" else "${km} km"
    }
}

object DateUtils {
    /**
     * Parse a date string in yyyy-MM-dd format to LocalDate
     */
    fun parseDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse a timestamp that could be either epoch millis or ISO date string
     */
    fun parseTimestamp(timestamp: Any?): LocalDate? {
        return when (timestamp) {
            is Long -> LocalDate.ofEpochDay(timestamp / 86400000)
            is String -> parseDate(timestamp)
            else -> null
        }
    }

    /**
     * Check if the user is eligible to donate based on last donation date
     * @param lastDonationDate The last donation date as a timestamp (millis or ISO string)
     * @return Pair of (isEligible, daysRemaining)
     */
    fun isEligibleToDonate(lastDonationDate: Any?): Pair<Boolean, Int> {
        if (lastDonationDate == null) return true to 0

        val lastDonation = parseTimestamp(lastDonationDate) ?: return true to 0
        val today = LocalDate.now()
        val daysSinceDonation = ChronoUnit.DAYS.between(lastDonation, today)
        val daysRemaining = (52 - daysSinceDonation).coerceAtLeast(0)
        
        return (daysSinceDonation >= 52) to daysRemaining.toInt()
    }

    /**
     * Format date to display in UI (e.g., "Oct 15, 2023")
     */
    fun formatDisplayDate(date: LocalDate): String {
        return date.atStartOfDay(ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }

    /**
     * Format time from epoch millis to HH:mm format
     */
    fun formatTime(timeMillis: Long): String {
        return java.time.Instant.ofEpochMilli(timeMillis)
            .atZone(ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
    }

    /**
     * Get today's date in yyyy-MM-dd format
     */
    fun getTodayDate(): String {
        return LocalDate.now().toString()
    }
}
