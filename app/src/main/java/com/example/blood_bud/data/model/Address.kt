package com.example.blood_bud.data.model

data class Address(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = "India",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    // For backward compatibility
    val pincode: String
        get() = postalCode
}