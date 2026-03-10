package com.example.blood_bud.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Hospital
import com.google.android.material.card.MaterialCardView

class SearchHospitalAdapter(
    private val onItemClick: (Hospital) -> Unit
) : ListAdapter<Hospital, SearchHospitalAdapter.HospitalViewHolder>(HospitalDiffCallback()) {

    private var selectedBloodType: String = ""

    fun setSelectedBloodType(bloodType: String) {
        selectedBloodType = bloodType
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_hospital, parent, false)
        return HospitalViewHolder(view)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {
        val hospital = getItem(position)
        holder.bind(hospital)
        holder.itemView.setOnClickListener { onItemClick(hospital) }
    }

    inner class HospitalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val hospitalName: TextView = itemView.findViewById(R.id.hospitalName)
        private val hospitalAddress: TextView = itemView.findViewById(R.id.hospitalAddress)
        private val bloodStock: TextView = itemView.findViewById(R.id.bloodStock)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.hospitalCard)
        private val stockAvailable: View = itemView.findViewById(R.id.stockAvailable)
        private val stockUnavailable: View = itemView.findViewById(R.id.stockUnavailable)

        fun bind(hospital: Hospital) {
            hospitalName.text = hospital.name

            // Fix address binding - format from map
            val address = hospital.address
            val formattedAddress = buildString {
                address?.get("street")?.let { append(it) }
                address?.get("city")?.let { if (isNotEmpty()) append(", ") ; append(it) }
                address?.get("state")?.let { if (isNotEmpty()) append(", ") ; append(it) }
                address?.get("postalCode")?.let { if (isNotEmpty()) append(" ") ; append(it) }
            }
            hospitalAddress.text = formattedAddress.ifEmpty { "Address not available" }

            // Show stock information for the selected blood type
            if (selectedBloodType.isNotEmpty()) {
                val stock = hospital.bloodInventory[selectedBloodType] ?: 0
                bloodStock.text = itemView.context.getString(R.string.blood_stock_count, selectedBloodType, stock)
                
                if (stock > 0) {
                    stockAvailable.visibility = View.VISIBLE
                    stockUnavailable.visibility = View.GONE
                } else {
                    stockAvailable.visibility = View.GONE
                    stockUnavailable.visibility = View.VISIBLE
                }
                bloodStock.visibility = View.VISIBLE
            } else {
                // If no blood type selected, show total stock
                val totalStock = hospital.bloodInventory.values.sum()
                bloodStock.text = itemView.context.getString(R.string.total_blood_stock, totalStock)
                bloodStock.visibility = View.VISIBLE
                stockAvailable.visibility = View.GONE
                stockUnavailable.visibility = View.GONE
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
