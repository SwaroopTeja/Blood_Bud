package com.example.blood_bud.models

data class Admin(
    val id: String = "",
    val email: String = "",
    val password: String = "",
    val isActive: Boolean = true,
    val name: String = "",
    val isSuperAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Secondary constructor for Firestore deserialization
    constructor() : this("", "", "", true, "", false, 0)
    
    // Helper function to convert to map for Firestore
    fun toMap(): Map<String, Any> = mapOf(
        "email" to email.lowercase(),
        "isActive" to isActive,
        "name" to name,
        "isSuperAdmin" to isSuperAdmin,
        "createdAt" to com.google.firebase.Timestamp.now()
    )
}
