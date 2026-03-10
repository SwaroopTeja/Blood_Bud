package com.example.blood_bud.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.R
import com.example.blood_bud.data.model.StockItem
import com.example.blood_bud.databinding.ItemBloodStockBinding

/**
 * Adapter for displaying a list of blood stock items with edit and delete actions.
 * @param onEditClick Callback when the edit button is clicked
 * @param onDeleteClick Callback when the delete button is clicked
 */

class StockAdapter(
    private val onEditClick: (StockItem) -> Unit,
    private val onDeleteClick: (StockItem) -> Unit
) : ListAdapter<StockItem, StockAdapter.StockViewHolder>(StockDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemBloodStockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        
        // Set click listeners for edit and delete buttons
        holder.binding.btnEdit.setOnClickListener { onEditClick(item) }
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    inner class StockViewHolder(
        val binding: ItemBloodStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        /**
         * Binds a stock item to the view holder
         * @param item The stock item to bind
         */
        fun bind(item: StockItem) {
            binding.apply {
                // Set blood type and units
                tvBloodType.text = item.bloodType
                tvUnitsAvailable.text = "${item.units} units"
                
                // Set button click listeners
                btnEdit.setOnClickListener { onEditClick(item) }
                btnDelete.setOnClickListener { onDeleteClick(item) }
            }
        }
    }
}

/**
 * Callback for calculating the difference between two StockItems in the list
 */
private class StockDiffCallback : DiffUtil.ItemCallback<StockItem>() {
    
    /**
     * Check if items represent the same object
     */
    override fun areItemsTheSame(oldItem: StockItem, newItem: StockItem): Boolean {
        return oldItem.bloodType == newItem.bloodType
    }
    
    /**
     * Check if items have the same data
     */

    override fun areContentsTheSame(oldItem: StockItem, newItem: StockItem): Boolean {
        return oldItem == newItem
    }
}
