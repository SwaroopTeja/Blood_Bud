package com.example.blood_bud.utils

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.widget.Toast

// Extension function to show a toast message
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

// Extension function to show a progress dialog
fun Context.showProgress(message: String = "Loading..."): AlertDialog {
    return AlertDialog.Builder(this)
        .setMessage(message)
        .setCancelable(false)
        .create()
        .apply { show() }
}

// Extension function to hide a progress dialog
fun AlertDialog?.hideProgress() {
    this?.dismiss()
}
