package com.example.blood_bud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blood_bud.data.model.Hospital
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

data class HospitalSearchUiState(
    val isLoading: Boolean = false,
    val hospitals: List<Hospital> = emptyList(),
    val errorMessage: String? = null,
    val cities: List<String> = emptyList()
)

@HiltViewModel
class HospitalSearchViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HospitalSearchUiState(isLoading = true))
    val uiState: StateFlow<HospitalSearchUiState> = _uiState.asStateFlow()

    private val _cities = MutableStateFlow<List<String>>(emptyList())
    val cities: StateFlow<List<String>> = _cities.asStateFlow()

    init {
        Log.d("HospitalSearchViewModel", "Initializing ViewModel")
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            Log.d("HospitalSearchViewModel", "Starting initial data load")
            loadAllHospitals()
            loadCities()
        }
    }

    private fun loadAllHospitals() {
        Log.d("HospitalSearchViewModel", "🔍 STARTING HOSPITAL QUERY")
        Log.d("HospitalSearchViewModel", "Loading approved hospitals from Firestore")

        firestore.collection("hospitals")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("HospitalSearchViewModel", "✅ QUERY SUCCESS - Found ${snapshot.documents.size} documents")
                Log.d("HospitalSearchViewModel", "Document IDs: ${snapshot.documents.map { it.id }}")

                val hospitals = mutableListOf<Hospital>()
                snapshot.documents.forEach { doc ->
                    try {
                        Log.d("HospitalSearchViewModel", "📄 Processing document ${doc.id}: ${doc.data}")
                        val hospital = doc.toObject(Hospital::class.java)?.copy(id = doc.id)
                        if (hospital != null) {
                            hospitals.add(hospital)
                            Log.d("HospitalSearchViewModel", "✅ Parsed hospital: ${hospital.name} - ${hospital.city}, ${hospital.state}")
                        } else {
                            Log.w("HospitalSearchViewModel", "❌ Failed to parse hospital document: ${doc.id}")
                        }
                    } catch (e: Exception) {
                        Log.e("HospitalSearchViewModel", "❌ Error parsing hospital ${doc.id}", e)
                    }
                }

                Log.d("HospitalSearchViewModel", "🏥 Final hospital count: ${hospitals.size}")
                Log.d("HospitalSearchViewModel", "Hospital names: ${hospitals.map { it.name }}")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hospitals = hospitals,
                    errorMessage = null,
                    cities = _cities.value
                )
            }
            .addOnFailureListener { e ->
                Log.e("HospitalSearchViewModel", "❌ QUERY FAILED", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hospitals = emptyList(),
                    errorMessage = "Failed to load hospitals: ${e.message}",
                    cities = _cities.value
                )
            }
    }

    private fun loadCities() {
        firestore.collection("hospitals")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { snapshot ->
                val cities = snapshot.documents
                    .mapNotNull { it.getString("city") }
                    .distinct()
                    .sorted()
                Log.d("HospitalSearchViewModel", "Loaded ${cities.size} cities from approved hospitals: $cities")
                _cities.value = cities
            }
            .addOnFailureListener { e ->
                Log.e("HospitalSearchViewModel", "Failed to load cities from approved hospitals", e)
            }
    }

    fun searchHospitals(state: String, city: String) {
        viewModelScope.launch {
            Log.d("HospitalSearchViewModel", "Searching hospitals in $city, $state")

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            firestore.collection("hospitals")
                .whereEqualTo("state", state)
                .whereEqualTo("city", city)
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.d("HospitalSearchViewModel", "Search query returned ${snapshot.documents.size} hospitals")

                    val hospitals = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Hospital::class.java)?.copy(id = doc.id)
                    }

                    Log.d("HospitalSearchViewModel", "Filtered to ${hospitals.size} hospitals in $city, $state")

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hospitals = hospitals,
                        errorMessage = if (hospitals.isEmpty()) "No hospitals found in $city, $state" else null,
                        cities = _cities.value
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("HospitalSearchViewModel", "Search failed", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hospitals = emptyList(),
                        errorMessage = "Search failed: ${e.message}",
                        cities = _cities.value
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
