package com.example.blood_bud.ui.donor

// Kotlin data class for hospital information
data class Hospital(
   val id: String = "",
   val name: String = "",
   val state: String = "",
   val district: String = "",
   val addressSnippet: String = "",
   val availableSlotsCount: Int = 0
) {
    val fullAddress: String
        get() = buildString {
            if (addressSnippet.isNotBlank()) {
                append(addressSnippet)
            } else {
                if (district.isNotBlank()) append(district)
                if (state.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(state)
                }
                if (isEmpty()) append("Address not available")
            }
        }
}
