package com.example.blood_bud.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.data.model.StockItem
import com.example.blood_bud.databinding.ItemBloodStockBinding

class BloodStockAdapter : ListAdapter<StockItem, BloodStockAdapter.BloodStockViewHolder>(BloodStockDiffCallback()) {

    var onEditClick: ((StockItem) -> Unit)? = null
    var onDeleteClick: ((StockItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BloodStockViewHolder {
        val binding = ItemBloodStockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BloodStockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BloodStockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BloodStockViewHolder(
        private val binding: ItemBloodStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StockItem) {
            with(binding) {
                tvBloodType.text = item.bloodType
                tvUnitsAvailable.text = "${item.units} units"
                
                // Set click listeners
                btnEdit.setOnClickListener { onEditClick?.invoke(item) }
                btnDelete.setOnClickListener { onDeleteClick?.invoke(item) }
            }
        }
    }

    class BloodStockDiffCallback : DiffUtil.ItemCallback<StockItem>() {
        override fun areItemsTheSame(oldItem: StockItem, newItem: StockItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StockItem, newItem: StockItem): Boolean {
            return oldItem == newItem
        }
    }
}
