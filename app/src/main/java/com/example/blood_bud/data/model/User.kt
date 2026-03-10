package com.example.blood_bud.data.model

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val userType: UserType = UserType.DONOR,
    val address: Address = Address(),
    val isActive: Boolean = true,
    val isAdmin: Boolean = false,
    val isSuperAdmin: Boolean = false,
    @Exclude
    val isSystemUser: Boolean = false,
    val fcmToken: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val bloodType: String? = null,
    val bloodGroup: String? = null, // Added blood group field
    val location: String? = null,
    val lastDonation: String? = null,
    val photoUrl: String? = null
) {
    @Exclude
    fun toMap(): Map<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to id,
            "email" to email,
            "name" to name,
            "phone" to phone,
            "userType" to userType.name,
            "isActive" to isActive,
            "bloodType" to (bloodType ?: ""),
            "bloodGroup" to (bloodGroup ?: ""),
            "location" to (location ?: ""),
            "lastDonation" to (lastDonation ?: ""),
            "photoUrl" to (photoUrl ?: ""),
            "isAdmin" to isAdmin,
            "isSuperAdmin" to isSuperAdmin,
            "isSystemUser" to isSystemUser,
            "createdAt" to createdAt
        )
        
        // Add address fields individually instead of using toMap()
        map["address"] = hashMapOf<String, Any>(
            "street" to address.street,
            "city" to address.city,
            "state" to address.state,
            "postalCode" to address.postalCode,
            "country" to address.country,
            "latitude" to address.latitude,
            "longitude" to address.longitude
        )
        
        // Add FCM token if present
        fcmToken?.let { map["fcmToken"] = it }
        
        return map
    }

    // Add a no-argument constructor for Firestore
    constructor() : this(
        id = "",
        email = "",
        name = "",
        phone = "",
        userType = UserType.DONOR,
        address = Address(),
        isActive = true,
        isSystemUser = false,
        fcmToken = null,
        createdAt = com.google.firebase.Timestamp.now()
    )
}