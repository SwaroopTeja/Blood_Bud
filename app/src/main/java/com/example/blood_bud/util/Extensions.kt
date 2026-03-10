package com.example.blood_bud.util

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import android.util.TypedValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

// Show toast from Fragment
fun Fragment.showToast(message: String) {
    context?.let {
        Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
    }
}

fun Fragment.showToast(@StringRes messageRes: Int) {
    context?.let {
        Toast.makeText(it, messageRes, Toast.LENGTH_SHORT).show()
    }
}

// Show toast from View
fun View.showToast(message: String) {
    context?.let {
        Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
    }
}

// Extension function to observe LiveData
fun <T> LiveData<T>.observe(lifecycleOwner: LifecycleOwner, onChanged: (T) -> Unit) {
    this.observe(lifecycleOwner, Observer { it?.let(onChanged) })
}

// Extension function to convert dp to pixels
fun Int.dpToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}
