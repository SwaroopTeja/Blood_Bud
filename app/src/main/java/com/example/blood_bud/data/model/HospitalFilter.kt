package com.example.blood_bud.data.model

data class HospitalFilter(
    val states: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val bloodType: String? = null,
    val distance: Double? = null,
    val rating: Double? = null,
    val services: List<String> = emptyList()
)
