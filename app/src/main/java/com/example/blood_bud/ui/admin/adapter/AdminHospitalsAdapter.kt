package com.example.blood_bud.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Hospital
import com.example.blood_bud.databinding.ItemAdminHospitalBinding

class AdminHospitalsAdapter(
    private val onDeleteClick: (Hospital) -> Unit
) : ListAdapter<Hospital, AdminHospitalsAdapter.AdminHospitalViewHolder>(AdminHospitalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminHospitalViewHolder {
        val binding = ItemAdminHospitalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdminHospitalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminHospitalViewHolder, position: Int) {
        val hospital = getItem(position)
        holder.bind(hospital)
    }

    inner class AdminHospitalViewHolder(
        private val binding: ItemAdminHospitalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hospital: Hospital) {
            binding.apply {
                tvHospitalName.text = hospital.name
                tvHospitalAddress.text = hospital.fullAddress
                
                // Set status chip
                chipStatus.text = hospital.status.replaceFirstChar { it.uppercase() }
                when (hospital.status.lowercase()) {
                    "approved" -> {
                        chipStatus.setChipBackgroundColorResource(R.color.green_light)
                        chipStatus.setTextColor(root.context.getColor(android.R.color.black))
                    }
                    "rejected" -> {
                        chipStatus.setChipBackgroundColorResource(R.color.red_light)
                        chipStatus.setTextColor(root.context.getColor(android.R.color.black))
                    }
                    else -> {
                        chipStatus.setChipBackgroundColorResource(R.color.colorPrimaryLight)
                        chipStatus.setTextColor(root.context.getColor(android.R.color.black))
                    }
                }

                // Set delete button click listener
                btnDelete.setOnClickListener { onDeleteClick(hospital) }
            }
        }
    }

    private class AdminHospitalDiffCallback : DiffUtil.ItemCallback<Hospital>() {
        override fun areItemsTheSame(oldItem: Hospital, newItem: Hospital): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Hospital, newItem: Hospital): Boolean {
            return oldItem == newItem
        }
    }
}
