package com.example.blood_bud.ui.donor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.databinding.ItemHospitalBinding

class HospitalAdapter(
    private val hospitals: List<Hospital>,
    private val onHospitalClick: (Hospital) -> Unit
) : RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder>() {

    class HospitalViewHolder(val binding: ItemHospitalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val binding = ItemHospitalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HospitalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {
        val hospital = hospitals[position]
        holder.binding.apply {
            hospitalName.text = hospital.name.ifEmpty { "Unknown Hospital" }
            
            // Set address fields
            tvAddressLine1.text = hospital.addressSnippet.ifEmpty { "" }
            tvCity.text = hospital.district.ifEmpty { "" }
            tvState.text = hospital.state.ifEmpty { "" }
            tvPostalCode.text = ""  // Postal code not available in the model
            
            hospitalStatus.text = "${hospital.availableSlotsCount} slots available"

            root.setOnClickListener { onHospitalClick(hospital) }
        }
    }

    override fun getItemCount() = hospitals.size
}
