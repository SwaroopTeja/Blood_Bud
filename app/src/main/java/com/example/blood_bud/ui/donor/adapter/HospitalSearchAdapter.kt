package com.example.blood_bud.ui.donor.adapter

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Hospital

class HospitalSearchAdapter(
    private val onItemClick: (Hospital) -> Unit
) : ListAdapter<Hospital, HospitalSearchAdapter.HospitalViewHolder>(DiffCallback()) {

    // Inner class for the DiffCallback
    private class DiffCallback : DiffUtil.ItemCallback<Hospital>() {
        override fun areItemsTheSame(oldItem: Hospital, newItem: Hospital): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Hospital, newItem: Hospital): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Hospital, newItem: Hospital): Any? {
            return if (oldItem.bloodInventory != newItem.bloodInventory) {
                "blood_inventory_changed"
            } else {
                super.getChangePayload(oldItem, newItem)
            }
        }
    }

    private var selectedBloodType: String = ""

    fun setSelectedBloodType(bloodType: String) {
        selectedBloodType = bloodType
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hospital_donor_search, parent, false)
        return HospitalViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {
        val hospital = getItem(position)
        holder.bind(hospital, selectedBloodType)
    }

    inner class HospitalViewHolder(
        itemView: View,
        private val onItemClick: (Hospital) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvHospitalName: TextView = itemView.findViewById(R.id.tvHospitalName)
        private val tvHospitalAddress: TextView = itemView.findViewById(R.id.tvHospitalAddress)
        private val tvBloodStock: TextView = itemView.findViewById(R.id.tvBloodStock)
        private val tvHospitalPhone: TextView = itemView.findViewById(R.id.tvHospitalPhone)
        private val tvStockStatus: View = itemView.findViewById(R.id.viewStockStatus)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(hospital: Hospital, selectedBloodType: String) {
            tvHospitalName.text = hospital.name

            // Use the existing fullAddress property which handles address formatting properly
            tvHospitalAddress.text = hospital.fullAddress

            tvHospitalPhone.text = hospital.phone.takeIf { it.isNotBlank() } ?: "N/A"

            if (selectedBloodType.isNotEmpty()) {
                val stock = hospital.bloodInventory[selectedBloodType] ?: 0
                val isAvailable = stock > 0

                tvBloodStock.text = if (isAvailable) "Available" else "Unavailable"

                // Update stock status indicator
                val statusColor = if (isAvailable) {
                    android.R.color.holo_green_light
                } else {
                    android.R.color.holo_red_light
                }
                try {
                    tvStockStatus.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, statusColor)
                    )
                    tvStockStatus.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Log.e("HospitalSearchAdapter", "Error setting status color", e)
                    tvStockStatus.visibility = View.INVISIBLE
                }
            } else {
                tvBloodStock.text = ""
                tvStockStatus.visibility = View.INVISIBLE
            }
        }
    }
}
