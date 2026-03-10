package com.example.blood_bud.ui.donor

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Hospital
import com.example.blood_bud.data.model.LocationData
import com.example.blood_bud.databinding.FragmentDonateBloodBinding
import com.example.blood_bud.Adapter.SearchHospitalAdapter
import com.example.blood_bud.viewmodel.HospitalSearchViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DonateBloodFragment : Fragment() {

    private var _binding: FragmentDonateBloodBinding? = null
    private val binding get() = _binding!!

    private lateinit var hospitalSearchAdapter: SearchHospitalAdapter
    private val viewModel: HospitalSearchViewModel by viewModels()
    private val locationData = LocationData.indianLocations
    private var currentState: LocationData.State? = null
    private var currentCity: String? = null

    init {
        Log.d("DonateBloodFragment", "🚀 DonateBloodFragment INSTANCE CREATED")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("DonateBloodFragment", "🎯 onCreateView CALLED - Inflating layout")
        _binding = FragmentDonateBloodBinding.inflate(inflater, container, false)
        Log.d("DonateBloodFragment", "🎯 Layout inflated - Root view: ${binding.root}")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DonateBloodFragment", "========== FRAGMENT VIEW CREATED ==========")

        // Temporarily remove adapter for testing - add test TextView to RecyclerView area\n        val testTextView = TextView(requireContext())\n        testTextView.text = \"TEST: RecyclerView area is visible!\"\n        testTextView.textSize = 24f\n        testTextView.setTextColor(Color.BLACK)\n        testTextView.gravity = Gravity.CENTER\n        testTextView.setBackgroundColor(Color.YELLOW)\n        testTextView.layoutParams = ViewGroup.LayoutParams(\n            ViewGroup.LayoutParams.MATCH_PARENT,\n            ViewGroup.LayoutParams.MATCH_PARENT\n        )\n        \n        // Add test TextView to RecyclerView parent\n        binding.rvDonationCenters.parent?.let { parent ->\n            if (parent is ViewGroup) {\n                parent.addView(testTextView)\n                binding.rvDonationCenters.visibility = View.VISIBLE\n            }\n        }\n        \n        Log.d(\"DonateBloodFragment\", \"🎯 Added test TextView to RecyclerView area\")\n\n        setupToolbar()
        setupStateDropdown()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        Log.d("DonateBloodFragment", "========== FRAGMENT SETUP COMPLETE ==========")
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Find Donation Center"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupStateDropdown() {
        val stateNames = locationData.states.map { it.name }
        val stateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, stateNames)

        binding.autoCompleteState.apply {
            setAdapter(stateAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }
        }

        binding.autoCompleteState.setOnItemClickListener { _, _, position, _ ->
            val selectedState = locationData.states[position]
            currentState = selectedState
            binding.cityLayout.isEnabled = true
            binding.autoCompleteCity.text.clear()

            val cityAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                selectedState.cities
            )

            binding.autoCompleteCity.apply {
                setAdapter(cityAdapter)
                threshold = 0
                setOnClickListener { showDropDown() }
                setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }
            }

            binding.autoCompleteState.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && binding.autoCompleteState.text.isNullOrEmpty()) {
                    binding.cityLayout.isEnabled = false
                    binding.autoCompleteCity.text.clear()
                    currentState = null
                }
            }
        }
    }

    private fun setupRecyclerView() {
        Log.d("DonateBloodFragment", "========== SETTING UP RECYCLERVIEW ==========")
        Log.d("DonateBloodFragment", "RecyclerView ID: ${binding.rvDonationCenters.id}")
        Log.d("DonateBloodFragment", "RecyclerView width: ${binding.rvDonationCenters.width}")
        Log.d("DonateBloodFragment", "RecyclerView height: ${binding.rvDonationCenters.height}")
        Log.d("DonateBloodFragment", "RecyclerView visibility: ${binding.rvDonationCenters.visibility}")

        binding.rvDonationCenters.apply {
            Log.d("DonateBloodFragment", "Setting up RecyclerView - adapter: ${hospitalSearchAdapter.itemCount} items initially")
            adapter = hospitalSearchAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        Log.d("DonateBloodFragment", "RecyclerView setup complete")
        Log.d("DonateBloodFragment", "Final RecyclerView visibility: ${binding.rvDonationCenters.visibility}")
        Log.d("DonateBloodFragment", "========== RECYCLERVIEW SETUP COMPLETE ==========")
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d("DonateBloodFragment", "State update - Loading: ${state.isLoading}, Hospitals: ${state.hospitals.size}")

                    // Update hospital list
                    hospitalSearchAdapter.submitList(state.hospitals)
                    Log.d("DonateBloodFragment", "Submitted ${state.hospitals.size} hospitals to adapter")

                    // Handle loading state
                    if (state.isLoading) {
                        Log.d("DonateBloodFragment", "🔄 SETTING LOADING STATE")
                        Log.d("DonateBloodFragment", "ProgressBar visibility: VISIBLE")
                        Log.d("DonateBloodFragment", "RecyclerView visibility: GONE")
                        Log.d("DonateBloodFragment", "EmptyState visibility: GONE")
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvDonationCenters.visibility = View.GONE
                        binding.emptyState.visibility = View.GONE
                        binding.tvResultsTitle.visibility = View.GONE
                    } else {
                        Log.d("DonateBloodFragment", "✅ SETTING DISPLAY STATE - Hospitals: ${state.hospitals.size}")

                        if (state.hospitals.isEmpty()) {
                            Log.d("DonateBloodFragment", "📭 NO HOSPITALS - SHOWING EMPTY STATE")
                            Log.d("DonateBloodFragment", "RecyclerView visibility: GONE")
                            Log.d("DonateBloodFragment", "EmptyState visibility: VISIBLE")
                            binding.rvDonationCenters.visibility = View.GONE
                            binding.emptyState.visibility = View.VISIBLE
                            binding.tvResultsTitle.visibility = View.GONE
                            binding.emptyState.text = state.errorMessage ?: "No hospitals found"
                        } else {
                            Log.d("DonateBloodFragment", "🏥 HOSPITALS FOUND - SHOWING LIST")
                            Log.d("DonateBloodFragment", "RecyclerView visibility: VISIBLE")
                            Log.d("DonateBloodFragment", "EmptyState visibility: GONE")
                            Log.d("DonateBloodFragment", "Title visibility: VISIBLE")
                            binding.rvDonationCenters.visibility = View.VISIBLE
                            binding.emptyState.visibility = View.GONE
                            binding.tvResultsTitle.visibility = View.VISIBLE
                            binding.tvResultsTitle.text = "${state.hospitals.size} Hospitals Available"

                            Log.d("DonateBloodFragment", "RecyclerView dimensions: ${binding.rvDonationCenters.width}x${binding.rvDonationCenters.height}")
                            Log.d("DonateBloodFragment", "Adapter item count: ${hospitalSearchAdapter.itemCount}")
                        }
                    }

                    // Show error message if any
                    state.errorMessage?.let { error ->
                        Log.e("DonateBloodFragment", "Error: $error")
                        showMessage(error)
                        viewModel.clearError()
                    }
                }
            }
        }

        // Observe cities for dropdown
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cities.collect { cities ->
                    Log.d("DonateBloodFragment", "Cities loaded: ${cities.size}")
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSearch.setOnClickListener {
            val state = binding.autoCompleteState.text.toString()
            val city = binding.autoCompleteCity.text.toString()

            if (state.isBlank() || city.isBlank()) {
                showMessage("Please select both state and city")
                return@setOnClickListener
            }

            currentCity = city
            viewModel.searchHospitals(state, city)
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
