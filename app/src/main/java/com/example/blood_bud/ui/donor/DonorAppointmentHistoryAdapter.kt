package com.example.blood_bud.ui.donor

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Appointment
import com.example.blood_bud.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DonorAppointmentHistoryAdapter(
    private val context: Context,
    private val firestore: FirebaseFirestore
) : RecyclerView.Adapter<DonorAppointmentHistoryAdapter.HistoryViewHolder>() {

    private var appointments = listOf<Appointment>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun submitList(list: List<Appointment>) {
        appointments = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount() = appointments.size

    inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHospitalName: TextView = view.findViewById(R.id.tvHospitalName)
        private val tvAppointmentDate: TextView = view.findViewById(R.id.tvAppointmentDate)
        private val tvAppointmentTime: TextView = view.findViewById(R.id.tvAppointmentTime)
        private val tvStatus: TextView = view.findViewById(R.id.tvStatus)

        fun bind(appointment: Appointment) {
            // Load hospital name from Firestore if available
            if (!appointment.hospitalId.isNullOrBlank()) {
                loadHospitalName(appointment.hospitalId, tvHospitalName)
            } else {
                tvHospitalName.text = appointment.hospitalName.ifBlank { "Unknown Hospital" }
            }

            tvAppointmentDate.text = appointment.appointmentDate
            tvAppointmentTime.text = appointment.appointmentTime

            // Set status with color
            val status = appointment.status.lowercase()
            tvStatus.text = status.replaceFirstChar { it.uppercase() }
            
            val (bgColor, textColor) = when (status) {
                "completed" -> Pair(R.color.green_500, R.color.green_dark)
                "cancelled" -> Pair(R.color.red_100, R.color.red_dark)
                else -> Pair(R.color.gray_200, R.color.gray_600)
            }
            
            tvStatus.setBackgroundResource(bgColor)
            tvStatus.setTextColor(ContextCompat.getColor(context, textColor))
        }

        private fun loadHospitalName(hospitalId: String, textView: TextView) {
            firestore.collection("hospitals").document(hospitalId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") 
                            ?: document.getString("hospitalName")
                            ?: "Unknown Hospital"
                        textView.text = name
                    } else {
                        textView.text = "Unknown Hospital"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HistoryAdapter", "Error loading hospital name", e)
                    textView.text = "Unknown Hospital"
                }
        }
    }
}
