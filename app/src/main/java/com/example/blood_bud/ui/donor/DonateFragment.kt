package com.example.blood_bud.ui.donor

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.Activity.AppointmentSchedulingActivity
import com.example.blood_bud.R
import com.example.blood_bud.databinding.FragmentDonateBinding
import com.example.blood_bud.data.model.Appointment
import com.example.blood_bud.data.model.User
import com.example.blood_bud.databinding.ItemSlotBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*

class DonateFragment : Fragment() {
    private var _binding: FragmentDonateBinding? = null
    private val binding get() = _binding  // ← Returns null if _binding is null

    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference
    private var donorId: String? = null

    private var selectedDate: String? = null
    private var selectedHospitalId: String? = null
    private var selectedSlotId: String? = null
    private var selectedState: String? = null
    private var selectedDistrict: String? = null
    private val availableHospitals = mutableListOf<Hospital>()
    private val availableStates = mutableListOf<String>()
    private val availableDistricts = mutableListOf<String>()
    private val stateDistrictsMap = mutableMapOf<String, MutableList<String>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): NestedScrollView? {
        _binding = FragmentDonateBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        // database is already initialized as a val
        donorId = auth.currentUser?.uid

        setupUI()
        checkEligibility()
        loadCurrentAppointment()
        loadStateDistrictFilters()
    }

    private fun setupUI() {
        // Date picker button
        binding?.btnPickDate?.setOnClickListener {
            showDatePicker()
        }

        // Cancel appointment button
        binding?.btnCancelAppointment?.setOnClickListener {
            cancelCurrentAppointment()
        }

        // Setup RecyclerView for available slots
        setupHospitalsRecyclerView()

        // Setup state and district dropdowns
        setupStateDistrictDropdowns()
    }

    private fun checkEligibility() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            updateEligibilityUI(false, "Please log in to check eligibility")
            return
        }

        // For new users with no donation history, they are eligible by default
        val userRef = firestore.collection("users").document(currentUser.uid)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Existing user - check last donation
                val lastDonation = document.getString("lastDonation")
                if (lastDonation.isNullOrEmpty()) {
                    // New user - eligible
                    updateEligibilityUI(true, "You are eligible to donate")
                } else {
                    // Existing user - check 3-month cooldown
                    val lastDonationDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .parse(lastDonation)
                    val daysSinceLastDonation = getDaysSinceLastDonation(lastDonation)
                    val isEligible = daysSinceLastDonation >= 90

                    val message = if (isEligible) {
                        "You are eligible to donate"
                    } else {
                        val daysLeft = 90 - daysSinceLastDonation
                        "You can donate again in $daysLeft days"
                    }
                    updateEligibilityUI(isEligible, message)
                }
            } else {
                // New user - eligible
                updateEligibilityUI(true, "You are eligible to donate")
            }
        }.addOnFailureListener {
            updateEligibilityUI(false, "Error checking eligibility: ${it.message}")
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                binding?.tvSelectedDate?.text = selectedDate
                loadAvailableSlots()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Prevent selecting past dates
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun loadAvailableSlots() {
        if (selectedDate == null) return

        Log.d("DonateFragment", "loadAvailableSlots for date: $selectedDate")
        binding?.progressBar?.visibility = View.VISIBLE
        availableHospitals.clear()

        val dateNode = selectedDate!!
        Log.d("DonateFragment", "Loading slots for date: $dateNode")
        val slotsRef = realtimeDb.child("appointment_slots").child(dateNode.replace("/", "-"))

        slotsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(slotsSnapshot: DataSnapshot) {
                Log.d(
                    "DonateFragment",
                    "slotsSnapshot.exists=${slotsSnapshot.exists()}, children=${slotsSnapshot.childrenCount}"
                )
                availableHospitals.clear()

                if (!slotsSnapshot.exists() || slotsSnapshot.childrenCount == 0L) {
                    // No slots for this date
                    binding?.progressBar?.visibility = View.GONE
                    setupHospitalsRecyclerView()
                    if (isAdded && context != null) {
                        Toast.makeText(
                            requireContext(),
                            "No hospitals with available slots for the selected date",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return
                }

                val totalHospitals = slotsSnapshot.childrenCount.toInt()
                var processedHospitals = 0

                fun hospitalProcessed() {
                    processedHospitals++
                    Log.d(
                        "DonateFragment",
                        "hospitalProcessed: $processedHospitals / $totalHospitals"
                    )
                    if (processedHospitals >= totalHospitals) {
                        // All hospital children processed — update UI
                        setupHospitalsRecyclerView()
                        binding?.progressBar?.visibility = View.GONE

                        if (availableHospitals.isEmpty()) {
                            if (isAdded && context != null) {
                                Toast.makeText(
                                    requireContext(),
                                    "No hospitals with available slots for the selected date",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                // Safety timeout: if some callbacks never return, hide loader after 10 seconds
                val SAFETY_TIMEOUT_MS = 10_000L
                binding?.rvAvailableSlots?.postDelayed({
                    if (binding?.progressBar?.visibility == View.VISIBLE) {
                        Log.w("DonateFragment", "Safety timeout reached while loading hospitals")
                        setupHospitalsRecyclerView()
                        binding?.progressBar?.visibility = View.GONE
                    }
                }, SAFETY_TIMEOUT_MS)

                // Iterate hospitals
                slotsSnapshot.children.forEach { hospitalSnapshot ->
                    val hospitalId = hospitalSnapshot.key ?: run {
                        hospitalProcessed()
                        return@forEach
                    }
                    Log.d("DonateFragment", "Processing hospitalId=$hospitalId")

                    // Calculate available slots count for this hospital (only future slots)
                    var availableSlotsCount = 0
                    val currentTime = System.currentTimeMillis()
                    
                    hospitalSnapshot.children.forEach { slotSnapshot ->
                        val slotData = slotSnapshot.value as? Map<*, *>
                        slotData?.let {
                            val currentBookings = (it["currentBookings"] as? Long)?.toInt() ?: 0
                            val capacity = (it["capacity"] as? Long)?.toInt() ?: 0
                            val endTime = (it["endTime"] as? Long) ?: 0
                            
                            // Only count slots that haven't passed
                            if (endTime > currentTime) {
                                availableSlotsCount += maxOf(0, capacity - currentBookings)
                            }
                        }
                    }

                    if (availableSlotsCount > 0) {
                        // Get hospital info from Firestore
                        firestore.collection("hospitals").document(hospitalId)
                            .get()
                            .addOnSuccessListener { hospitalDoc ->
                                if (!hospitalDoc.exists()) {
                                    Log.w("DonateFragment", "Hospital document not found: $hospitalId")
                                    hospitalProcessed()
                                    return@addOnSuccessListener
                                }
                                
                                val hospitalName = hospitalDoc.getString("name") ?: "Unknown Hospital"
                                Log.d("DonateFragment", "Found hospital: $hospitalName")

                                // Get address as Map since it's not a String
                                val addressMap =
                                    hospitalDoc.get("address") as? Map<String, Any?> ?: emptyMap()

                                // Try multiple possible address field names (but NOT as String since address is a Map)
                                val addressFromDoc = hospitalDoc.getString("location")
                                    ?: hospitalDoc.getString("fullAddress")
                                    ?: hospitalDoc.getString("registrationNumber") ?: ""

                                val state = addressMap["state"]?.toString()
                                    ?: hospitalDoc.getString("state") ?: ""
                                val city =
                                    addressMap["city"]?.toString() ?: hospitalDoc.getString("city")
                                    ?: ""
                                val street = addressMap["street"]?.toString()
                                    ?: hospitalDoc.getString("street") ?: ""
                                val postalCode = addressMap["postalCode"]?.toString()
                                    ?: hospitalDoc.getString("postalCode") ?: ""

                                // Apply state and district filters
                                val stateMatches = selectedState == null || state == selectedState
                                val districtMatches =
                                    selectedDistrict == null || city == selectedDistrict

                                if (stateMatches && districtMatches) {
                                    Log.d("DonateFragment", "Hospital: $hospitalName")
                                    Log.d("DonateFragment", "Address field: '$addressFromDoc'")
                                    Log.d("DonateFragment", "Address Map: $addressMap")
                                    Log.d(
                                        "DonateFragment",
                                        "State: '$state', City: '$city', Street: '$street'"
                                    )

                                    // Use direct address field if available, otherwise construct from components
                                    val addressSnippet = if (addressFromDoc.isNotBlank()) {
                                        addressFromDoc
                                    } else {
                                        buildString {
                                            if (street.isNotBlank()) append("$street, ")
                                            if (city.isNotBlank()) append("$city, ")
                                            if (state.isNotBlank()) append(state)
                                            if (postalCode.isNotBlank()) {
                                                if (isNotEmpty()) append(" ")
                                                append(postalCode)
                                            }
                                            if (isEmpty()) {
                                                append("Address not available")
                                            }
                                        }
                                    }

                                    Log.d(
                                        "DonateFragment",
                                        "Final addressSnippet: '$addressSnippet'"
                                    )

                                    val hospital = Hospital(
                                        id = hospitalId,
                                        name = hospitalName,
                                        state = state,
                                        district = city,
                                        addressSnippet = addressSnippet,
                                        availableSlotsCount = availableSlotsCount
                                    )
                                    availableHospitals.add(hospital)
                                }

                                hospitalProcessed()
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "DonateFragment",
                                    "Error loading hospital info for $hospitalId",
                                    e
                                )
                                // Still count this hospital as processed so UI can finish
                                hospitalProcessed()
                            }
                    } else {
                        hospitalProcessed()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DonateFragment", "Error loading slots from RTDB", error.toException())
                binding?.progressBar?.visibility = View.GONE
                if (isAdded && context != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading slots: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }


    private fun setupHospitalsRecyclerView() {
        val adapter = HospitalAdapter(availableHospitals) { hospital ->
            selectedHospitalId = hospital.id
            navigateToSlotSelection(hospital.id, hospital.name, hospital.state, hospital.district)
        }

        binding?.rvAvailableSlots?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }

    private fun navigateToSlotSelection(
        hospitalId: String,
        hospitalName: String,
        state: String,
        city: String
    ) {
        try {
            // Use Intent directly since Navigation Component action doesn't exist
            Log.d("DonateFragment", "Navigating to slot selection for hospital: $hospitalName")
            val address = buildString {
                if (city.isNotBlank()) append(city)
                if (state.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(state)
                }
                if (isEmpty()) append("Address not available")
            }
            Log.d("DonateFragment", "Built address for navigation: $address")

            val intent = Intent(requireContext(), AppointmentSchedulingActivity::class.java).apply {
                putExtra("hospitalId", hospitalId)
                putExtra("selectedDate", selectedDate)
                putExtra("hospitalName", hospitalName)
                putExtra("hospitalAddress", address)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("DonateFragment", "Failed to navigate to slot selection", e)
            if (isAdded && context != null) {
                Toast.makeText(requireContext(), "Error opening slot selection", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun bookAppointment() {
        if (donorId == null || selectedHospitalId == null || selectedSlotId == null) {
            if (isAdded && context != null) {
                Toast.makeText(
                    requireContext(),
                    "Please select a date and time slot",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        // Check if donor already has an active appointment using RTDB
        realtimeDb.child("appointments")
            .orderByChild("donorId")
            .equalTo(donorId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasActiveAppointment = false

                    for (appointmentSnapshot in snapshot.children) {
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val status = appointmentData?.get("status") as? String

                        if (status == "pending" || status == "confirmed") {
                            hasActiveAppointment = true
                            break
                        }
                    }

                    if (hasActiveAppointment) {
                        // Donor already has an active appointment
                        if (isAdded && context != null) {
                            Toast.makeText(
                                requireContext(),
                                "You already have an active appointment. Please cancel it first.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        // No active appointment found, proceed with booking
                        proceedWithBooking()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        "DonateFragment",
                        "Error checking for existing appointments",
                        error.toException()
                    )
                    if (isAdded && context != null) {
                        Toast.makeText(
                            requireContext(),
                            "Could not verify existing appointments. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

    private fun proceedWithBooking() {
        // The slot selection logic will be handled in SlotSelectionFragment
        // This function is kept for compatibility but booking logic moved to SlotSelectionFragment
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), "Please select a time slot", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetSelection() {
        selectedDate = null
        selectedHospitalId = null
        selectedSlotId = null
        binding?.tvSelectedDate?.text = "No date selected"
        availableHospitals.clear()
        setupHospitalsRecyclerView()
    }

    private fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    private fun loadCurrentAppointment() {
        if (donorId == null) return

        // Check if donor has any active appointment using RTDB
        realtimeDb.child("appointments")
            .orderByChild("donorId")
            .equalTo(donorId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var activeAppointment: Map<String, Any>? = null

                    for (appointmentSnapshot in snapshot.children) {
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val status = appointmentData?.get("status") as? String

                        if (status == "pending" || status == "confirmed") {
                            activeAppointment = appointmentSnapshot.value as? Map<String, Any>
                            break
                        }
                    }

                    if (activeAppointment != null) {
                        // Appointment display has been moved to AppointmentsFragment
                    } else {
                        // Appointment display has been moved to AppointmentsFragment
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DonateFragment", "Error loading appointments", error.toException())
                    if (isAdded && context != null) {
                        Toast.makeText(
                            requireContext(),
                            "Failed to load appointments: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

    private fun cancelCurrentAppointment() {
        if (donorId == null) return

        // First find the active appointment using RTDB
        realtimeDb.child("appointments")
            .orderByChild("donorId")
            .equalTo(donorId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var appointmentToCancel: Pair<String, Map<String, Any>>? = null

                    for (appointmentSnapshot in snapshot.children) {
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val status = appointmentData?.get("status") as? String

                        if (status == "pending" || status == "confirmed") {
                            appointmentToCancel = Pair(
                                appointmentSnapshot.key!!,
                                appointmentSnapshot.value as Map<String, Any>
                            )
                            break
                        }
                    }

                    if (appointmentToCancel != null) {
                        val (appointmentId, appointmentData) = appointmentToCancel

                        // Update appointment status to cancelled
                        val updates = mapOf(
                            "status" to "cancelled",
                            "updatedAt" to System.currentTimeMillis()
                        )

                        realtimeDb.child("appointments").child(appointmentId)
                            .updateChildren(updates)
                            .addOnSuccessListener {
                                if (isAdded && context != null) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Appointment cancelled successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Appointment display has been moved to AppointmentsFragment
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("DonateFragment", "Error cancelling appointment", e)
                                if (isAdded && context != null) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to cancel appointment: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        if (isAdded && context != null) {
                            Toast.makeText(
                                requireContext(),
                                "No active appointment found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        "DonateFragment",
                        "Error finding appointment to cancel",
                        error.toException()
                    )
                    if (isAdded && context != null) {
                        Toast.makeText(
                            requireContext(),
                            "Failed to find appointment: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

    private fun updateEligibilityUI(isEligible: Boolean, message: String) {
        if (!isAdded || context == null) return
        // Show eligibility status as a toast
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun getDaysSinceLastDonation(lastDonation: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val lastDonationDate = dateFormat.parse(lastDonation)
            val currentDate = Date()
            val diffInMillis = currentDate.time - lastDonationDate.time
            (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun loadStateDistrictFilters() {
        availableStates.clear()
        availableDistricts.clear()
        stateDistrictsMap.clear()

        // Load all hospitals to get unique states and districts
        firestore.collection("hospitals")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { querySnapshot ->
                // First pass: Collect all states and initialize their district lists
                for (document in querySnapshot.documents) {
                    val addressMap = document.get("address") as? Map<String, Any?> ?: emptyMap()
                    val state = addressMap["state"]?.toString() ?: document.getString("state") ?: ""
                    
                    if (state.isNotBlank() && !stateDistrictsMap.containsKey(state)) {
                        stateDistrictsMap[state] = mutableListOf()
                    }
                }

                // Second pass: Add districts to their respective states
                for (document in querySnapshot.documents) {
                    val addressMap = document.get("address") as? Map<String, Any?> ?: emptyMap()
                    val state = addressMap["state"]?.toString() ?: document.getString("state") ?: ""
                    val city = addressMap["city"]?.toString() ?: document.getString("city") ?: ""

                    if (state.isNotBlank() && city.isNotBlank()) {
                        val districts = stateDistrictsMap[state] ?: mutableListOf()
                        if (!districts.contains(city)) {
                            districts.add(city)
                            stateDistrictsMap[state] = districts
                        }
                    }
                }

                // Sort states and their districts
                availableStates.addAll(stateDistrictsMap.keys.sorted())
                
                // Sort districts within each state
                stateDistrictsMap.forEach { (_, districts) ->
                    districts.sort()
                }

                // Setup dropdowns after loading data (check if fragment is still attached)
                if (isAdded && context != null) {
                    setupStateDistrictDropdowns()
                }
            }
            .addOnFailureListener { e ->
                Log.e("DonateFragment", "Error loading filter options", e)
            }
    }

    private fun setupStateDistrictDropdowns() {
        // Check if fragment is attached and has context
        if (!isAdded || context == null) {
            Log.w("DonateFragment", "Fragment not attached, skipping dropdown setup")
            return
        }

        // State dropdown with custom layout
        val stateAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            availableStates
        )
        binding?.actvState?.setAdapter(stateAdapter)

        // District dropdown with custom layout - initially empty
        val districtAdapter = ArrayAdapter<String>(
            requireContext(),
            R.layout.dropdown_item,
            mutableListOf()
        )
        binding?.actvDistrict?.setAdapter(districtAdapter)
        
        // Set dropdown background and text colors programmatically
        binding?.actvState?.setDropDownBackgroundResource(android.R.color.white)
        binding?.actvDistrict?.setDropDownBackgroundResource(android.R.color.white)
        
        // Set text color for dropdown items
        stateAdapter.setDropDownViewResource(R.layout.dropdown_item)
        districtAdapter.setDropDownViewResource(R.layout.dropdown_item)

        // State selection listener
        binding?.actvState?.setOnItemClickListener { _, _, position, _ ->
            selectedState = availableStates.getOrNull(position)
            selectedDistrict = null
            
            // Update districts based on selected state
            val districts = if (selectedState != null) {
                stateDistrictsMap[selectedState!!]?.toMutableList() ?: mutableListOf()
            } else {
                mutableListOf()
            }
            
            // Update district adapter
            districtAdapter.clear()
            districtAdapter.addAll(districts)
            binding?.actvDistrict?.setText("", false) // Clear the selected district
            
            reloadHospitalsIfDateSelected()
        }

        // District selection listener
        binding?.actvDistrict?.setOnItemClickListener { _, _, position, _ ->
            val districts = if (selectedState != null) {
                stateDistrictsMap[selectedState!!] ?: emptyList()
            } else {
                emptyList()
            }
            selectedDistrict = districts.getOrNull(position)
            reloadHospitalsIfDateSelected()
        }
    }

    private fun reloadHospitalsIfDateSelected() {
        if (selectedDate != null && isAdded && context != null) {
            loadAvailableSlots()
        }
    }
}



// Data class for slots (kept for compatibility with existing booking logic)
// data class Slot(
//     var slotId: String = "",
//     var hospitalId: String = "",
//     var hospitalName: String = "",
//     var hospitalAddress: String = "",
//     val timestamp: Long = 0,
//     val startTime: Long = 0,
//     val endTime: Long = 0,
//     val capacity: Int = 0,
//     val available: Int = 0,
//     val date: String = "",
//     val createdAt: Long = System.currentTimeMillis()
// )

// SlotAdapter class (kept for compatibility)
// class SlotAdapter(
//     private val slots: List<Slot>,
//     private val onSlotClick: (Slot) -> Unit
// ) : androidx.recyclerview.widget.RecyclerView.Adapter<SlotAdapter.SlotViewHolder>() {

//     class SlotViewHolder(val binding: ItemSlotBinding) :
//         RecyclerView.ViewHolder(binding.root)



//     override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SlotViewHolder {
//         val binding = ItemSlotBinding.inflate(
//             LayoutInflater.from(parent.context), parent, false
//         )
//         return SlotViewHolder(binding)
//     }

//     override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
//         val slot = slots[position]
//         holder.binding?.apply {
//             tvHospitalName.text = slot.hospitalName
//             tvHospitalAddress.text = slot.hospitalAddress
//             tvTimeSlot.text = "${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}"
//             tvAvailable.text = "${slot.available}/${slot.capacity} available"
            
//             root.setOnClickListener {
//                 onSlotClick(slot)
//             }
//         }
//     }

//     override fun getItemCount() = slots.size

//     private fun formatTime(timestamp: Long): String {
//         val date = java.util.Date(timestamp)
//         val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
//         return format.format(date)
//     }
// }
