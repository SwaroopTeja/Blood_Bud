package com.example.blood_bud.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blood_bud.models.Admin
import com.example.blood_bud.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Loading)
    val uiState: StateFlow<AdminUiState> = _uiState
    
    // Current user's email to filter out from the admin list
    private var currentUserEmail: String = ""

    private val _addAdminState = MutableStateFlow<AddAdminState>(AddAdminState.Idle)
    val addAdminState: StateFlow<AddAdminState> = _addAdminState

    init {
        loadAdmins()
    }

    fun setCurrentUserEmail(email: String) {
        if (currentUserEmail != email) {
            currentUserEmail = email
            loadAdmins()
        }
    }
    
    fun loadAdmins() {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                adminRepository.getAllAdmins().collectLatest { admins ->
                    Log.d("AdminViewModel", "Raw admins from Firestore: ${admins.map { "${it.name} (${it.email}) - superAdmin: ${it.isSuperAdmin}" }}")
                    
                    // Filter out super admins and sort by name
                    val nonSuperAdmins = admins
                        .filter { !it.isSuperAdmin }
                        .sortedBy { it.name.lowercase() }
                    
                    Log.d("AdminViewModel", "Non-super admins: ${nonSuperAdmins.map { "${it.name} (${it.email})" }}")
                    
                    _uiState.value = if (nonSuperAdmins.isEmpty()) {
                        AdminUiState.Empty
                    } else {
                        AdminUiState.Success(nonSuperAdmins)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error(e.message ?: "Failed to load admins")
            }
        }
    }

    fun createAdmin(email: String, password: String, name: String) {
        viewModelScope.launch {
            _addAdminState.value = AddAdminState.Loading
            try {
                // Check if the email is the super admin's email
                if (email.equals("swarooproyal777@gmail.com", ignoreCase = true)) {
                    _addAdminState.value = AddAdminState.Error("Cannot create super admin through this interface")
                    return@launch
                }
                
                // Validate password
                if (password.length < 6) {
                    _addAdminState.value = AddAdminState.Error("Password must be at least 6 characters")
                    return@launch
                }
                
                val result = adminRepository.createAdminWithEmailAndPassword(email, password, name)
                result.fold(
                    onSuccess = { admin ->
                        _addAdminState.value = AddAdminState.Success(admin)
                        loadAdmins()
                    },
                    onFailure = {
                        _addAdminState.value = AddAdminState.Error("Failed to create admin: ${it.message}")
                    }
                )
            } catch (e: Exception) {
                _addAdminState.value = AddAdminState.Error("An error occurred: ${e.message}")
            }
        }
    }
    
    fun updateAdminStatus(adminId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                // First get the admin to check if it's the super admin
                val admin = adminRepository.getAdminById(adminId)
                if (admin?.email.equals("swarooproyal777@gmail.com", ignoreCase = true)) {
                    _uiState.value = AdminUiState.Error("Cannot modify super admin status")
                    return@launch
                }
                
                val result = adminRepository.updateAdminStatus(adminId, isActive)
                result.fold(
                    onSuccess = {
                        // The Flow will automatically update the UI
                    },
                    onFailure = { e ->
                        _uiState.value = AdminUiState.Error("Failed to update admin status: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Failed to update admin status: ${e.message}")
            }
        }
    }

    fun deleteAdmin(adminId: String) {
        viewModelScope.launch {
            try {
                // First get the admin to check if it's the super admin
                val admin = adminRepository.getAdminById(adminId)
                if (admin?.email.equals("swarooproyal777@gmail.com", ignoreCase = true)) {
                    _uiState.value = AdminUiState.Error("Cannot delete super admin")
                    return@launch
                }
                
                adminRepository.deleteAdmin(adminId)
                // The Flow will automatically update the UI
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Failed to delete admin: ${e.message}")
            }
        }
    }
}

sealed class AdminUiState {
    object Loading : AdminUiState()
    object Empty : AdminUiState()
    data class Error(val message: String) : AdminUiState()
    data class Success(val admins: List<Admin>) : AdminUiState()
}

sealed class AddAdminState {
    object Idle : AddAdminState()
    object Loading : AddAdminState()
    data class Success(val admin: Admin) : AddAdminState()
    data class Error(val message: String) : AddAdminState()
}
