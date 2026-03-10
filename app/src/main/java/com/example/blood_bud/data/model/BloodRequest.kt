package com.example.blood_bud.data.model

import com.google.firebase.Timestamp

data class BloodRequest(
    val id: String = "",
    val requesterId: String = "", // User ID who made the request
    val hospitalId: String = "",
    val bloodType: String = "",
    val units: Int = 0,
    val status: String = "pending", // "pending", "approved", "rejected", "completed"
    val requestedAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val adminNotes: String = "",
    val hospitalNotes: String = ""
) {
    // Add a no-argument constructor for Firestore
    constructor() : this("", "", "", "", 0, "pending", Timestamp.now(), Timestamp.now(), "", "")
}