package com.example.blood_bud.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Hospital
import com.example.blood_bud.databinding.ItemHospitalBinding

interface HospitalActionListener {
    fun onApproveHospital(hospital: Hospital)
    fun onRejectHospital(hospital: Hospital)
    fun onDeleteHospital(hospital: Hospital)
    fun onHospitalClick(hospital: Hospital)
}

class HospitalsAdapter(
    private val actionListener: HospitalActionListener
) : ListAdapter<Hospital, HospitalsAdapter.HospitalViewHolder>(HospitalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val binding = ItemHospitalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HospitalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HospitalViewHolder(
        private val binding: ItemHospitalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hospital: Hospital) {
            binding.apply {
                hospitalName.text = hospital.name.ifBlank { "Unknown Hospital" }
                
                // Set address fields
                val address = hospital.address
                tvAddressLine1.text = address["street"]?.toString() ?: ""
                tvCity.text = address["city"]?.toString() ?: ""
                tvState.text = address["state"]?.toString() ?: ""
                tvPostalCode.text = address["postalCode"]?.toString() ?: ""
                
                // Set status with color coding
                val status = hospital.status?.lowercase() ?: "pending"
                hospitalStatus.text = status.replaceFirstChar { it.uppercase() }
                
                // Set status background color
                val (bgColorRes, textColorRes) = when (status) {
                    "approved" -> R.drawable.bg_status_approved to R.color.green_700
                    "rejected" -> R.drawable.bg_status_rejected to R.color.red_700
                    else -> R.drawable.bg_status_pending to R.color.orange_700
                }
                
                hospitalStatus.setBackgroundResource(bgColorRes)
                hospitalStatus.setTextColor(ContextCompat.getColor(itemView.context, textColorRes))
                
                // Show/hide approval/delete buttons based on status
                if (status == "pending") {
                    approvalContainer.visibility = View.VISIBLE
                    deleteContainer.visibility = View.GONE
                    divider1.visibility = View.VISIBLE
                    divider2.visibility = View.GONE
                    
                    btnApprove.setOnClickListener {
                        showApproveDialog(hospital)
                    }
                    
                    btnReject.setOnClickListener {
                        showRejectDialog(hospital)
                    }
                } else {
                    approvalContainer.visibility = View.GONE
                    // Show delete button for approved/rejected hospitals
                    deleteContainer.visibility = View.VISIBLE
                    divider1.visibility = View.GONE
                    divider2.visibility = View.VISIBLE
                    
                    btnDelete.setOnClickListener {
                        showDeleteDialog(hospital)
                    }
                }
                
                root.setOnClickListener {
                    actionListener.onHospitalClick(hospital)
                }
            }
        }
    }

    class HospitalDiffCallback : DiffUtil.ItemCallback<Hospital>() {
        override fun areItemsTheSame(oldItem: Hospital, newItem: Hospital): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Hospital, newItem: Hospital): Boolean {
            return oldItem == newItem
        }
    }
    
    private fun HospitalViewHolder.showApproveDialog(hospital: Hospital) {
        AlertDialog.Builder(itemView.context)
            .setTitle(R.string.approve_hospital)
            .setMessage(R.string.approve_hospital_message)
            .setPositiveButton(R.string.approve) { _, _ ->
                actionListener.onApproveHospital(hospital)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun HospitalViewHolder.showRejectDialog(hospital: Hospital) {
        val dialog = AlertDialog.Builder(itemView.context)
            .setTitle(R.string.reject_hospital)
            .setMessage(R.string.reject_hospital_message)
            .setPositiveButton(R.string.reject) { _, _ ->
                actionListener.onRejectHospital(hospital)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()
    }
    
    private fun HospitalViewHolder.showDeleteDialog(hospital: Hospital) {
        val dialog = AlertDialog.Builder(itemView.context)
            .setTitle(R.string.delete_hospital)
            .setMessage(R.string.delete_hospital_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                actionListener.onDeleteHospital(hospital)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()
    }
}
