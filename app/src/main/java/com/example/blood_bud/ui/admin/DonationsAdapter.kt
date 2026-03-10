package com.example.blood_bud.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ItemDonationBinding

class DonationsAdapter : ListAdapter<Donation, DonationsAdapter.DonationViewHolder>(DonationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
        val binding = ItemDonationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DonationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DonationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DonationViewHolder(private val binding: ItemDonationBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(donation: Donation) {
            binding.donorName.text = donation.donorName
            binding.donationDate.text = donation.date
            binding.donationLocation.text = donation.hospitalName
            
            // Set status badge (using bloodType TextView as status indicator)
            binding.bloodType.text = donation.status.uppercase()
            binding.bloodType.setBackgroundResource(
                when (donation.status) {
                    "completed" -> R.drawable.bg_status_completed
                    "pending" -> R.drawable.bg_status_pending
                    "cancelled" -> R.drawable.bg_status_cancelled
                    else -> R.drawable.bg_blood_type
                }
            )
        }
    }

    class DonationDiffCallback : DiffUtil.ItemCallback<Donation>() {
        override fun areItemsTheSame(oldItem: Donation, newItem: Donation): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: Donation, newItem: Donation): Boolean {
            return oldItem == newItem
        }
    }
}
