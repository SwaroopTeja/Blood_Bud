package com.example.blood_bud.ui.hospital

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Appointment
import com.example.blood_bud.databinding.FragmentHospitalAppointmentsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HospitalAppointmentsFragment : Fragment() {
    private var _binding: FragmentHospitalAppointmentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var appointmentListener: ValueEventListener? = null
    private lateinit var adapter: HospitalAppointmentAdapter
    private val allAppointments = mutableListOf<Appointment>()
    private val displayedAppointments = mutableListOf<Appointment>()
    private var currentFilter: String = "all" // all, pending, completed, cancelled
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    companion object {
        private const val STATUS_PENDING = "pending"
        private const val STATUS_CONFIRMED = "confirmed"
        private const val STATUS_COMPLETED = "completed"
        private const val STATUS_CANCELLED = "cancelled"

        fun newInstance() = HospitalAppointmentsFragment()
    }

    init {
        Log.d("HospitalAppointments", "HospitalAppointmentsFragment created!")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HospitalAppointments", "onCreateView called")
        _binding = FragmentHospitalAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HospitalAppointments", "onViewCreated called")

        auth = FirebaseAuth.getInstance()
        database = Firebase.database

        Log.d("HospitalAppointments", "Current user: ${auth.currentUser?.uid}")
        Log.d("HospitalAppointments", "Is user authenticated: ${auth.currentUser != null}")

        setupRecyclerView()
        loadAppointments()
    }

    private fun setupRecyclerView() {
        adapter = HospitalAppointmentAdapter(
            context = requireContext(),
            onStatusUpdate = { appointment, newStatus ->
                updateAppointmentStatus(appointment, newStatus)
            }
        )
        
        binding.rvAppointments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppointments.adapter = adapter
        
        // Setup confirmation button
        binding.btnConfirmation.setOnClickListener {
            showToast("✅ Confirmed! You're on the Hospital Appointments Page!")
            Log.d("HospitalAppointments", "Confirmation button clicked - this confirms we're on the correct page!")
        }

        // Setup filter buttons
        binding.filterAll.setOnClickListener { setFilter("all") }
        binding.filterPending.setOnClickListener { setFilter("pending") }
        binding.filterCompleted.setOnClickListener { setFilter("completed") }
        binding.filterCancelled.setOnClickListener { setFilter("cancelled") }
    }

    private fun loadAppointments() {
        val hospitalId = auth.currentUser?.uid
        
        Log.d("HospitalAppointments", "========== LOADING APPOINTMENTS ==========")
        Log.d("HospitalAppointments", "Current user: ${auth.currentUser?.email}")
        Log.d("HospitalAppointments", "Hospital ID: $hospitalId")
        
        if (hospitalId == null) {
            Log.e("HospitalAppointments", "❌ No hospital ID - user not logged in!")
            binding.progressBar.visibility = View.GONE
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE

        // Load from appointments_active collection
        loadAppointmentsFromActiveCollection(hospitalId)
    }

    private fun loadAppointmentsFromActiveCollection(hospitalId: String) {
        Log.d("HospitalAppointments", "Loading from appointments_active collection")

        // Query appointments from appointments_active collection
        val appointmentsActiveRef = database.getReference("appointments_active")
            .orderByChild("hospitalId")
            .equalTo(hospitalId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("HospitalAppointments", "Received ${snapshot.childrenCount} appointments from appointments_active")

                if (!snapshot.exists()) {
                    Log.d("HospitalAppointments", "No appointments found in appointments_active")
                    // Fallback to main appointments collections
                    loadFromMainCollections(hospitalId)
                    return
                }

                val loadedAppointments = mutableListOf<Appointment>()

                for (appointmentSnapshot in snapshot.children) {
                    try {
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val appointmentId = appointmentSnapshot.key ?: ""

                        Log.d("HospitalAppointments", "Processing hospital appointment $appointmentId")
                        Log.d("HospitalAppointments", "Raw appointment data: $appointmentData")

                        if (appointmentData != null) {
                            val donorId = appointmentData["donorId"] as? String ?: ""
                            val donorName = appointmentData["donorName"] as? String ?: ""
                            Log.d("HospitalAppointments", "Extracted donorId: '$donorId' (isEmpty: ${donorId.isEmpty()})")
                            Log.d("HospitalAppointments", "Extracted donorName: '$donorName' (isEmpty: ${donorName.isEmpty()})")
                            
                            // Parse appointment data from appointments_active collection
                            // Note: appointments_active uses 'date' and 'time' fields
                            val appointment = Appointment(
                                appointmentId = appointmentId,
                                donorId = donorId,
                                donorName = donorName,
                                hospitalId = appointmentData["hospitalId"] as? String ?: hospitalId,
                                slotId = appointmentData["slotId"] as? String ?: "",
                                appointmentDate = appointmentData["date"] as? String ?: "",
                                appointmentTime = appointmentData["time"] as? String ?: "",
                                status = appointmentData["status"] as? String ?: "pending",
                                createdAt = (appointmentData["bookedAt"] as? Long) ?: System.currentTimeMillis()
                            )

                            loadedAppointments.add(appointment)
                            Log.d("HospitalAppointments", "✅ Parsed appointment ${appointment.appointmentId} - Donor: ${appointment.donorName}")
                        }
                    } catch (e: Exception) {
                        Log.e("HospitalAppointments", "Error parsing hospital appointment data", e)
                    }
                }

                Log.d("HospitalAppointments", "Total appointments loaded from hospital collection: ${loadedAppointments.size}")

                // Update allAppointments and apply filters
                allAppointments.clear()
                allAppointments.addAll(loadedAppointments)
                applyFilter()
                updateUI()
                binding.progressBar.visibility = View.GONE

                // Also listen for updates from main appointments collection as fallback
                if (loadedAppointments.isEmpty()) {
                    loadFromMainCollections(hospitalId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HospitalAppointments", "Error loading appointments from appointments_active", error.toException())
                // Fallback to main appointments collections
                loadFromMainCollections(hospitalId)
            }
        }

        appointmentsActiveRef.addValueEventListener(listener)
    }

    private fun loadFromMainCollections(hospitalId: String) {
        Log.d("HospitalAppointments", "Loading from main appointments collections")

        // Query all appointments for this hospital from appointments collection
        val appointmentsRef = database.getReference("appointments")
            .orderByChild("hospitalId")
            .equalTo(hospitalId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("HospitalAppointments", "Received ${snapshot.childrenCount} appointments from appointments collection")

                if (!snapshot.exists()) {
                    Log.d("HospitalAppointments", "No appointments found in appointments collection")
                    // Try appointments_active as final fallback
                    loadFromAppointmentsActive(hospitalId)
                    return
                }

                val loadedAppointments = mutableListOf<Appointment>()

                for (appointmentSnapshot in snapshot.children) {
                    try {
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val appointmentId = appointmentSnapshot.key ?: ""

                        Log.d("HospitalAppointments", "Processing appointment $appointmentId from appointments collection")

                        if (appointmentData != null) {
                            // Parse appointment data manually
                            val appointment = Appointment(
                                appointmentId = appointmentId,
                                donorId = appointmentData["donorId"] as? String ?: "",
                                donorName = appointmentData["donorName"] as? String ?: "",
                                hospitalId = appointmentData["hospitalId"] as? String ?: hospitalId,
                                slotId = appointmentData["slotId"] as? String ?: "",
                                appointmentDate = appointmentData["appointmentDate"] as? String ?: "",
                                appointmentTime = appointmentData["appointmentTime"] as? String ?: "",
                                status = appointmentData["status"] as? String ?: "pending",
                                createdAt = (appointmentData["createdAt"] as? Long) ?: System.currentTimeMillis()
                            )

                            loadedAppointments.add(appointment)
                            Log.d("HospitalAppointments", "Successfully parsed appointment ${appointment.appointmentId} with donorName: ${appointment.donorName}")
                        }
                    } catch (e: Exception) {
                        Log.e("HospitalAppointments", "Error parsing appointment data", e)
                    }
                }

                Log.d("HospitalAppointments", "Total appointments loaded from appointments collection: ${loadedAppointments.size}")

                // Update allAppointments and apply filters
                allAppointments.clear()
                allAppointments.addAll(loadedAppointments)
                applyFilter()
                updateUI()
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HospitalAppointments", "Error loading appointments from appointments collection", error.toException())
                // Final fallback to appointments_active
                loadFromAppointmentsActive(hospitalId)
            }
        }

        appointmentsRef.addValueEventListener(listener)
    }

    private fun loadFromAppointmentsActive(hospitalId: String) {
        Log.d("HospitalAppointments", "Loading from appointments_active collection")

        // Query all appointments for this hospital from appointments_active collection
        val appointmentsActiveRef = database.getReference("appointments_active")
            .orderByChild("hospitalId")
            .equalTo(hospitalId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("HospitalAppointments", "Received ${snapshot.childrenCount} appointments from appointments_active collection")

                val loadedAppointments = mutableListOf<Appointment>()

                for (appointmentSnapshot in snapshot.children) {
                    try {
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val appointmentId = appointmentSnapshot.key ?: ""

                        Log.d("HospitalAppointments", "Processing appointment $appointmentId from appointments_active collection")

                        if (appointmentData != null) {
                            // Parse appointment data manually
                            val appointment = Appointment(
                                appointmentId = appointmentId,
                                donorId = appointmentData["donorId"] as? String ?: "",
                                hospitalId = appointmentData["hospitalId"] as? String ?: hospitalId,
                                slotId = appointmentData["slotId"] as? String ?: "",
                                appointmentDate = appointmentData["appointmentDate"] as? String ?: "",
                                appointmentTime = appointmentData["appointmentTime"] as? String ?: "",
                                status = appointmentData["status"] as? String ?: "pending",
                                createdAt = (appointmentData["createdAt"] as? Long) ?: System.currentTimeMillis()
                            )

                            loadedAppointments.add(appointment)
                            Log.d("HospitalAppointments", "Successfully parsed appointment ${appointment.appointmentId}")
                        }
                    } catch (e: Exception) {
                        Log.e("HospitalAppointments", "Error parsing appointment data", e)
                    }
                }

                Log.d("HospitalAppointments", "Total appointments loaded from appointments_active collection: ${loadedAppointments.size}")

                // Update allAppointments and apply filters
                allAppointments.clear()
                allAppointments.addAll(loadedAppointments)
                applyFilter()
                updateUI()
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HospitalAppointments", "Error loading appointments from appointments_active collection", error.toException())
                binding.progressBar.visibility = View.GONE
                binding.tvNoAppointments.visibility = View.VISIBLE
            }
        }

        appointmentsActiveRef.addValueEventListener(listener)
    }
    
    private fun setFilter(filter: String) {
        currentFilter = filter
        applyFilter()
        updateFilterUi()
    }
    
    private fun applyFilter() {
        displayedAppointments.clear()
        Log.d("HospitalAppointments", "ApplyFilter - Total appointments before filter: ${allAppointments.size}")
        Log.d("HospitalAppointments", "ApplyFilter - Current filter: $currentFilter")

        displayedAppointments.addAll(allAppointments.filter { appointment ->
            val status = appointment.status?.lowercase() ?: ""
            
            // Exclude completed and cancelled appointments from "all" view
            // Only show them when specifically filtered
            val isActiveAppointment = status != STATUS_COMPLETED && status != STATUS_CANCELLED
            
            Log.d("HospitalAppointments", "ApplyFilter - Appointment ${appointment.appointmentId}: Status ${appointment.status}, IsActive: $isActiveAppointment")

            // Apply filter based on status
            val matchesFilter = when (currentFilter.lowercase()) {
                "all" -> isActiveAppointment // Only show active appointments in "all"
                "completed" -> status == STATUS_COMPLETED
                "cancelled" -> status == STATUS_CANCELLED
                else -> status == currentFilter.lowercase()
            }
            Log.d("HospitalAppointments", "ApplyFilter - Appointment ${appointment.appointmentId}: MatchesFilter: $matchesFilter")

            matchesFilter
        })

        Log.d("HospitalAppointments", "ApplyFilter - Total appointments after filter: ${displayedAppointments.size}")
        updateUI()
    }
    
    private fun updateFilterUi() {
        binding.filterAll.isSelected = currentFilter == "all"
        binding.filterPending.isSelected = currentFilter == "pending"
        binding.filterCompleted.isSelected = currentFilter == "completed"
        binding.filterCancelled.isSelected = currentFilter == "cancelled"
    }
    
    private fun viewAppointmentDetails(appointment: Appointment) {
        // TODO: Implement appointment details view
        // This could show more details and allow editing if needed
    }
    
    private fun updateUI() {
        if (displayedAppointments.isEmpty()) {
            binding.tvNoAppointments.visibility = View.VISIBLE
            binding.rvAppointments.visibility = View.GONE
            
            // Show appropriate message based on filter
            val message = when (currentFilter) {
                "all" -> getString(R.string.no_appointments_found)
                "pending" -> getString(R.string.no_pending_appointments)
                "confirmed" -> getString(R.string.no_confirmed_appointments)
                "completed" -> getString(R.string.no_completed_appointments)
                "cancelled" -> getString(R.string.no_cancelled_appointments)
                else -> getString(R.string.no_appointments_found)
            }
            binding.tvNoAppointments.text = message
        } else {
            binding.tvNoAppointments.visibility = View.GONE
            binding.rvAppointments.visibility = View.VISIBLE
            
            // Use DiffUtil for efficient updates
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                val oldList = adapter.currentList
                
                override fun getOldListSize() = oldList.size
                override fun getNewListSize() = displayedAppointments.size
                
                override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                    return oldList[oldPos].appointmentId == displayedAppointments[newPos].appointmentId
                }
                
                override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                    return oldList[oldPos] == displayedAppointments[newPos]
                }
                
                override fun getChangePayload(oldPos: Int, newPos: Int): Any? {
                    return if (oldList[oldPos].status != displayedAppointments[newPos].status) "status" else null
                }
            })
            
            adapter.submitList(displayedAppointments.toList(), {
                diffResult.dispatchUpdatesTo(adapter)
            })
        }
    }

    private fun updateAppointmentStatus(appointment: Appointment, newStatus: String) {
        val appointmentId = appointment.appointmentId ?: return
        val appointmentRef = database.getReference("appointments").child(appointmentId)

        // Optimistic UI update
        val previousStatus = appointment.status
        val position = allAppointments.indexOfFirst { it.appointmentId == appointmentId }
        if (position != -1) {
            allAppointments[position] = allAppointments[position].copy(status = newStatus)
            applyFilter()
        }

        // Update the appointment status
        val updates = hashMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        // Handle slot booking count changes
        when (newStatus) {
            STATUS_COMPLETED -> {
                // Appointment completed - slot becomes available
                if (appointment.slotId != null) {
                    updateSlotBookingCount(appointment, increment = false)
                }
                // Update hospital collection
                updateHospitalCollectionOnStatusChange(appointment, newStatus, previousStatus)
            }
            STATUS_CANCELLED -> {
                // Appointment cancelled - slot becomes available
                if (previousStatus == STATUS_CONFIRMED && appointment.slotId != null) {
                    updateSlotBookingCount(appointment, increment = false)
                }
                // Update hospital collection
                updateHospitalCollectionOnStatusChange(appointment, newStatus, previousStatus)
            }
            STATUS_CONFIRMED -> {
                // Appointment confirmed - slot was already booked, no change needed
                if (appointment.slotId != null) {
                    // Slot is already booked, just confirm the appointment
                }
                // Update hospital collection
                updateHospitalCollectionOnStatusChange(appointment, newStatus, previousStatus)
            }
        }

        // Update appointment status
        val transactionHandler = object : DatabaseReference.CompletionListener {
            override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
                if (databaseError != null) {
                    // Revert UI on error
                    if (position != -1) {
                        allAppointments[position] = allAppointments[position].copy(status = previousStatus)
                        applyFilter()
                    }
                    showToast("Failed to update appointment: ${databaseError.message}")
                    Log.e("AppointmentUpdate", "Error updating appointment", databaseError.toException())
                } else {
                    Log.d("AppointmentUpdate", "Appointment $appointmentId status updated to $newStatus")
                    showToast("Appointment ${newStatus.lowercase().capitalize()}")
                    
                    // Move to history if completed or cancelled
                    if (newStatus == STATUS_COMPLETED || newStatus == STATUS_CANCELLED) {
                        moveAppointmentToHistory(appointment.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
                    }
                }
            }
        }

        // Run the update
        appointmentRef.updateChildren(updates, transactionHandler)
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun updateHospitalCollectionOnStatusChange(appointment: Appointment, newStatus: String, previousStatus: String) {
        val hospitalRef = database.getReference("hospitals").child(appointment.hospitalId ?: return)

        Log.d("HospitalUpdate", "Updating hospital collection on status change: ${appointment.hospitalId} from $previousStatus to $newStatus")

        // Update hospital statistics based on status change
        hospitalRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val hospitalData = currentData.value as? Map<*, *> ?: HashMap<String, Any>()

                // Update appointment in hospital collection
                val appointmentPath = "appointments/${appointment.appointmentId}"
                val appointmentData = currentData.child(appointmentPath)
                appointmentData.child("status").value = newStatus
                appointmentData.child("updatedAt").value = System.currentTimeMillis()

                // Update hospital statistics
                val activeCount = (hospitalData["activeAppointments"] as? Long)?.toInt() ?: 0
                val completedCount = (hospitalData["completedAppointments"] as? Long)?.toInt() ?: 0
                val cancelledCount = (hospitalData["cancelledAppointments"] as? Long)?.toInt() ?: 0

                when (newStatus) {
                    STATUS_COMPLETED -> {
                        if (previousStatus != STATUS_COMPLETED) {
                            currentData.child("activeAppointments").value = maxOf(0, activeCount - 1)
                            currentData.child("completedAppointments").value = completedCount + 1
                        }
                    }
                    STATUS_CANCELLED -> {
                        if (previousStatus != STATUS_CANCELLED) {
                            currentData.child("activeAppointments").value = maxOf(0, activeCount - 1)
                            currentData.child("cancelledAppointments").value = cancelledCount + 1
                        }
                    }
                    STATUS_CONFIRMED -> {
                        // No count changes needed for confirmation
                    }
                }

                // Update daily stats
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())

                val dailyStats = currentData.child("dailyStats").child(today)
                val bookedSlots = (dailyStats.child("bookedSlots").getValue(Int::class.java)) ?: 0
                val totalSlots = (dailyStats.child("totalSlots").getValue(Int::class.java)) ?: 0

                when (newStatus) {
                    STATUS_COMPLETED, STATUS_CANCELLED -> {
                        if (previousStatus == STATUS_PENDING || previousStatus == STATUS_CONFIRMED) {
                            dailyStats.child("bookedSlots").value = maxOf(0, bookedSlots - 1)
                            dailyStats.child("availableSlots").value = maxOf(0, totalSlots - maxOf(0, bookedSlots - 1))
                        }
                    }
                }

                dailyStats.child("totalSlots").value = totalSlots

                Log.d("HospitalUpdate", "Updated hospital stats - Active: ${currentData.child("activeAppointments").getValue(Int::class.java) ?: 0}, " +
                      "Completed: ${currentData.child("completedAppointments").getValue(Int::class.java) ?: 0}, " +
                      "Cancelled: ${currentData.child("cancelledAppointments").getValue(Int::class.java) ?: 0}")

                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("HospitalUpdate", "Failed to update hospital collection on status change", error.toException())
                } else if (committed) {
                    Log.d("HospitalUpdate", "Hospital collection updated successfully on status change")
                } else {
                    Log.w("HospitalUpdate", "Hospital collection update not committed on status change")
                }
            }
        })
    }

    private fun updateSlotBookingCount(appointment: Appointment, increment: Boolean) {
        val hospitalId = appointment.hospitalId ?: return
        val slotId = appointment.slotId ?: return
        val appointmentDate = appointment.appointmentDate ?: return

        val slotRef = database.getReference("appointment_slots")
            .child(appointmentDate)
            .child(hospitalId)
            .child(slotId)
            .child("currentBookings")

        Log.d("SlotUpdate", "Updating slot booking count: ${appointmentDate}/${hospitalId}/${slotId}, increment: $increment")

        // Use transaction to handle concurrent updates
        slotRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Int::class.java) ?: 0
                val newValue = if (increment) current + 1 else maxOf(0, current - 1)
                currentData.value = newValue

                Log.d("SlotUpdate", "Slot booking count updated from $current to $newValue")
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("SlotUpdate", "Error updating slot bookings", error.toException())
                } else if (committed) {
                    Log.d("SlotUpdate", "Slot booking count updated successfully")
                } else {
                    Log.w("SlotUpdate", "Slot booking count update not committed")
                }
            }
        })
    }

    private fun moveAppointmentToHistory(appointment: Appointment) {
        val appointmentId = appointment.appointmentId
        if (appointmentId.isNullOrBlank()) {
            Log.e("MoveToHistory", "Cannot move appointment to history: appointmentId is null or blank")
            return
        }

        Log.d("MoveToHistory", "Moving appointment $appointmentId to history with status ${appointment.status}")

        // Fetch donor name from Firestore before saving to history
        val donorId = appointment.donorId
        if (!donorId.isNullOrBlank()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(donorId)
                .get()
                .addOnSuccessListener { document ->
                    val donorName = if (document.exists()) {
                        document.getString("name") 
                            ?: document.getString("fullName")
                            ?: document.getString("userName")
                            ?: "Unknown Donor"
                    } else {
                        "Unknown Donor"
                    }
                    
                    // Now save to history with donor name
                    saveAppointmentToHistory(appointment, appointmentId, donorName)
                }
                .addOnFailureListener { e ->
                    Log.e("MoveToHistory", "Failed to fetch donor name, using default", e)
                    saveAppointmentToHistory(appointment, appointmentId, "Unknown Donor")
                }
        } else {
            saveAppointmentToHistory(appointment, appointmentId, "Unknown Donor")
        }
    }

    private fun saveAppointmentToHistory(appointment: Appointment, appointmentId: String, donorName: String) {
        // Create appointment data for history
        val historyData = hashMapOf<String, Any>(
            "appointmentId" to appointmentId,
            "donorId" to (appointment.donorId ?: ""),
            "donorName" to donorName,
            "hospitalId" to (appointment.hospitalId ?: ""),
            "hospitalName" to (appointment.hospitalName ?: ""),
            "hospitalAddress" to (appointment.hospitalAddress ?: ""),
            "slotId" to (appointment.slotId ?: ""),
            "appointmentDate" to (appointment.appointmentDate ?: ""),
            "appointmentTime" to (appointment.appointmentTime ?: ""),
            "startTime" to (appointment.startTime ?: ""),
            "endTime" to (appointment.endTime ?: ""),
            "status" to appointment.status,
            "createdAt" to appointment.createdAt,
            "updatedAt" to appointment.updatedAt,
            "notes" to (appointment.notes ?: "")
        )

        // Save to appointments_history
        database.getReference("appointments_history")
            .child(appointmentId)
            .setValue(historyData)
            .addOnSuccessListener {
                Log.d("MoveToHistory", "Successfully moved appointment $appointmentId to history with donor name: $donorName")
                
                // Remove from appointments collection
                database.getReference("appointments")
                    .child(appointmentId)
                    .removeValue()
                    .addOnSuccessListener {
                        Log.d("MoveToHistory", "Successfully removed appointment $appointmentId from appointments collection")
                    }
                    .addOnFailureListener { e ->
                        Log.e("MoveToHistory", "Failed to remove appointment from appointments collection", e)
                    }

                // Remove from hospital's appointments subcollection
                appointment.hospitalId?.let { hospitalId ->
                    database.getReference("hospitals")
                        .child(hospitalId)
                        .child("appointments")
                        .child(appointmentId)
                        .removeValue()
                        .addOnSuccessListener {
                            Log.d("MoveToHistory", "Successfully removed appointment from hospital collection")
                        }
                        .addOnFailureListener { e ->
                            Log.e("MoveToHistory", "Failed to remove appointment from hospital collection", e)
                        }
                }

                // Remove from appointments_active if exists
                database.getReference("appointments_active")
                    .child(appointmentId)
                    .removeValue()
                    .addOnSuccessListener {
                        Log.d("MoveToHistory", "Successfully removed appointment from appointments_active")
                    }
                    .addOnFailureListener { e ->
                        Log.d("MoveToHistory", "Appointment not in appointments_active or failed to remove: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MoveToHistory", "Failed to move appointment to history", e)
                showToast("Failed to archive appointment")
            }
    }
}
