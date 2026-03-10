package com.example.blood_bud.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.example.blood_bud.R
import com.example.blood_bud.data.model.User
import com.example.blood_bud.data.model.UserType
import com.example.blood_bud.databinding.ItemUserBinding

class UsersAdapter(
    private val onStatusUpdate: (userId: String, isActive: Boolean) -> Unit,
    private val onDeleteUser: (userId: String) -> Unit
) : ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    private var isUpdating = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentUser: User? = null

        init {
            // Handle delete button click
            binding.deleteButton.setOnClickListener {
                currentUser?.let { user ->
                    onDeleteUser(user.id)
                }
            }

            // Handle status switch changes for hospitals
            binding.statusSwitch.setOnCheckedChangeListener { _, isChecked ->
                currentUser?.let { user ->
                    if (user.userType == UserType.HOSPITAL) {
                        binding.btnUpdateStatus.visibility = View.VISIBLE
                    }
                }
            }

            // Handle update status button click
            binding.btnUpdateStatus.setOnClickListener {
                currentUser?.let { user ->
                    onStatusUpdate(user.id, binding.statusSwitch.isChecked)
                    binding.btnUpdateStatus.visibility = View.GONE
                }
            }
        }

        fun bind(user: User) {
            currentUser = user

            binding.apply {
                // Set user info
                userName.text = user.name
                userEmail.text = user.email
                userType.text = user.userType.name

                // Set user type background color
                val context = root.context
                userType.setBackgroundColor(
                    when (user.userType) {
                        UserType.DONOR -> context.getColor(R.color.donor_color)
                        UserType.HOSPITAL -> context.getColor(R.color.hospital_color)
                        UserType.ADMIN -> context.getColor(R.color.admin_color)
                    }
                )

                // Configure status switch
                statusSwitch.isChecked = user.isActive
                statusSwitch.visibility =
                    if (user.userType == UserType.HOSPITAL) View.VISIBLE else View.GONE

                // Show delete button for non-admin users
                deleteButton.visibility =
                    if (user.userType != UserType.ADMIN) View.VISIBLE else View.GONE

                // Hide update button initially
                btnUpdateStatus.visibility = View.GONE
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
