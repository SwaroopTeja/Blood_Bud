package com.example.blood_bud.ui.donor

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Hospital
import com.example.blood_bud.data.model.HospitalFilter
import com.example.blood_bud.data.model.LocationData
import com.example.blood_bud.databinding.FragmentSearchBinding
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.RadioButton
import com.example.blood_bud.ui.donor.adapter.HospitalSearchAdapter
import com.example.blood_bud.viewmodel.SearchViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var hospitalAdapter: HospitalSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SearchFragment", "onViewCreated: Initializing UI components")

        setupRecyclerView()
        Log.d("SearchFragment", "RecyclerView setup complete")
        
        setupSearch()
        Log.d("SearchFragment", "Search setup complete")
        
        observeViewModel()
        Log.d("SearchFragment", "ViewModel observation started")
        
        // Set up filter button click listener
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun setupRecyclerView() {
        Log.d("SearchFragment", "Setting up RecyclerView...")
        hospitalAdapter = HospitalSearchAdapter(
            onItemClick = { hospital ->
                Log.d("SearchFragment", "Hospital clicked: ${hospital.name} (${hospital.id})")
                showHospitalDetails(hospital)
            }
        )
        
        binding.rvDonationCenters.apply {
            Log.d("SearchFragment", "Configuring RecyclerView layout")
            layoutManager = LinearLayoutManager(requireContext())
            adapter = hospitalAdapter
            setHasFixedSize(true)
            itemAnimator = null // Disable animations to prevent flickering
            Log.d("SearchFragment", "RecyclerView layout configured")
        }
        
        Log.d("SearchFragment", "RecyclerView setup complete with adapter: $hospitalAdapter")
        Log.d("SearchFragment", "RecyclerView visibility: ${binding.rvDonationCenters.visibility}")
        Log.d("SearchFragment", "No centers text visibility: ${binding.tvNoCenters.visibility}")
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { viewModel.onSearchQueryChanged(it.toString()) }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showFilterDialog() {
        val bottomSheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_filter, null)

        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Initialize views
        val bloodTypeGroup = bottomSheetView.findViewById<RadioGroup>(R.id.bloodTypeGroup)
        val radioAPos = bottomSheetView.findViewById<RadioButton>(R.id.radioAPos)
        val radioANeg = bottomSheetView.findViewById<RadioButton>(R.id.radioANeg)
        val radioBPos = bottomSheetView.findViewById<RadioButton>(R.id.radioBPos)
        val radioBNeg = bottomSheetView.findViewById<RadioButton>(R.id.radioBNeg)
        val radioOPos = bottomSheetView.findViewById<RadioButton>(R.id.radioOPos)
        val radioONeg = bottomSheetView.findViewById<RadioButton>(R.id.radioONeg)
        val radioABPos = bottomSheetView.findViewById<RadioButton>(R.id.radioABPos)
        val radioABNeg = bottomSheetView.findViewById<RadioButton>(R.id.radioABNeg)
        val radioAll = bottomSheetView.findViewById<RadioButton>(R.id.radioAll)
        
        val dropdownCity = bottomSheetView.findViewById<AutoCompleteTextView>(R.id.dropdownCity)
        val dropdownState = bottomSheetView.findViewById<AutoCompleteTextView>(R.id.dropdownState)
        
        val btnApply = bottomSheetView.findViewById<Button>(R.id.btnApply)
        val btnReset = bottomSheetView.findViewById<Button>(R.id.btnReset)

        // Get location data
        val locationData = LocationData.indianLocations
        val states = locationData.states.map { it.name }.toTypedArray()
        
        // Set up state dropdown
        val stateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, states)
        dropdownState.setAdapter(stateAdapter)
        
        // Update cities when state is selected
        dropdownState.setOnItemClickListener { _, _, position, _ ->
            val selectedState = states[position]
            val cities = locationData.states.find { it.name == selectedState }?.cities?.toTypedArray() ?: emptyArray()
            val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cities)
            dropdownCity.setAdapter(cityAdapter)
            dropdownCity.text.clear()
        }

        // Set current filter values
        val currentFilter = viewModel.currentFilter.value ?: HospitalFilter()
        
        // Set blood type radio buttons
        currentFilter.bloodType?.let { bloodType ->
            when (bloodType) {
                "A+" -> radioAPos.isChecked = true
                "A-" -> radioANeg.isChecked = true
                "B+" -> radioBPos.isChecked = true
                "B-" -> radioBNeg.isChecked = true
                "O+" -> radioOPos.isChecked = true
                "O-" -> radioONeg.isChecked = true
                "AB+" -> radioABPos.isChecked = true
                "AB-" -> radioABNeg.isChecked = true
                else -> radioAll.isChecked = true
            }
        } ?: run {
            radioAll.isChecked = true
        }
        
        // Set location filters
        currentFilter.cities.firstOrNull()?.let { city ->
            dropdownCity.setText(city, false)
        }
        
        currentFilter.states.firstOrNull()?.let { state ->
            dropdownState.setText(state, false)
            // Set cities for the selected state
            val cities = locationData.states.find { it.name == state }?.cities?.toTypedArray() ?: emptyArray()
            val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cities)
            dropdownCity.setAdapter(cityAdapter)
        }

        // Handle apply button click
        btnApply.setOnClickListener {
            val selectedBloodType = when (bloodTypeGroup.checkedRadioButtonId) {
                R.id.radioAPos -> "A+"
                R.id.radioANeg -> "A-"
                R.id.radioBPos -> "B+"
                R.id.radioBNeg -> "B-"
                R.id.radioOPos -> "O+"
                R.id.radioONeg -> "O-"
                R.id.radioABPos -> "AB+"
                R.id.radioABNeg -> "AB-"
                else -> null
            }
            
            val selectedState = dropdownState.text.toString()
            val selectedCity = dropdownCity.text.toString()
            
            viewModel.updateFilter(
                bloodType = selectedBloodType,
                states = if (selectedState.isNotEmpty()) listOf(selectedState) else emptyList(),
                cities = if (selectedCity.isNotEmpty()) listOf(selectedCity) else emptyList()
            )
            
            bottomSheetDialog.dismiss()
        }
        
        // Handle reset button click
        btnReset.setOnClickListener {
            // Reset radio group to default (All Blood Types)
            bloodTypeGroup.check(R.id.radioAll)
            
            // Clear location fields
            dropdownCity.text.clear()
            dropdownState.text.clear()
            
            // Reset filters in ViewModel
            viewModel.resetFilters()
            
            // Dismiss the dialog
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }

    private fun observeViewModel() {
        Log.d("SearchFragment", "Starting to observe ViewModel")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d("SearchFragment", "Lifecycle is STARTED")
                
                // Collect hospitals
                launch {
                    Log.d("SearchFragment", "Starting to collect hospitals")
                    viewModel.hospitals.collect { hospitals ->
                        Log.d("SearchFragment", "Received ${hospitals.size} hospitals from ViewModel")
                        if (hospitals.isNotEmpty()) {
                            Log.d("SearchFragment", "First hospital: ${hospitals[0].name} (${hospitals[0].id})")
                        }
                        
                        hospitalAdapter.submitList(hospitals) {
                            Log.d("SearchFragment", "Adapter list updated with ${hospitals.size} items")
                            binding.rvDonationCenters.scrollToPosition(0)
                            
                            // Force layout update
                            binding.rvDonationCenters.post {
                                binding.rvDonationCenters.invalidate()
                            }
                        }
                        
                        if (hospitals.isEmpty()) {
                            Log.d("SearchFragment", "No hospitals to display")
                            binding.tvNoCenters.visibility = View.VISIBLE
                            binding.rvDonationCenters.visibility = View.GONE
                        } else {
                            Log.d("SearchFragment", "Displaying ${hospitals.size} hospitals")
                            binding.tvNoCenters.visibility = View.GONE
                            binding.rvDonationCenters.visibility = View.VISIBLE
                            
                            // Debug: Check if RecyclerView has an adapter
                            Log.d("SearchFragment", "RecyclerView has adapter: ${binding.rvDonationCenters.adapter != null}")
                            Log.d("SearchFragment", "RecyclerView visibility: ${binding.rvDonationCenters.visibility}")
                            Log.d("SearchFragment", "No centers text visibility: ${binding.tvNoCenters.visibility}")
                        }
                    }
                }

                // Collect loading state
                launch {
                    Log.d("SearchFragment", "Starting to observe loading state")
                    viewModel.isLoading.collect { isLoading ->
                        Log.d("SearchFragment", "Loading state changed: $isLoading")
                        binding.loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                // Collect errors
                launch {
                    Log.d("SearchFragment", "Starting to observe errors")
                    viewModel.error.collect { error ->
                        error?.let { errorMessage ->
                            Log.e("SearchFragment", "Error from ViewModel: $errorMessage")
                            Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).apply {
                                setAction("Dismiss") { dismiss() }
                                show()
                            }
                            viewModel.onErrorShown()
                        }
                    }
                }

                // Observe filter changes
                launch {
                    Log.d("SearchFragment", "Starting to observe filter changes")
                    viewModel.currentFilter.collect { filter ->
                        Log.d("SearchFragment", "Filter updated: $filter")
                        // The hospitals list will be automatically updated by the ViewModel
                    }
                }
            }
        }
    }

    private fun showHospitalDetails(hospital: Hospital) {
        // TODO: Implement hospital details navigation
        // For now, just show a toast
        Toast.makeText(requireContext(), "Selected: ${hospital.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
