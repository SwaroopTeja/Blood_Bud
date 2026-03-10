package com.example.blood_bud.data.model

import android.util.Log

enum class UserType {
    DONOR,
    HOSPITAL,
    ADMIN;

    companion object {
        fun fromString(value: String?): UserType {
            return try {
                value?.let {
                    valueOf(it.uppercase())
                } ?: DONOR
            } catch (e: Exception) {
                Log.e("UserType", "Invalid user type: $value", e)
                DONOR
            }
        }
    }
}
