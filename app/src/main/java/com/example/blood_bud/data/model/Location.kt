package com.example.blood_bud.data.model

/**
 * Data class representing a location with city and state information.
 * Used for filtering hospitals by location.
 */
data class Location(
    val city: String = "",
    val state: String = ""
) {
    fun isEmpty(): Boolean = city.isBlank() && state.isBlank()
    
    companion object {
        val EMPTY = Location()
        
        /**
         * Parse a full address string into a Location object.
         * Assumes format: "City, State"
         */
        fun fromAddress(address: String?): Location {
            if (address.isNullOrBlank()) return EMPTY
            
            val parts = address.split(",").map { it.trim() }
            return when (parts.size) {
                2 -> Location(city = parts[0], state = parts[1])
                1 -> Location(city = parts[0])
                else -> EMPTY
            }
        }
    }
}
