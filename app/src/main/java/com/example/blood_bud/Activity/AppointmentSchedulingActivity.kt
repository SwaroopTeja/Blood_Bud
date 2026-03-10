package com.example.blood_bud.Activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ActivityAppointmentSchedulingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AppointmentSchedulingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppointmentSchedulingBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var selectedSlot: Slot? = null

    private var selectedDate: String? = null
    private var selectedHospitalId: String? = null
    private var selectedSlotId: String? = null
    private var hospitalName: String? = null
    private var hospitalAddress: String? = null
    private val availableSlots = mutableListOf<Slot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentSchedulingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Get hospital information from intent
        getHospitalInfoFromIntent()

        setupToolbar()
        setupUI()
        loadAvailableSlots()
    }

    private fun getHospitalInfoFromIntent() {
        selectedHospitalId = intent.getStringExtra("hospitalId")
        selectedDate = intent.getStringExtra("selectedDate")
        hospitalName = intent.getStringExtra("hospitalName")
        hospitalAddress = intent.getStringExtra("hospitalAddress")

        Log.d("AppointmentScheduling", "Hospital ID: $selectedHospitalId, Date: $selectedDate, Name: $hospitalName")
        Log.d("AppointmentScheduling", "Hospital Address: $hospitalAddress")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Book Appointment"
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupUI() {
        // Display hospital information
        binding.tvHospitalName.text = hospitalName ?: "Unknown Hospital"
        binding.tvHospitalAddress.text = hospitalAddress ?: "Address not available"

        binding.btnBookAppointment.setOnClickListener {
            bookAppointment()
        }
    }

    private fun loadAvailableSlots() {
        if (selectedDate == null || selectedHospitalId == null) {
            Toast.makeText(this, "Missing date or hospital information", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        availableSlots.clear()

        // Load slots for specific hospital and date
        val slotsRef = database.child("appointment_slots").child(selectedDate!!).child(selectedHospitalId!!)

        slotsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(slotsSnapshot: DataSnapshot) {
                availableSlots.clear()

                if (!slotsSnapshot.exists() || slotsSnapshot.childrenCount == 0L) {
                    binding.progressBar.visibility = android.view.View.GONE
                    setupSlotsRecyclerView()
                    Toast.makeText(this@AppointmentSchedulingActivity, "No slots available for the selected date", Toast.LENGTH_SHORT).show()
                    return
                }

                // Process slots for this hospital
                val currentTime = System.currentTimeMillis()
                
                slotsSnapshot.children.forEach { slotSnapshot ->
                    val slotData = slotSnapshot.value as? Map<*, *>
                    slotData?.let {
                        val currentBookings = (it["currentBookings"] as? Long)?.toInt() ?: 0
                        val capacity = (it["capacity"] as? Long)?.toInt() ?: 0
                        val endTime = (it["endTime"] as? Long) ?: 0
                        
                        // Only show slots that haven't passed and have availability
                        if (currentBookings < capacity && endTime > currentTime) {
                            val slot = Slot(
                                slotId = slotSnapshot.key ?: "",
                                hospitalId = selectedHospitalId!!,
                                hospitalName = hospitalName ?: "",
                                hospitalAddress = hospitalAddress ?: "",
                                startTime = (it["startTime"] as? Long) ?: 0,
                                endTime = endTime,
                                capacity = capacity,
                                available = capacity - currentBookings,
                                date = selectedDate!!,
                                createdAt = (it["createdAt"] as? Long) ?: 0
                            )
                            availableSlots.add(slot)
                        }
                    }
                }

                setupSlotsRecyclerView()
                binding.progressBar.visibility = android.view.View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AppointmentScheduling", "Error loading slots from RTDB", error.toException())
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@AppointmentSchedulingActivity, "Error loading slots: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSlotsRecyclerView() {
        val adapter = SlotAdapter(availableSlots) { slot ->
            selectedSlot = slot
            selectedSlotId = slot.slotId
            updateSelectedSlotUI(slot)
        }

        binding.rvAvailableSlots.layoutManager = LinearLayoutManager(this)
        binding.rvAvailableSlots.adapter = adapter
    }

    private fun updateSelectedSlotUI(slot: Slot) {
        binding.selectedSlotContainer.visibility = android.view.View.VISIBLE
        
        // Format start and end times
        val startTime = formatTime(slot.startTime)
        val endTime = formatTime(slot.endTime)
        
        binding.tvSelectedSlot.text = "Selected: $startTime - $endTime"
    }
    
    private fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return format.format(date)
    }

    private fun bookAppointment() {
        if (selectedSlot == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show()
            return
        }

        val donorId = auth.currentUser?.uid ?: return

        Log.d("AppointmentBooking", "Starting appointment booking process")
        Log.d("AppointmentBooking", "Donor ID: $donorId")
        Log.d("AppointmentBooking", "Hospital ID: $selectedHospitalId")
        Log.d("AppointmentBooking", "Slot ID: $selectedSlotId")
        Log.d("AppointmentBooking", "Date: $selectedDate")

        // Check if donor already has an active appointment
        database.child("appointments").orderByChild("donorId").equalTo(donorId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("AppointmentBooking", "Checking existing appointments for donor: ${snapshot.childrenCount} found")

                    var hasActiveAppointment = false

                    for (appointmentSnapshot in snapshot.children) {
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val status = appointmentData?.get("status") as? String

                        if (status == "pending" || status == "confirmed") {
                            hasActiveAppointment = true
                            Log.d("AppointmentBooking", "Found active appointment: ${appointmentSnapshot.key}, status: $status")
                            break
                        }
                    }

                    if (hasActiveAppointment) {
                        Log.d("AppointmentBooking", "Donor already has active appointment - blocking booking")
                        Toast.makeText(this@AppointmentSchedulingActivity, "You already have an active appointment", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("AppointmentBooking", "No active appointments found - checking 52-day eligibility")
                        checkDonationEligibility(donorId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppointmentBooking", "Error checking existing appointments", error.toException())
                    Toast.makeText(this@AppointmentSchedulingActivity, "Could not verify existing appointments. Proceeding with booking...", Toast.LENGTH_SHORT).show()
                    checkDonationEligibility(donorId)
                }
            })
    }

    private fun createAppointment(donorId: String) {
        Log.d("AppointmentBooking", "Creating appointment for donor: $donorId")

        val appointmentId = database.child("appointments").push().key
        if (appointmentId == null) {
            Log.e("AppointmentBooking", "Failed to generate appointment ID")
            Toast.makeText(this, "Failed to generate appointment ID", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("AppointmentBooking", "Generated appointment ID: $appointmentId")

        // Get donor name from Firestore
        FirebaseFirestore.getInstance().collection("users").document(donorId)
            .get()
            .addOnSuccessListener { document ->
                val donorName = if (document.exists()) {
                    val user = document.toObject(com.example.blood_bud.data.model.User::class.java)
                    user?.name ?: document.getString("name") ?: "Unknown Donor"
                } else {
                    "Unknown Donor"
                }
                
                Log.d("AppointmentBooking", "Donor name: $donorName")
                
                val appointment = Appointment(
                    appointmentId = appointmentId,
                    donorId = donorId,
                    donorName = donorName,
                    hospitalId = selectedHospitalId!!,
                    slotId = selectedSlotId!!,
                    appointmentDate = selectedDate!!,
                    appointmentTime = formatTime(getSelectedSlot()?.startTime ?: 0),
                    status = "pending"
                )

                Log.d("AppointmentBooking", "Created appointment object with donor name: $donorName")

                // First update the slot booking count
                updateSlotBookingCount(appointment) { slotUpdated ->
                    if (slotUpdated) {
                        Log.d("AppointmentBooking", "Slot updated successfully, creating appointment record")
                        createAppointmentRecord(appointment)
                    } else {
                        Log.e("AppointmentBooking", "Failed to update slot booking count")
                        Toast.makeText(this, "Failed to update slot availability", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("AppointmentBooking", "Failed to get donor name", e)
                // Create appointment without name as fallback
                val appointment = Appointment(
                    appointmentId = appointmentId,
                    donorId = donorId,
                    donorName = "",
                    hospitalId = selectedHospitalId!!,
                    slotId = selectedSlotId!!,
                    appointmentDate = selectedDate!!,
                    appointmentTime = formatTime(getSelectedSlot()?.startTime ?: 0),
                    status = "pending"
                )

                updateSlotBookingCount(appointment) { slotUpdated ->
                    if (slotUpdated) {
                        createAppointmentRecord(appointment)
                    } else {
                        Toast.makeText(this, "Failed to update slot availability", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun updateSlotBookingCount(appointment: Appointment, onComplete: (Boolean) -> Unit) {
        val slotRef = database.child("appointment_slots")
            .child(appointment.appointmentDate)
            .child(appointment.hospitalId)
            .child(appointment.slotId)

        Log.d("AppointmentBooking", "Updating slot: ${appointment.appointmentDate}/${appointment.hospitalId}/${appointment.slotId}")

        // Run transaction on the entire slot to access both currentBookings and capacity
        slotRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val slotData = currentData.value as? Map<*, *>
                if (slotData == null) {
                    Log.e("AppointmentBooking", "Slot data is null")
                    throw Exception("Slot data not found")
                }

                val current = (slotData["currentBookings"] as? Long)?.toInt() ?: 0
                val capacity = (slotData["capacity"] as? Long)?.toInt() ?: 10

                Log.d("AppointmentBooking", "Current slot bookings: $current")
                Log.d("AppointmentBooking", "Slot capacity: $capacity")

                if (current >= capacity) {
                    Log.e("AppointmentBooking", "Slot is fully booked")
                    throw Exception("Slot is fully booked")
                }

                // Update the slot data with incremented booking count
                val updatedSlotData = slotData.toMutableMap()
                updatedSlotData["currentBookings"] = current + 1
                currentData.value = updatedSlotData

                Log.d("AppointmentBooking", "Incremented slot bookings to: ${current + 1}")

                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("AppointmentBooking", "Slot transaction failed", error.toException())
                    onComplete(false)
                } else if (committed) {
                    Log.d("AppointmentBooking", "Slot transaction committed successfully")
                    onComplete(true)
                } else {
                    Log.e("AppointmentBooking", "Slot transaction not committed")
                    onComplete(false)
                }
            }
        })
    }

    private fun createAppointmentRecord(appointment: Appointment) {
        Log.d("AppointmentBooking", "Creating appointment record: ${appointment.appointmentId}")

        // First create the appointment record
        database.child("appointments").child(appointment.appointmentId).setValue(appointment)
            .addOnSuccessListener {
                Log.d("AppointmentBooking", "Appointment record created successfully")

                // Now update the hospital collection
                updateHospitalCollection(appointment)

                Toast.makeText(this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { error ->
                Log.e("AppointmentBooking", "Failed to create appointment record", error)
                Toast.makeText(this, "Failed to save appointment: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateHospitalCollection(appointment: Appointment) {
        Log.d("HospitalUpdate", "Saving appointment to RTDB for hospital: ${appointment.hospitalId}")
        
        // Create appointment data map with all fields including donorName
        val appointmentData = mapOf(
            "donorId" to appointment.donorId,
            "donorName" to appointment.donorName,
            "hospitalId" to appointment.hospitalId,
            "slotId" to appointment.slotId,
            "date" to appointment.appointmentDate,
            "time" to appointment.appointmentTime,
            "status" to appointment.status,
            "bookedAt" to System.currentTimeMillis()
        )
        
        // Save to appointments_active collection (indexed by hospitalId)
        val appointmentsActiveRef = database.child("appointments_active")
            .child(appointment.appointmentId)
        
        appointmentsActiveRef.setValue(appointmentData)
            .addOnSuccessListener {
                Log.d("HospitalUpdate", "✅ Appointment saved to appointments_active")
                Log.d("HospitalUpdate", "Appointment ID: ${appointment.appointmentId}")
                Log.d("HospitalUpdate", "Donor: ${appointment.donorName}")
                Log.d("HospitalUpdate", "Hospital: ${appointment.hospitalId}")
            }
            .addOnFailureListener { error ->
                Log.e("HospitalUpdate", "❌ Failed to save to appointments_active", error)
                Log.e("HospitalUpdate", "Error: ${error.message}")
            }
    }

    private fun checkDonationEligibility(donorId: String) {
        Log.d("AppointmentBooking", "Checking 52-day donation eligibility for donor: $donorId")
        
        // Check appointments_history for last completed donation
        database.child("appointments_history")
            .orderByChild("donorId")
            .equalTo(donorId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var lastCompletedDonationDate: Long? = null
                    
                    for (appointmentSnapshot in snapshot.children) {
                        val appointmentData = appointmentSnapshot.value as? Map<*, *>
                        val status = appointmentData?.get("status") as? String
                        
                        if (status == "completed") {
                            val updatedAt = appointmentData?.get("updatedAt") as? Long
                            if (updatedAt != null) {
                                if (lastCompletedDonationDate == null || updatedAt > lastCompletedDonationDate) {
                                    lastCompletedDonationDate = updatedAt
                                }
                            }
                        }
                    }
                    
                    if (lastCompletedDonationDate != null) {
                        val daysSinceLastDonation = (System.currentTimeMillis() - lastCompletedDonationDate) / (1000 * 60 * 60 * 24)
                        Log.d("AppointmentBooking", "Last donation was $daysSinceLastDonation days ago")
                        
                        if (daysSinceLastDonation < 52) {
                            val daysRemaining = 52 - daysSinceLastDonation
                            Log.d("AppointmentBooking", "Donor not eligible - $daysRemaining days remaining")
                            Toast.makeText(
                                this@AppointmentSchedulingActivity,
                                "You can donate again in $daysRemaining days. Donors must wait 52 days between donations.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Log.d("AppointmentBooking", "Donor is eligible - proceeding with booking")
                            createAppointment(donorId)
                        }
                    } else {
                        Log.d("AppointmentBooking", "No previous completed donations found - donor is eligible")
                        createAppointment(donorId)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppointmentBooking", "Error checking donation history", error.toException())
                    Toast.makeText(this@AppointmentSchedulingActivity, "Could not verify donation history. Please try again.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getSelectedSlot(): Slot? {
        return availableSlots.find { it.slotId == selectedSlotId }
    }
}

// Data classes
data class Slot(
    var slotId: String = "",
    var hospitalId: String = "",
    var hospitalName: String = "",
    var hospitalAddress: String = "",
    val timestamp: Long = 0,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val capacity: Int = 0,
    val available: Int = 0,
    val createdAt: Long = 0,
    val date: String = ""
)

data class Appointment(
    val appointmentId: String = "",
    val donorId: String = "",
    val donorName: String = "",
    val hospitalId: String = "",
    val slotId: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

// Simple adapter for slots
class SlotAdapter(
    private val slots: List<Slot>,
    private val onSlotClick: (Slot) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<SlotAdapter.SlotViewHolder>() {
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SlotViewHolder {
        val binding = com.example.blood_bud.databinding.ItemSlotBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return SlotViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]
        holder.bind(slot, onSlotClick)
    }
    
    override fun getItemCount() = slots.size
    
    class SlotViewHolder(private val binding: com.example.blood_bud.databinding.ItemSlotBinding) : 
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(slot: Slot, onSlotClick: (Slot) -> Unit) {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.apply {
                tvHospitalName.text = slot.hospitalName
                tvHospitalAddress.text = slot.hospitalAddress
                tvTimeSlot.text = "${format.format(Date(slot.startTime))} - ${format.format(Date(slot.endTime))}"
                tvAvailable.text = "${slot.available}/${slot.capacity} available"
                
                root.setOnClickListener { onSlotClick(slot) }
            }
        }
    }
}
