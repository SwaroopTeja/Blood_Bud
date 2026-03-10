package com.example.blood_bud.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blood_bud.data.model.Hospital
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

sealed class ManageHospitalsUiState {
    object Loading : ManageHospitalsUiState()
    data class Success(val hospitals: List<Hospital>) : ManageHospitalsUiState()
    data class Error(val message: String) : ManageHospitalsUiState()
}

@HiltViewModel
class ManageHospitalsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManageHospitalsUiState>(ManageHospitalsUiState.Loading)
    val uiState: StateFlow<ManageHospitalsUiState> = _uiState

    init {
        loadHospitals()
    }

    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    fun loadHospitals() {
        if (!isUserAuthenticated()) {
            _uiState.value = ManageHospitalsUiState.Error("User not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = ManageHospitalsUiState.Loading
                
                val snapshot = firestore.collection("hospitals")
                    .get()
                    .await()
                    
                val hospitals = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Hospital::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _uiState.value = if (hospitals.isNotEmpty()) {
                    ManageHospitalsUiState.Success(hospitals)
                } else {
                    ManageHospitalsUiState.Success(emptyList())
                }
                
            } catch (e: Exception) {
                _uiState.value = ManageHospitalsUiState.Error(
                    e.message ?: "Failed to load hospitals"
                )
            }
        }
    }

    fun approveHospital(hospitalId: String): LiveData<Result<Unit>> = liveData {
        try {
            val updates = hashMapOf<String, Any>(
                "isApproved" to true,
                "status" to "approved",
                "updatedAt" to Date()
            )
            
            firestore.collection("hospitals")
                .document(hospitalId)
                .update(updates)
                .await()
                
            // Update local state
            updateHospitalStatus(hospitalId, "approved")
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    fun rejectHospital(hospitalId: String): LiveData<Result<Unit>> = liveData {
        try {
            val updates = hashMapOf<String, Any>(
                "isApproved" to false,
                "status" to "rejected",
                "updatedAt" to Date()
            )
            
            firestore.collection("hospitals")
                .document(hospitalId)
                .update(updates)
                .await()
                
            // Update local state
            updateHospitalStatus(hospitalId, "rejected")
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    
    fun deleteHospital(hospitalId: String) {
        if (hospitalId.isBlank()) {
            _uiState.value = ManageHospitalsUiState.Error("Invalid hospital ID")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("hospitals")
                    .document(hospitalId)
                    .delete()
                    .await()
                
                // Update local state by removing the deleted hospital
                val currentState = _uiState.value
                if (currentState is ManageHospitalsUiState.Success) {
                    val updatedHospitals = currentState.hospitals.filter { it.id != hospitalId }
                    _uiState.value = ManageHospitalsUiState.Success(updatedHospitals)
                }
            } catch (e: Exception) {
                _uiState.value = ManageHospitalsUiState.Error(
                    "Failed to delete hospital: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    private fun updateHospitalStatus(hospitalId: String, status: String) {
        if (hospitalId.isBlank()) {
            _uiState.value = ManageHospitalsUiState.Error("Invalid hospital ID")
            return
        }

        viewModelScope.launch {
            try {
                // Update in Firestore
                firestore.collection("hospitals")
                    .document(hospitalId)
                    .update("status", status)
                    .await()
                
                // Update local state
                val currentState = _uiState.value
                if (currentState is ManageHospitalsUiState.Success) {
                    val updatedHospitals = currentState.hospitals.map { hospital ->
                        if (hospital.id == hospitalId) {
                            // Create a new hospital with updated status
                            hospital.copy(
                                additionalInfo = status // Using additionalInfo to store status for now
                            )
                        } else {
                            hospital
                        }
                    }
                    _uiState.value = ManageHospitalsUiState.Success(updatedHospitals)
                }
            } catch (e: Exception) {
                _uiState.value = ManageHospitalsUiState.Error(
                    "Failed to update hospital status: ${e.message}"
                )
            }
        }
    }
}
