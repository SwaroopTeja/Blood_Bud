package com.example.blood_bud.ui.hospital

import androidx.recyclerview.widget.DiffUtil
import com.example.blood_bud.data.model.Appointment

class AppointmentDiffCallback : DiffUtil.ItemCallback<Appointment>() {
    override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
        return oldItem.appointmentId == newItem.appointmentId
    }

    override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: Appointment, newItem: Appointment): Any? {
        return if (oldItem.status != newItem.status) "status" else null
    }
}
