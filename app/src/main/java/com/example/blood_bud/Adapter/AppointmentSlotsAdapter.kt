package com.example.blood_bud.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.R
import com.example.blood_bud.data.model.AppointmentSlot
import com.google.android.material.button.MaterialButton

class AppointmentSlotsAdapter(
    private val onSlotClick: (AppointmentSlot) -> Unit,
    private val onSlotBooked: (AppointmentSlot) -> Unit
) : ListAdapter<AppointmentSlot, AppointmentSlotsAdapter.SlotViewHolder>(SlotDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_slot, parent, false)
        return SlotViewHolder(view, onSlotClick, onSlotBooked)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = getItem(position)
        holder.bind(slot)
    }

    class SlotViewHolder(
        itemView: View,
        private val onSlotClick: (AppointmentSlot) -> Unit,
        private val onSlotBooked: (AppointmentSlot) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvTimeRange: TextView = itemView.findViewById(R.id.tvTimeRange)
        private val tvCapacity: TextView = itemView.findViewById(R.id.tvCapacity)
        private val tvAvailability: TextView = itemView.findViewById(R.id.tvAvailability)
        private val btnBookSlot: MaterialButton = itemView.findViewById(R.id.btnBookSlot)

        fun bind(slot: AppointmentSlot) {
            tvTimeRange.text = slot.getTimeRange()
            tvCapacity.text = "Capacity: ${slot.capacity}"

            val availableSpots = slot.capacity - slot.currentBookings
            tvAvailability.text = "$availableSpots spots available"

            // Update button state based on availability
            if (slot.isAvailable()) {
                btnBookSlot.isEnabled = true
                btnBookSlot.text = "Book Slot"
                btnBookSlot.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.primary)
                )
            } else {
                btnBookSlot.isEnabled = false
                btnBookSlot.text = "Full"
                btnBookSlot.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
                )
            }

            btnBookSlot.setOnClickListener {
                if (slot.isAvailable()) {
                    onSlotClick(slot)
                }
            }

            // Set item click listener for the entire card
            itemView.setOnClickListener {
                if (slot.isAvailable()) {
                    onSlotClick(slot)
                }
            }
        }
    }

    class SlotDiffCallback : DiffUtil.ItemCallback<AppointmentSlot>() {
        override fun areItemsTheSame(oldItem: AppointmentSlot, newItem: AppointmentSlot): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppointmentSlot, newItem: AppointmentSlot): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: AppointmentSlot, newItem: AppointmentSlot): Any? {
            return if (oldItem.currentBookings != newItem.currentBookings) {
                "bookings_changed"
            } else {
                super.getChangePayload(oldItem, newItem)
            }
        }
    }
}
