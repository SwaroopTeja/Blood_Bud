package com.example.blood_bud.ui.hospital

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Appointment
import com.example.blood_bud.data.model.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HospitalAppointmentAdapter(
    private val context: Context,
    private val onStatusUpdate: (Appointment, String) -> Unit
) : ListAdapter<Appointment, HospitalAppointmentAdapter.AppointmentViewHolder>(AppointmentDiffCallback()) {

    companion object {
        private const val STATUS_PENDING = "pending"
        private const val STATUS_CONFIRMED = "confirmed"
        private const val STATUS_COMPLETED = "completed"
        private const val STATUS_CANCELLED = "cancelled"
    }

    private val database = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private fun setStatusStyle(textView: TextView, status: String) {
        val bgResId = when (status.lowercase()) {
            STATUS_PENDING -> R.drawable.bg_status_pending
            STATUS_CONFIRMED -> R.drawable.bg_status_confirmed
            STATUS_COMPLETED -> R.drawable.bg_status_completed
            STATUS_CANCELLED -> R.drawable.bg_status_cancelled
            else -> R.drawable.bg_status_pending
        }
        val textColorResId = when (status.lowercase()) {
            STATUS_COMPLETED, STATUS_CANCELLED -> R.color.white
            else -> R.color.black
        }
        textView.setBackgroundResource(bgResId)
        textView.setTextColor(ContextCompat.getColor(context, textColorResId))
    }

    private fun loadDonorName(donorId: String, textView: TextView) {
        if (donorId.isBlank()) {
            Log.e("AppointmentAdapter", "❌ Donor ID is blank")
            textView.text = context.getString(R.string.unknown_donor)
            return
        }

        Log.d("AppointmentAdapter", "🔍 Querying Firestore: users/$donorId")
        
        firestore.collection("users").document(donorId)
            .get()
            .addOnSuccessListener { document ->
                Log.d("AppointmentAdapter", "📊 Firestore response - exists: ${document.exists()}")
                
                if (document.exists()) {
                    Log.d("AppointmentAdapter", "📊 Firestore data: ${document.data}")
                    
                    // Try multiple ways to get the name
                    var name: String? = null
                    
                    try {
                        // First try direct field access
                        name = document.getString("name")
                        Log.d("AppointmentAdapter", "👤 Direct name field: $name")
                        
                        if (name.isNullOrBlank()) {
                            // Try User object deserialization
                            val user = document.toObject(User::class.java)
                            name = user?.name
                            Log.d("AppointmentAdapter", "👤 User object name: $name")
                        }
                        
                        if (name.isNullOrBlank()) {
                            // Try alternative field names
                            name = document.getString("fullName")
                                ?: document.getString("userName")
                                ?: document.getString("displayName")
                            Log.d("AppointmentAdapter", "👤 Alternative name field: $name")
                        }
                    } catch (e: Exception) {
                        Log.e("AppointmentAdapter", "❌ Error parsing user data: ${e.message}", e)
                    }
                    
                    Log.d("AppointmentAdapter", "👤 Final name: $name")
                    textView.text = if (!name.isNullOrBlank()) {
                        name
                    } else {
                        Log.w("AppointmentAdapter", "⚠️ Name is null/blank, using unknown donor")
                        context.getString(R.string.unknown_donor)
                    }
                } else {
                    Log.e("AppointmentAdapter", "❌ User document not found in Firestore: $donorId")
                    textView.text = context.getString(R.string.unknown_donor)
                }
            }
            .addOnFailureListener { e ->
                Log.e("AppointmentAdapter", "❌ Firestore error: ${e.message}", e)
                Log.e("AppointmentAdapter", "❌ Full error: ", e)
                textView.text = context.getString(R.string.unknown_donor)
            }
    }

    private fun loadDonorName(donorId: String, onNameLoaded: (String) -> Unit) {
        if (donorId.isBlank()) {
            onNameLoaded(context.getString(R.string.unknown_donor))
            return
        }

        firestore.collection("users").document(donorId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    var name: String? = null
                    
                    try {
                        // First try direct field access
                        name = document.getString("name")
                        
                        if (name.isNullOrBlank()) {
                            // Try User object deserialization
                            val user = document.toObject(User::class.java)
                            name = user?.name
                        }
                        
                        if (name.isNullOrBlank()) {
                            // Try alternative field names
                            name = document.getString("fullName")
                                ?: document.getString("userName")
                                ?: document.getString("displayName")
                        }
                    } catch (e: Exception) {
                        Log.e("AppointmentAdapter", "Error parsing user data: ${e.message}", e)
                    }
                    
                    onNameLoaded(if (!name.isNullOrBlank()) name else context.getString(R.string.unknown_donor))
                } else {
                    Log.e("AppointmentAdapter", "User document not found: $donorId")
                    onNameLoaded(context.getString(R.string.unknown_donor))
                }
            }
            .addOnFailureListener { e ->
                Log.e("AppointmentAdapter", "Error loading donor name from Firestore", e)
                onNameLoaded(context.getString(R.string.unknown_donor))
            }
    }

    inner class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDonorName: TextView = view.findViewById(R.id.tvDonorName)
        private val tvAppointmentStatus: TextView = view.findViewById(R.id.tvAppointmentStatus)
        private val tvAppointmentDate: TextView = view.findViewById(R.id.tvAppointmentDate)
        private val tvAppointmentTime: TextView = view.findViewById(R.id.tvAppointmentTime)
        private val tvNotes: TextView = view.findViewById(R.id.tvNotes)
        private val tvCreatedAt: TextView = view.findViewById(R.id.tvCreatedAt)
        private val btnComplete: Button = view.findViewById(R.id.btnComplete)
        private val btnCancel: Button = view.findViewById(R.id.btnCancel)
        private val actionButtons: ViewGroup = view.findViewById(R.id.actionButtons)

        private fun formatTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours ${if (hours == 1L) "hour" else "hours"} ago"
                }
                else -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "$days ${if (days == 1L) "day" else "days"} ago"
                }
            }
        }

        private fun showConfirmationDialog(message: String, onConfirm: () -> Unit) {
            AlertDialog.Builder(context)
                .setTitle("Confirm")
                .setMessage(message)
                .setPositiveButton("Yes") { _, _ -> onConfirm() }
                .setNegativeButton("No", null)
                .show()
        }

        private fun showAppointmentDetails(appointment: Appointment) {
            val dialog = android.app.Dialog(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
            dialog.setContentView(R.layout.dialog_appointment_details)

            // Set appointment details
            dialog.findViewById<TextView>(R.id.tvDonorName).text = "Loading..."
            dialog.findViewById<TextView>(R.id.tvStatus).text = appointment.status?.capitalize() ?: ""
            dialog.findViewById<TextView>(R.id.tvDate).text = appointment.appointmentDate ?: ""
            dialog.findViewById<TextView>(R.id.tvTime).text = appointment.appointmentTime ?: ""
            dialog.findViewById<TextView>(R.id.tvNotesContent).text = appointment.notes ?: "No notes"

            // Format and set created at time
            try {
                val createdAt = appointment.createdAt as? Long ?: 0L
                val date = Date(createdAt)
                val formattedDate = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(date)
                dialog.findViewById<TextView>(R.id.tvCreatedAt).text = formattedDate
            } catch (e: Exception) {
                dialog.findViewById<TextView>(R.id.tvCreatedAt).text = "N/A"
            }

            // Load donor name
            loadDonorName(appointment.donorId ?: "") { name ->
                dialog.findViewById<TextView>(R.id.tvDonorName).text = name
            }

            // Set status background
            val statusView = dialog.findViewById<TextView>(R.id.tvStatus)
            setStatusStyle(statusView, appointment.status ?: "pending")

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.show()
        }

        fun bind(appointment: Appointment) {
            // Debug log appointment data
            Log.d("AppointmentAdapter", "=== Binding Appointment ===")
            Log.d("AppointmentAdapter", "Appointment ID: ${appointment.appointmentId}")
            Log.d("AppointmentAdapter", "Donor ID: ${appointment.donorId}")
            Log.d("AppointmentAdapter", "Donor Name field: '${appointment.donorName}'")
            
            // Use donor name from appointment if available, otherwise load from Firestore
            if (!appointment.donorName.isNullOrBlank()) {
                Log.d("AppointmentAdapter", "✅ Using donor name from appointment: ${appointment.donorName}")
                tvDonorName.text = appointment.donorName
            } else if (!appointment.donorId.isNullOrBlank()) {
                Log.d("AppointmentAdapter", "📡 Donor name not in appointment, loading from Firestore for donorId: ${appointment.donorId}")
                tvDonorName.text = "Loading..."
                loadDonorName(appointment.donorId ?: "", tvDonorName)
            } else {
                Log.e("AppointmentAdapter", "❌ No donor ID or name available")
                tvDonorName.text = context.getString(R.string.unknown_donor)
            }

            // Set status with appropriate background and text color
            val status = appointment.status?.lowercase() ?: STATUS_PENDING
            tvAppointmentStatus.text = status.capitalize()
            setStatusStyle(tvAppointmentStatus, status)

            // Format and set date and time
            try {
                val date = dateFormat.parse(appointment.appointmentDate ?: "")
                tvAppointmentDate.text = date?.let { dateFormat.format(it) } ?: appointment.appointmentDate
                
                val time = timeFormat.parse(appointment.appointmentTime ?: "")
                tvAppointmentTime.text = time?.let { timeFormat.format(it) } ?: appointment.appointmentTime
                
                // Set notes if available
                if (!appointment.notes.isNullOrBlank()) {
                    tvNotes.text = appointment.notes
                    tvNotes.visibility = View.VISIBLE
                } else {
                    tvNotes.visibility = View.GONE
                }
                
                // Set relative time for created at
                appointment.createdAt?.let { timestamp ->
                    val relativeTime = formatTimeAgo(timestamp)
                    tvCreatedAt.text = context.getString(R.string.created_s_ago, relativeTime)
                } ?: run {
                    tvCreatedAt.visibility = View.GONE
                }
            } catch (e: Exception) {
                tvAppointmentDate.text = appointment.appointmentDate
                tvAppointmentTime.text = appointment.appointmentTime
                // Log.e("AppointmentAdapter", "Error formatting date/time", e)
            }
            
            // Set up action buttons based on status
            when (status) {
                STATUS_PENDING, STATUS_CONFIRMED -> {
                    btnComplete.text = context.getString(R.string.donation_completed)
                    btnComplete.visibility = View.VISIBLE
                    btnCancel.text = context.getString(R.string.donation_cancelled)
                    btnCancel.visibility = View.VISIBLE
                    actionButtons.visibility = View.VISIBLE
                }
                else -> {
                    // Hide buttons for completed or cancelled appointments
                    actionButtons.visibility = View.GONE
                }
            }
            
            btnComplete.setOnClickListener {
                btnComplete.isEnabled = false
                btnCancel.isEnabled = false
                showConfirmationDialog("Mark this donation as completed?") {
                    onStatusUpdate(appointment, STATUS_COMPLETED)
                }
            }
            
            btnCancel.setOnClickListener {
                btnComplete.isEnabled = false
                btnCancel.isEnabled = false
                showConfirmationDialog("Cancel this donation appointment?") {
                    onStatusUpdate(appointment, STATUS_CANCELLED)
                }
            }
            
            // Make the whole item clickable for viewing details
            itemView.setOnClickListener {
                // Show appointment details dialog
                showAppointmentDetails(appointment)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    fun submitList(list: List<Appointment>, commitCallback: () -> Unit = {}) {
        super.submitList(ArrayList(list)) {
            commitCallback()
        }
    }
}
