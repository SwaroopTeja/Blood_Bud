package com.example.blood_bud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blood_bud.data.model.Hospital
import com.example.blood_bud.data.model.HospitalFilter
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HospitalsUiState {
    object Loading : HospitalsUiState()
    data class Success(val hospitals: List<Hospital>) : HospitalsUiState()
    data class Error(val message: String) : HospitalsUiState()
}

@HiltViewModel
class HospitalsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<HospitalsUiState>(HospitalsUiState.Loading)
    val uiState: StateFlow<HospitalsUiState> = _uiState

    fun loadHospitals(filter: HospitalFilter? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = HospitalsUiState.Loading

                var query = firestore.collection("hospitals")
                    .whereEqualTo("status", "approved")

                // Apply filters if provided
                filter?.let { f ->
                    f.states.firstOrNull()?.let { state -> query = query.whereEqualTo("state", state) }
                    f.cities.firstOrNull()?.let { city -> query = query.whereEqualTo("city", city) }
                }

                query.get()
                    .addOnSuccessListener { snapshot ->
                        val hospitals = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Hospital::class.java)?.copy(id = doc.id)
                        }
                        _uiState.value = HospitalsUiState.Success(hospitals)
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = HospitalsUiState.Error(
                            e.message ?: "Failed to load hospitals"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = HospitalsUiState.Error(
                    e.message ?: "An unknown error occurred"
                )
            }
        }
    }
}
