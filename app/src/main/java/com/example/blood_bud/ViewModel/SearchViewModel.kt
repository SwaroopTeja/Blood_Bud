package com.example.blood_bud.viewmodel

import android.util.Log
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

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _currentFilter = MutableStateFlow(HospitalFilter())
    val currentFilter: StateFlow<HospitalFilter> = _currentFilter

    private val _hospitals = MutableStateFlow<List<Hospital>>(emptyList())
    val hospitals: StateFlow<List<Hospital>> = _hospitals

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadHospitals()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterHospitals()
    }

    fun onErrorShown() {
        _error.value = null
    }

    private fun loadHospitals() {
        Log.d("SearchViewModel", "Starting to load hospitals...")
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d("SearchViewModel", "Calling Firestore to get hospitals")
                firestore.collection("hospitals")
                    .whereEqualTo("status", "approved")
                    .limit(100)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        var results = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Hospital::class.java)?.copy(id = doc.id)
                        }

                        Log.d("SearchViewModel", "Received ${results.size} hospitals from Firestore")
                        if (results.isEmpty()) {
                            Log.w("SearchViewModel", "No hospitals found in Firestore")
                        } else {
                            Log.d("SearchViewModel", "First hospital: ${results[0].name} (${results[0].id}), status: ${results[0].status}")
                        }

                        // Filter for approved hospitals
                        val approvedHospitals = results.filter { it.status == "approved" }
                        Log.d("SearchViewModel", "Filtered to ${approvedHospitals.size} active hospitals")

                        if (approvedHospitals.isEmpty()) {
                            Log.w("SearchViewModel", "No active hospitals found after filtering")
                        }

                        _hospitals.value = approvedHospitals
                        _isLoading.value = false

                        // Log details of all hospitals for debugging
                        approvedHospitals.forEachIndexed { index, hospital ->
                            Log.d("SearchViewModel",
                                "Hospital $index: ${hospital.name} (${hospital.id}), " +
                                "Status: ${hospital.status}, " +
                                "Address: ${hospital.address}, " +
                                "Blood Inventory: ${hospital.bloodInventory}"
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        val errorMsg = "Error loading hospitals: ${e.message}"
                        _error.value = errorMsg
                        _isLoading.value = false
                        Log.e("SearchViewModel", errorMsg, e)
                    }
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message}"
                _isLoading.value = false
                Log.e("SearchViewModel", "Unexpected error in loadHospitals", e)
            }
        }
    }

    private fun filterHospitals() {
        val query = _searchQuery.value.lowercase()
        val filter = _currentFilter.value

        Log.d("SearchViewModel", "Filtering hospitals - Query: '$query', Filter: $filter")

        viewModelScope.launch {
            try {
                firestore.collection("hospitals")
                    .whereEqualTo("status", "approved")
                    .limit(100)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        var results = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Hospital::class.java)?.copy(id = doc.id)
                        }

                        // Apply search query filter
                        val q = query.trim()
                        if (q.isNotEmpty()) {
                            results = results.filter { it.name.contains(q, ignoreCase = true) }
                        }

                        // Apply blood type filter
                        filter.bloodType?.takeIf { it.isNotBlank() }?.let { bloodType ->
                            results = results.filter { hospital ->
                                val inventory = hospital.bloodInventory[bloodType] ?: 0
                                inventory > 0
                            }
                        }

                        // Apply location filter
                        if (filter.states.isNotEmpty()) {
                            results = results.filter { hospital ->
                                val addrState = (hospital.address["state"] as? String).orEmpty().ifEmpty { hospital.state }
                                addrState in filter.states
                            }
                        }

                        if (filter.cities.isNotEmpty()) {
                            results = results.filter { hospital ->
                                val addrCity = (hospital.address["city"] as? String).orEmpty().ifEmpty { hospital.city }
                                addrCity in filter.cities
                            }
                        }

                        // Sort by blood inventory count for the selected blood type (if any)
                        results = results.sortedByDescending { hospital ->
                            filter.bloodType?.let { bloodType ->
                                hospital.bloodInventory[bloodType] ?: 0
                            } ?: hospital.bloodInventory.values.sum()
                        }

                        Log.d("SearchViewModel", "Filtered to ${results.size} hospitals")
                        _hospitals.value = results
                    }
                    .addOnFailureListener { e ->
                        val errorMsg = "Error filtering hospitals: ${e.message}"
                        _error.value = errorMsg
                        Log.e("SearchViewModel", errorMsg, e)
                        _hospitals.value = emptyList()
                    }
            } catch (e: Exception) {
                val errorMsg = "Unexpected error in filterHospitals: ${e.message}"
                _error.value = errorMsg
                Log.e("SearchViewModel", errorMsg, e)
                _hospitals.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun updateFilter(bloodType: String? = null, cities: List<String> = emptyList(), states: List<String> = emptyList()) {
        _currentFilter.value = HospitalFilter(
            bloodType = bloodType ?: _currentFilter.value.bloodType,
            cities = cities.ifEmpty { _currentFilter.value.cities },
            states = states.ifEmpty { _currentFilter.value.states }
        )
        filterHospitals()
    }

    fun resetFilters() {
        _currentFilter.value = HospitalFilter()
        filterHospitals()
    }

    // Legacy methods for backward compatibility
    fun searchHospitals(query: String, state: String? = null, city: String? = null) {
        onSearchQueryChanged(query)
        if (state != null || city != null) {
            updateFilter(
                states = state?.let { listOf(it) } ?: emptyList(),
                cities = city?.let { listOf(it) } ?: emptyList()
            )
        }
    }

    fun applyFilters(bloodType: String, state: String, district: String) {
        val bloodTypeValue = bloodType.takeIf { it.isNotBlank() }
        val stateValue = state.takeIf { it.isNotBlank() }
        val districtValue = district.takeIf { it.isNotBlank() }

        updateFilter(
            bloodType = bloodTypeValue,
            states = stateValue?.let { listOf(it) } ?: emptyList(),
            cities = districtValue?.let { listOf(it) } ?: emptyList()
        )
    }

    fun clearFilters() {
        resetFilters()
    }
}
