package com.example.blood_bud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blood_bud.data.model.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AppointmentsUiState {
    object Loading : AppointmentsUiState()
    data class Success(val appointments: List<Appointment>) : AppointmentsUiState()
    data class Error(val message: String) : AppointmentsUiState()
}

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppointmentsUiState>(AppointmentsUiState.Loading)
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    init {
        loadAppointments()
    }

    fun loadAppointments() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = AppointmentsUiState.Error("User not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = AppointmentsUiState.Loading
                
                firestore.collection("appointments")
                    .whereEqualTo("donorId", currentUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _uiState.value = AppointmentsUiState.Error(
                                error.message ?: "Failed to load appointments"
                            )
                            return@addSnapshotListener
                        }

                        val appointments = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject<Appointment>()?.copy(appointmentId = doc.id)
                        } ?: emptyList()

                        _uiState.value = AppointmentsUiState.Success(appointments)
                    }
            } catch (e: Exception) {
                _uiState.value = AppointmentsUiState.Error(
                    e.message ?: "An unknown error occurred"
                )
            }
        }
    }

    fun refresh() {
        loadAppointments()
    }
}
