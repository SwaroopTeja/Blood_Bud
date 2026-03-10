package com.example.blood_bud.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ItemAdminBinding
import com.example.blood_bud.models.Admin

class AdminsAdapter : RecyclerView.Adapter<AdminsAdapter.AdminViewHolder>() {
    
    private val admins = mutableListOf<Admin>()
    private var onStatusChanged: ((Admin, Boolean) -> Unit) = { _, _ -> }
    private var onDeleteClicked: ((Admin) -> Unit) = { }
    
    fun setOnStatusChangedListener(listener: (Admin, Boolean) -> Unit) {
        onStatusChanged = listener
    }
    
    fun setOnDeleteClickListener(listener: (Admin) -> Unit) {
        onDeleteClicked = listener
    }
    

    class AdminViewHolder(
        private val binding: ItemAdminBinding,
        private val onStatusChanged: (Admin, Boolean) -> Unit,
        private val onDeleteClicked: (Admin) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentAdmin: Admin? = null

        init {
            binding.adminStatusSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) { // Only trigger on user interaction
                    currentAdmin?.let { admin ->
                        // Update UI immediately for better UX
                        binding.adminStatusSwitch.isChecked = isChecked
                        // Notify the listener
                        onStatusChanged(admin, isChecked)
                    }
                }
            }

            binding.deleteButton.setOnClickListener {
                currentAdmin?.let { admin ->
                    onDeleteClicked(admin)
                }
            }
        }

        fun bind(admin: Admin) {
            currentAdmin = admin
            binding.apply {
                adminEmail.text = admin.email
                // Remove the listener to avoid triggering it when setting the checked state
                adminStatusSwitch.setOnCheckedChangeListener(null)
                adminStatusSwitch.isChecked = admin.isActive
                // Set the listener back
                adminStatusSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        onStatusChanged(admin, isChecked)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val binding = ItemAdminBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdminViewHolder(
            binding = binding,
            onStatusChanged = { admin, isActive -> onStatusChanged(admin, isActive) },
            onDeleteClicked = { admin -> onDeleteClicked(admin) }
        )
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        holder.bind(admins[position])
    }

    override fun getItemCount(): Int = admins.size

    fun updateAdmins(newAdmins: List<Admin>) {
        val diffCallback = object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize(): Int = admins.size
            override fun getNewListSize(): Int = newAdmins.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return admins[oldPos].id == newAdmins[newPos].id
            }
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return admins[oldPos] == newAdmins[newPos]
            }
        }
        
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffCallback)
        admins.clear()
        admins.addAll(newAdmins)
        diffResult.dispatchUpdatesTo(this)
    }
}
