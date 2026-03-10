package com.example.blood_bud.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ActivityManageAdminsBinding
import com.example.blood_bud.models.Admin
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageAdminsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageAdminsBinding
    private lateinit var adapter: AdminsAdapter
    private val viewModel: AdminViewModel by viewModels()
    private val adminsList = mutableListOf<Admin>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageAdminsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get current user's email from intent
        val currentUserEmail = intent.getStringExtra("CURRENT_USER_EMAIL") ?: run {
            showError("User email not found")
            finish()
            return
        }
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        
        // Set current user email and load admins
        viewModel.setCurrentUserEmail(currentUserEmail)
        
        // Set up FAB
        binding.addAdminFab.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                showAddAdminDialog()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.manage_admins)
    }

    private fun setupRecyclerView() {
        adapter = AdminsAdapter().apply {
            setOnStatusChangedListener { admin, isActive ->
                viewModel.updateAdminStatus(admin.id, isActive)
            }
            setOnDeleteClickListener { admin ->
                showDeleteConfirmation(admin)
            }
        }
        
        binding.adminsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ManageAdminsActivity)
            adapter = this@ManageAdminsActivity.adapter
        }
        
        // Observe UI state
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is AdminUiState.Loading -> {
                        binding.loadingProgressBar.isVisible = true
                        binding.adminsRecyclerView.isVisible = false
                    }
                    is AdminUiState.Empty -> {
                        binding.loadingProgressBar.isVisible = false
                        binding.adminsRecyclerView.isVisible = false
                        showMessage("No admins found")
                    }
                    is AdminUiState.Error -> {
                        binding.loadingProgressBar.isVisible = false
                        showError(state.message)
                    }
                    is AdminUiState.Success -> {
                        binding.loadingProgressBar.isVisible = false
                        binding.adminsRecyclerView.isVisible = true
                        updateAdminList(state.admins)
                    }
                }
            }
        }
        
        // Observe add admin state
        lifecycleScope.launch {
            viewModel.addAdminState.collectLatest { state ->
                when (state) {
                    is AddAdminState.Success -> {
                        binding.loadingProgressBar.isVisible = false
                        showMessage("Admin ${state.admin.email} added successfully")
                        // Refresh the admin list to show the new admin
                        viewModel.loadAdmins()
                    }
                    is AddAdminState.Error -> {
                        binding.loadingProgressBar.isVisible = false
                        showError(state.message)
                    }
                    is AddAdminState.Loading -> {
                        binding.loadingProgressBar.isVisible = true
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupClickListeners() {
        // FAB click listener is now set in onCreate
    }

    private fun showAddAdminDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_admin, null)
        val emailInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.emailInput)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.nameInput)
        val passwordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.passwordInput)
        
        // Show password toggle
        val passwordLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.passwordLayout)
        passwordLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Add New Admin")
            .setView(dialogView)
            .setPositiveButton("Add", null) // We'll set the click listener after dialog creation
            .setNegativeButton("Cancel", null)
            .create()
            
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val email = emailInput.text?.toString()?.trim()
                val name = nameInput.text?.toString()?.trim()
                val password = passwordInput.text?.toString()
                
                when {
                    email.isNullOrEmpty() -> {
                        emailInput.error = "Email is required"
                        return@setOnClickListener
                    }
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        emailInput.error = "Please enter a valid email address"
                        return@setOnClickListener
                    }
                    name.isNullOrEmpty() -> {
                        nameInput.error = "Name is required"
                        return@setOnClickListener
                    }
                    password.isNullOrEmpty() -> {
                        passwordInput.error = "Password is required"
                        return@setOnClickListener
                    }
                    password.length < 6 -> {
                        passwordInput.error = "Password must be at least 6 characters"
                        return@setOnClickListener
                    }
                    else -> {
                        // Clear any previous errors
                        emailInput.error = null
                        nameInput.error = null
                        passwordInput.error = null
                        
                        // Create the admin
                        viewModel.createAdmin(email, password, name)
                        dialog.dismiss()
                    }
                }
            }
        }
        
        dialog.show()
    }
    
    private fun updateAdminList(admins: List<Admin>) {
        adapter.updateAdmins(admins)
        // Show a toast if no admins are found
        if (admins.isEmpty()) {
            showMessage("No admins found")
        }
    }
    
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
    
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Retry") { viewModel.loadAdmins() }
            .show()
    }

    private fun loadAdmins() {
        viewModel.loadAdmins()
    }

    private fun showDeleteConfirmation(admin: Admin) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Admin")
            .setMessage("Are you sure you want to remove ${admin.email} as an admin?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteAdmin(admin.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun start(activity: AppCompatActivity, currentUserEmail: String) {
            val intent = Intent(activity, ManageAdminsActivity::class.java).apply {
                putExtra("CURRENT_USER_EMAIL", currentUserEmail)
            }
            activity.startActivity(intent)
        }
    }
}
