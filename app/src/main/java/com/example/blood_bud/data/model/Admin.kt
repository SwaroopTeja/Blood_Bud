package com.example.blood_bud.data.model

import com.google.firebase.Timestamp

data class Admin(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val isSuperAdmin: Boolean = false,
    val createdBy: String = "", // ID of the super admin who created this admin
    val createdAt: Timestamp = Timestamp.now()
) {
    // Add a no-argument constructor for Firestore
    constructor() : this("", "", "", false, "", Timestamp.now())
}