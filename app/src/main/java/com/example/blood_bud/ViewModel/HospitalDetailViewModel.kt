package com.example.blood_bud.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blood_bud.data.model.AppointmentSlot
import com.example.blood_bud.data.model.Hospital
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HospitalDetailUiState {
    object Loading : HospitalDetailUiState()
    data class Success(val hospital: Hospital, val slots: List<AppointmentSlot>) : HospitalDetailUiState()
    data class Error(val message: String) : HospitalDetailUiState()
}

@HiltViewModel
class HospitalDetailViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val hospitalId: String = savedStateHandle["hospitalId"] ?: ""

    private val _uiState = MutableStateFlow<HospitalDetailUiState>(HospitalDetailUiState.Loading)
    val uiState: StateFlow<HospitalDetailUiState> = _uiState

    init {
        loadHospitalDetails()
    }

    fun loadHospitalDetails() {
        if (hospitalId.isEmpty()) {
            _uiState.value = HospitalDetailUiState.Error("Invalid hospital ID")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = HospitalDetailUiState.Loading

                // Load hospital details
                firestore.collection("hospitals").document(hospitalId)
                    .get()
                    .addOnSuccessListener { hospitalDoc ->
                        val hospital = hospitalDoc.toObject(Hospital::class.java)?.copy(id = hospitalDoc.id)

                        if (hospital == null) {
                            _uiState.value = HospitalDetailUiState.Error("Hospital not found")
                            return@addOnSuccessListener
                        }

                        // Load available slots for this hospital
                        loadAvailableSlots(hospital)
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = HospitalDetailUiState.Error(
                            e.message ?: "Failed to load hospital details"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = HospitalDetailUiState.Error(
                    e.message ?: "An unknown error occurred"
                )
            }
        }
    }

    private fun loadAvailableSlots(hospital: Hospital) {
        firestore.collection("hospitals")
            .document(hospitalId)
            .collection("slots")
            .whereEqualTo("available", true)
            .limit(10)
            .get()
            .addOnSuccessListener { slotsSnapshot ->
                val slots = slotsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(AppointmentSlot::class.java)?.copy(id = doc.id)
                }
                _uiState.value = HospitalDetailUiState.Success(hospital, slots)
            }
            .addOnFailureListener { e ->
                _uiState.value = HospitalDetailUiState.Error(
                    e.message ?: "Failed to load slots"
                )
            }
    }
}
