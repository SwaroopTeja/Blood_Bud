package com.example.blood_bud.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ActivityManageUsersBinding
import com.example.blood_bud.util.showToast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManageUsersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageUsersBinding
    private val viewModel: ManageUsersViewModel by viewModels()
    private lateinit var usersAdapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.manage_users)
        }
    }

    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter(
            onStatusUpdate = { userId, isActive ->
                lifecycleScope.launch {
                    viewModel.updateUserStatus(userId, isActive)
                }
            },
            onDeleteUser = { userId ->
                viewModel.deleteUser(userId)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ManageUsersActivity)
            adapter = usersAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.users.collect { users ->
                usersAdapter.submitList(users)
                binding.emptyView.isVisible = users.isEmpty()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.isVisible = isLoading
                binding.recyclerView.isVisible = !isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let { message ->
                    Toast.makeText(this@ManageUsersActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun showDeleteConfirmation(userId: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete this user?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteUser(userId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
