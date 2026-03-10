package com.example.blood_bud.ui.donor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Appointment
import com.example.blood_bud.databinding.FragmentAppointmentsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.LinearLayoutManager

class AppointmentsFragment : Fragment() {
    private var _binding: FragmentAppointmentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private var appointmentListener: ValueEventListener? = null
    private var historyListener: ValueEventListener? = null
    private val historyList = mutableListOf<Appointment>()
    private var historyAdapter: DonorAppointmentHistoryAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        firestore = FirebaseFirestore.getInstance()
        
        setupRecyclerView()
        setupClickListeners()
        loadCurrentAppointment()
        loadAppointmentHistory()
    }
    
    private fun setupClickListeners() {
        binding.btnNewAppointment.setOnClickListener {
            // Navigate to DonateFragment to book a new appointment
            val intent = Intent(requireContext(), DonateFragment::class.java)
            startActivity(intent)
        }
        
        binding.btnCancelAppointment.setOnClickListener {
            cancelCurrentAppointment()
        }
    }
    
    private fun loadCurrentAppointment() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("DonorAppointments", "No authenticated user")
            hideAppointmentUI()
            return
        }

        Log.d("DonorAppointments", "Loading appointments for donor: ${currentUser.uid}")

        // Show loading state
        binding.progressBar.visibility = View.VISIBLE

        // Clear any existing listener
        appointmentListener?.let {
            database.removeEventListener(it)
        }

        appointmentListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if view is still available
                if (_binding == null) {
                    Log.w("DonorAppointments", "View destroyed, skipping data update")
                    return
                }
                
                binding.progressBar.visibility = View.GONE
                Log.d("DonorAppointments", "Received ${snapshot.childrenCount} appointments")

                if (!snapshot.exists()) {
                    Log.d("DonorAppointments", "No appointments found in appointments collection")
                    // Try appointments_active as fallback
                    loadFromAppointmentsActive(currentUser.uid)
                    return
                }

                // Find the first active appointment
                for (appointmentSnapshot in snapshot.children) {
                    try {
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val status = appointmentData?.get("status") as? String ?: ""
                        val appointmentId = appointmentSnapshot.key ?: ""

                        Log.d("DonorAppointments", "Checking appointment $appointmentId with status: $status")

                        // Only show active appointments (not cancelled or completed)
                        if (status.equals("pending", ignoreCase = true) ||
                            status.equals("confirmed", ignoreCase = true) ||
                            status.equals("booked", ignoreCase = true)) {

                            val appointment = Appointment(
                                id = appointmentId,
                                appointmentId = appointmentId,
                                donorId = appointmentData?.get("donorId") as? String ?: "",
                                donorName = appointmentData?.get("donorName") as? String ?: "",
                                hospitalId = appointmentData?.get("hospitalId") as? String ?: "",
                                hospitalName = appointmentData?.get("hospitalName") as? String ?: "",
                                appointmentDate = appointmentData?.get("appointmentDate") as? String ?: "",
                                appointmentTime = appointmentData?.get("appointmentTime") as? String ?: "",
                                status = status,
                                startTime = appointmentData?.get("startTime") as? String ?: "",
                                endTime = appointmentData?.get("endTime") as? String ?: "",
                                createdAt = (appointmentData?.get("createdAt") as? Long) ?: 0,
                                notes = appointmentData?.get("notes") as? String ?: ""
                            )

                            Log.d("DonorAppointments", "Found active appointment: $appointmentId")
                            updateAppointmentUI(appointment)
                            return
                        }
                    } catch (e: Exception) {
                        Log.e("DonorAppointments", "Error parsing appointment", e)
                    }
                }

                // No active appointments found, try appointments_active
                Log.d("DonorAppointments", "No active appointments found, checking appointments_active")
                loadFromAppointmentsActive(currentUser.uid)
            }

            override fun onCancelled(error: DatabaseError) {
                // Check if view is still available
                if (_binding == null) {
                    Log.w("DonorAppointments", "View destroyed, skipping error handling")
                    return
                }
                
                binding.progressBar.visibility = View.GONE
                Log.e("DonorAppointments", "Error loading appointments", error.toException())
                Toast.makeText(context, "Failed to load appointments: ${error.message}", Toast.LENGTH_SHORT).show()
                hideAppointmentUI()
            }
        }

        // Query for active appointments for the current user
        database.child("appointments")
            .orderByChild("donorId")
            .equalTo(currentUser.uid)
            .addValueEventListener(appointmentListener as ValueEventListener)
    }

    private fun loadFromAppointmentsActive(donorId: String) {
        Log.d("DonorAppointments", "Loading from appointments_active collection")

        database.child("appointments_active")
            .orderByChild("donorId")
            .equalTo(donorId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Check if view is still available
                    if (_binding == null) {
                        Log.w("DonorAppointments", "View destroyed, skipping appointments_active update")
                        return
                    }
                    
                    Log.d("DonorAppointments", "Received ${snapshot.childrenCount} appointments from appointments_active")

                    if (!snapshot.exists()) {
                        Log.d("DonorAppointments", "No appointments found in either collection")
                        hideAppointmentUI()
                        return
                    }

                    // Find the first active appointment
                    for (appointmentSnapshot in snapshot.children) {
                        try {
                            val appointmentData = appointmentSnapshot.value as? Map<*, *>
                            val status = appointmentData?.get("status") as? String ?: ""
                            val appointmentId = appointmentSnapshot.key ?: ""

                            Log.d("DonorAppointments", "Checking appointment_active $appointmentId with status: $status")

                            if (status.equals("pending", ignoreCase = true) ||
                                status.equals("confirmed", ignoreCase = true) ||
                                status.equals("booked", ignoreCase = true)) {

                                val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                                appointment?.let {
                                    Log.d("DonorAppointments", "Found active appointment in appointments_active: $appointmentId")
                                    updateAppointmentUI(it)
                                    return
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("DonorAppointments", "Error parsing appointment_active", e)
                        }
                    }

                    Log.d("DonorAppointments", "No active appointments found in appointments_active either")
                    hideAppointmentUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Check if view is still available
                    if (_binding == null) {
                        Log.w("DonorAppointments", "View destroyed, skipping appointments_active error handling")
                        return
                    }
                    
                    Log.e("DonorAppointments", "Error loading appointments_active", error.toException())
                    hideAppointmentUI()
                }
            })
    }
    
    private fun cancelCurrentAppointment() {
        val currentUser = auth.currentUser ?: return

        // Show confirmation dialog
        // For simplicity, using a toast. Consider using a proper dialog in production.
        Toast.makeText(requireContext(), "Cancelling appointment...", Toast.LENGTH_SHORT).show()

        // Find and cancel the appointment - check both collections
        database.child("appointments")
            .orderByChild("donorId")
            .equalTo(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d("Cancellation", "No appointments found in appointments collection")
                        // Also check appointments_active as fallback
                        checkAppointmentsActive(currentUser.uid)
                        return
                    }

                    Log.d("Cancellation", "Found ${snapshot.childrenCount} appointments in appointments collection")

                    // Get the first appointment (assuming one active appointment per user)
                    for (appointmentSnapshot in snapshot.children) {
                        val appointmentId = appointmentSnapshot.key ?: continue
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val status = appointmentData?.get("status") as? String

                        Log.d("Cancellation", "Checking appointment $appointmentId with status: $status")

                        // Only cancel active appointments (not cancelled or completed)
                        if (status == "pending" || status == "confirmed") {
                            val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                            appointment?.let { appt ->
                                Log.d("Cancellation", "Found active appointment, proceeding with cancellation")
                                // Decrement slot booking count first
                                decrementSlotBookingCount(appt) { success ->
                                    if (success) {
                                        // Now update appointment status and move to history
                                        updateAppointmentStatusAndHistory(appointmentId, appt, "appointments")
                                    } else {
                                        Toast.makeText(requireContext(), "Error releasing slot, but appointment cancelled", Toast.LENGTH_SHORT).show()
                                        updateAppointmentStatusAndHistory(appointmentId, appt, "appointments")
                                    }
                                }
                            }
                            return
                        }
                    }

                    Log.d("Cancellation", "No active appointments found in appointments collection")
                    // Check appointments_active as fallback
                    checkAppointmentsActive(currentUser.uid)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Cancellation", "Error finding appointment to cancel", error.toException())
                    Toast.makeText(requireContext(), "Error cancelling appointment", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun checkAppointmentsActive(donorId: String) {
        Log.d("Cancellation", "Checking appointments_active collection as fallback")
        database.child("appointments_active")
            .orderByChild("donorId")
            .equalTo(donorId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d("Cancellation", "No appointments found in appointments_active collection either")
                        Toast.makeText(requireContext(), "No active appointment found", Toast.LENGTH_SHORT).show()
                        return
                    }

                    Log.d("Cancellation", "Found ${snapshot.childrenCount} appointments in appointments_active collection")

                    for (appointmentSnapshot in snapshot.children) {
                        val appointmentId = appointmentSnapshot.key ?: continue
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val status = appointmentData?.get("status") as? String

                        if (status == "pending" || status == "confirmed") {
                            val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                            appointment?.let { appt ->
                                decrementSlotBookingCount(appt) { success ->
                                    if (success) {
                                        updateAppointmentStatusAndHistory(appointmentId, appt, "appointments_active")
                                    } else {
                                        Toast.makeText(requireContext(), "Error releasing slot, but appointment cancelled", Toast.LENGTH_SHORT).show()
                                        updateAppointmentStatusAndHistory(appointmentId, appt, "appointments_active")
                                    }
                                }
                            }
                            return
                        }
                    }

                    Toast.makeText(requireContext(), "No active appointment found", Toast.LENGTH_SHORT).show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Cancellation", "Error checking appointments_active", error.toException())
                    Toast.makeText(requireContext(), "Error cancelling appointment", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun decrementSlotBookingCount(appointment: Appointment, onComplete: (Boolean) -> Unit) {
        if (appointment.slotId.isBlank() || appointment.appointmentDate.isBlank() || appointment.hospitalId.isBlank()) {
            Log.w("SlotDecrement", "Missing slot information for appointment ${appointment.appointmentId}")
            onComplete(false)
            return
        }

        val slotRef = database.child("appointment_slots")
            .child(appointment.appointmentDate)
            .child(appointment.hospitalId)
            .child(appointment.slotId)
            .child("currentBookings")

        slotRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Int::class.java) ?: 0
                if (current > 0) {
                    currentData.value = current - 1
                    Log.d("SlotDecrement", "Decremented slot bookings from $current to ${current - 1}")
                } else {
                    Log.w("SlotDecrement", "Slot booking count was already 0")
                }
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("SlotDecrement", "Error decrementing slot bookings", error.toException())
                    onComplete(false)
                } else {
                    Log.d("SlotDecrement", "Slot booking count decremented successfully")
                    onComplete(true)
                }
            }
        })
    }

    private fun updateAppointmentStatusAndHistory(appointmentId: String, appointment: Appointment, sourceCollection: String) {
        val updates = hashMapOf<String, Any>(
            "status" to "cancelled",
            "updatedAt" to System.currentTimeMillis()
        )

        Log.d("Cancellation", "Updating appointment $appointmentId in $sourceCollection collection")

        // Update in the source collection
        database.child(sourceCollection)
            .child(appointmentId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Log.d("Cancellation", "Successfully updated appointment status in $sourceCollection")

                // Update hospital collection
                updateHospitalCollectionOnCancel(appointment)

                // Fetch donor name from Firestore before saving to history
                val donorId = appointment.donorId
                if (!donorId.isNullOrBlank()) {
                    firestore.collection("users")
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
                            saveCancelledAppointmentToHistory(appointmentId, appointment, sourceCollection, donorName)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Cancellation", "Failed to fetch donor name, using default", e)
                            saveCancelledAppointmentToHistory(appointmentId, appointment, sourceCollection, "Unknown Donor")
                        }
                } else {
                    saveCancelledAppointmentToHistory(appointmentId, appointment, sourceCollection, "Unknown Donor")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Cancellation", "Error updating appointment in $sourceCollection", e)
                Toast.makeText(requireContext(), "Error cancelling appointment", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveCancelledAppointmentToHistory(appointmentId: String, appointment: Appointment, sourceCollection: String, donorName: String) {
        // Move to history with complete appointment data
        val updatedAppointment = appointment.copy(
            status = "cancelled",
            updatedAt = System.currentTimeMillis()
        )
        
        val historyData = hashMapOf<String, Any>(
            "appointmentId" to appointmentId,
            "donorId" to (updatedAppointment.donorId ?: ""),
            "donorName" to donorName,
            "hospitalId" to (updatedAppointment.hospitalId ?: ""),
            "hospitalName" to (updatedAppointment.hospitalName ?: ""),
            "hospitalAddress" to (updatedAppointment.hospitalAddress ?: ""),
            "slotId" to (updatedAppointment.slotId ?: ""),
            "appointmentDate" to (updatedAppointment.appointmentDate ?: ""),
            "appointmentTime" to (updatedAppointment.appointmentTime ?: ""),
            "startTime" to (updatedAppointment.startTime ?: ""),
            "endTime" to (updatedAppointment.endTime ?: ""),
            "status" to "cancelled",
            "createdAt" to updatedAppointment.createdAt,
            "updatedAt" to updatedAppointment.updatedAt,
            "notes" to (updatedAppointment.notes ?: "")
        )

        database.child("appointments_history")
            .child(appointmentId)
            .setValue(historyData)
            .addOnSuccessListener {
                Log.d("Cancellation", "Successfully moved appointment to history with donor name: $donorName")
                
                // Remove from source collection
                database.child(sourceCollection)
                    .child(appointmentId)
                    .removeValue()
                    .addOnSuccessListener {
                        Log.d("Cancellation", "Successfully removed appointment from $sourceCollection")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Cancellation", "Failed to remove appointment from $sourceCollection", e)
                    }

                // Remove from appointments_active if exists
                database.child("appointments_active")
                    .child(appointmentId)
                    .removeValue()
                    .addOnSuccessListener {
                        Log.d("Cancellation", "Successfully removed appointment from appointments_active")
                    }
                    .addOnFailureListener { e ->
                        Log.d("Cancellation", "Appointment not in appointments_active: ${e.message}")
                    }

                Toast.makeText(requireContext(), "Appointment cancelled", Toast.LENGTH_SHORT).show()
                hideAppointmentUI()
            }
            .addOnFailureListener { e ->
                Log.e("Cancellation", "Error moving to history", e)
                Toast.makeText(requireContext(), "Error cancelling appointment", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateHospitalCollectionOnCancel(appointment: Appointment) {
        val hospitalRef = database.child("hospitals").child(appointment.hospitalId)

        Log.d("HospitalUpdate", "Updating hospital collection on cancel: ${appointment.hospitalId}")

        // Update hospital statistics when appointment is cancelled
        hospitalRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val hospitalData = currentData.value as? Map<*, *> ?: HashMap<String, Any>()

                // Remove appointment from hospital collection
                currentData.child("appointments").child(appointment.appointmentId).value = null

                // Update hospital statistics
                val activeCount = (hospitalData["activeAppointments"] as? Long)?.toInt() ?: 0
                currentData.child("activeAppointments").value = maxOf(0, activeCount - 1)

                val cancelledCount = (hospitalData["cancelledAppointments"] as? Long)?.toInt() ?: 0
                currentData.child("cancelledAppointments").value = cancelledCount + 1

                // Update daily stats
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())

                val dailyStats = currentData.child("dailyStats").child(today)
                val bookedSlots = (dailyStats.child("bookedSlots").getValue(Int::class.java)) ?: 0
                dailyStats.child("bookedSlots").value = maxOf(0, bookedSlots - 1)

                val totalSlots = (dailyStats.child("totalSlots").getValue(Int::class.java)) ?: 0
                dailyStats.child("totalSlots").value = totalSlots
                dailyStats.child("availableSlots").value = maxOf(0, totalSlots - maxOf(0, bookedSlots - 1))

                Log.d("HospitalUpdate", "Updated hospital stats on cancel - Active: ${maxOf(0, activeCount - 1)}, Cancelled: ${cancelledCount + 1}")

                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("HospitalUpdate", "Failed to update hospital collection on cancel", error.toException())
                } else if (committed) {
                    Log.d("HospitalUpdate", "Hospital collection updated successfully on cancel")
                } else {
                    Log.w("HospitalUpdate", "Hospital collection update not committed on cancel")
                }
            }
        })
    }
    
    private fun updateAppointmentUI(appointment: Appointment) {
        // Check if view is still available
        if (_binding == null) {
            Log.w("DonorAppointments", "View destroyed, skipping UI update")
            return
        }
        
        binding.cardCurrentAppointment.visibility = View.VISIBLE
        binding.tvAppointmentDate.text = "Date: ${appointment.appointmentDate}"
        binding.tvAppointmentTime.text = "Time: ${appointment.appointmentTime}"
        binding.tvAppointmentHospital.text = "Hospital: ${appointment.hospitalName}"
        binding.tvAppointmentStatus.text = "Status: ${appointment.status}"
    }
    
    private fun hideAppointmentUI() {
        // Check if view is still available
        if (_binding == null) {
            Log.w("DonorAppointments", "View destroyed, skipping UI hide")
            return
        }
        
        binding.cardCurrentAppointment.visibility = View.GONE
        binding.tvNoAppointments.visibility = View.VISIBLE
    }
    
    private fun formatTime(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private fun setupRecyclerView() {
        historyAdapter = DonorAppointmentHistoryAdapter(requireContext(), firestore)
        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }
    
    private fun loadAppointmentHistory() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("DonorAppointments", "No authenticated user for history")
            return
        }

        Log.d("DonorAppointments", "Loading appointment history for donor: ${currentUser.uid}")

        historyListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) {
                    Log.w("DonorAppointments", "View destroyed, skipping history update")
                    return
                }
                
                Log.d("DonorAppointments", "Received ${snapshot.childrenCount} history appointments")
                
                historyList.clear()
                
                for (appointmentSnapshot in snapshot.children) {
                    try {
                        val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                        if (appointment != null) {
                            appointment.appointmentId = appointmentSnapshot.key ?: ""
                            historyList.add(appointment)
                            Log.d("DonorAppointments", "Added history appointment: ${appointment.appointmentId} - ${appointment.status}")
                        }
                    } catch (e: Exception) {
                        Log.e("DonorAppointments", "Error parsing history appointment", e)
                    }
                }
                
                // Sort by updatedAt or createdAt descending (most recent first)
                historyList.sortByDescending { it.updatedAt }
                
                Log.d("DonorAppointments", "Total history appointments: ${historyList.size}")
                
                historyAdapter?.submitList(historyList.toList())
                
                if (historyList.isEmpty()) {
                    binding.rvAppointments.visibility = View.GONE
                    binding.tvNoAppointments.visibility = View.VISIBLE
                } else {
                    binding.rvAppointments.visibility = View.VISIBLE
                    binding.tvNoAppointments.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null) return
                
                Log.e("DonorAppointments", "Error loading history", error.toException())
                binding.rvAppointments.visibility = View.GONE
                binding.tvNoAppointments.visibility = View.VISIBLE
            }
        }

        database.child("appointments_history")
            .orderByChild("donorId")
            .equalTo(currentUser.uid)
            .addValueEventListener(historyListener as ValueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listeners when the view is destroyed
        appointmentListener?.let {
            database.removeEventListener(it)
        }
        historyListener?.let {
            database.removeEventListener(it)
        }
        _binding = null
    }
}





