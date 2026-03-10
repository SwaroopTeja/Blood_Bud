package com.example.blood_bud.Activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.blood_bud.R
import com.example.blood_bud.data.model.AppointmentSlot
import com.example.blood_bud.data.model.ScheduleType
import com.example.blood_bud.data.model.SlotCreationRequest
import com.example.blood_bud.databinding.ActivityAddAppointmentSlotBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddAppointmentSlotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAppointmentSlotBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDb: FirebaseDatabase
    private var hospitalId: String = ""
    private var hospitalName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAppointmentSlotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDb = FirebaseDatabase.getInstance()
        hospitalId = auth.currentUser?.uid ?: ""

        setupToolbar()
        setupViews()
        loadHospitalInfo()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Appointment Slots"
    }

    private fun setupViews() {
        // Set default values
        binding.etStartDateSingle.setText(getCurrentDate())
        binding.etStartDate.setText(getCurrentDate())
        binding.etEndDate.setText(getCurrentDate())
        binding.etStartTime.setText("09:00")
        binding.etEndTime.setText("17:00")
        binding.etSlotDuration.setText("30")
        binding.etCapacity.setText("10")
        binding.etBreakStart.setText("12:00")
        binding.etBreakEnd.setText("13:00")

        // Setup click listeners and radio button listener
        setupClickListeners()
        setupScheduleTypeListener()
    }

    private fun setupClickListeners() {
        // Single day date picker
        binding.etStartDateSingle.setOnClickListener { showDatePicker(true, true) }

        // Weekly schedule date pickers
        binding.etStartDate.setOnClickListener { showDatePicker(true) }
        binding.etEndDate.setOnClickListener { showDatePicker(false) }

        // Time pickers
        binding.etStartTime.setOnClickListener { showTimePicker(true) }
        binding.etEndTime.setOnClickListener { showTimePicker(false) }
        binding.etBreakStart.setOnClickListener { showTimePicker(true, true) }
        binding.etBreakEnd.setOnClickListener { showTimePicker(false, true) }

        // Buttons
        binding.btnPreview.setOnClickListener { previewSlots() }
        binding.btnCreateSlots.setOnClickListener { createSlots() }
    }

    private fun setupScheduleTypeListener() {
        binding.rgScheduleType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSingleDay -> {
                    // Show single day date selection, hide weekly dates
                    binding.tilStartDateSingle.visibility = View.VISIBLE
                    binding.llWeeklyDates.visibility = View.GONE
                    // Copy current date to single day field if needed
                    if (binding.etStartDate.text.toString().isNotEmpty()) {
                        binding.etStartDateSingle.setText(binding.etStartDate.text.toString())
                    }
                }
                R.id.rbWeeklySchedule -> {
                    // Show weekly date selection, hide single day
                    binding.tilStartDateSingle.visibility = View.GONE
                    binding.llWeeklyDates.visibility = View.VISIBLE
                    // Set default end date to 7 days from start date
                    val startDate = binding.etStartDate.text.toString()
                    if (startDate.isNotEmpty()) {
                        binding.etEndDate.setText(getDateAfterDays(startDate, 6))
                    }
                }
            }
        }
    }

    private fun loadHospitalInfo() {
        if (hospitalId.isEmpty()) {
            Toast.makeText(this, "Error: Hospital ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("hospitals").document(hospitalId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    hospitalName = document.getString("name") ?: "Unknown Hospital"
                    binding.tvHospitalName.text = "Creating slots for: $hospitalName"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading hospital info", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePicker(isStartDate: Boolean, isSingleDay: Boolean = false) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

                if (isSingleDay) {
                    binding.etStartDateSingle.setText(selectedDate)
                } else {
                    if (isStartDate) {
                        binding.etStartDate.setText(selectedDate)
                        // If selecting start date for weekly schedule, update end date to 7 days later
                        if (binding.rbWeeklySchedule.isChecked) {
                            binding.etEndDate.setText(getDateAfterDays(selectedDate, 6))
                        }
                    } else {
                        binding.etEndDate.setText(selectedDate)
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker(isStartTime: Boolean, isBreakTime: Boolean = false) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                if (isBreakTime) {
                    if (isStartTime) {
                        binding.etBreakStart.setText(selectedTime)
                    } else {
                        binding.etBreakEnd.setText(selectedTime)
                    }
                } else {
                    if (isStartTime) {
                        binding.etStartTime.setText(selectedTime)
                    } else {
                        binding.etEndTime.setText(selectedTime)
                    }
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun createSlots() {
        if (!validateInputs()) return

        val request = SlotCreationRequest(
            hospitalId = hospitalId,
            scheduleType = if (binding.rbWeeklySchedule.isChecked) ScheduleType.WEEKLY else ScheduleType.SINGLE_DAY,
            startDate = if (binding.rbSingleDay.isChecked) {
                binding.etStartDateSingle.text.toString()
            } else {
                binding.etStartDate.text.toString()
            },
            endDate = if (binding.rbWeeklySchedule.isChecked) {
                binding.etEndDate.text?.toString()
            } else {
                null
            },
            startTime = binding.etStartTime.text.toString(),
            endTime = binding.etEndTime.text.toString(),
            slotDuration = binding.etSlotDuration.text.toString().toIntOrNull() ?: 30,
            capacity = binding.etCapacity.text.toString().toIntOrNull() ?: 10,
            breakStart = binding.etBreakStart.text?.toString(),
            breakEnd = binding.etBreakEnd.text?.toString(),
            excludeWeekends = binding.cbExcludeWeekends.isChecked,
            workingDays = listOf(1, 2, 3, 4, 5, 6, 7) // Monday to Sunday for now
        )

        // Generate and save slots
        generateAndSaveSlots(request)
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate date based on schedule type
        if (binding.rbSingleDay.isChecked) {
            // Single day validation
            if (binding.etStartDateSingle.text.isNullOrBlank()) {
                binding.tilStartDateSingle.error = "Date is required"
                isValid = false
            } else {
                binding.tilStartDateSingle.error = null
            }
        } else {
            // Weekly schedule validation
            if (binding.etStartDate.text.isNullOrBlank()) {
                binding.tilStartDate.error = "Start date is required"
                isValid = false
            } else {
                binding.tilStartDate.error = null
            }

            if (binding.etEndDate.text.isNullOrBlank()) {
                binding.tilEndDate.error = "End date is required"
                isValid = false
            } else {
                binding.tilEndDate.error = null
            }
        }

        // Validate start time
        if (binding.etStartTime.text.isNullOrBlank()) {
            binding.tilStartTime.error = "Start time is required"
            isValid = false
        } else {
            binding.tilStartTime.error = null
        }

        // Validate end time
        if (binding.etEndTime.text.isNullOrBlank()) {
            binding.tilEndTime.error = "End time is required"
            isValid = false
        } else {
            binding.tilEndTime.error = null
        }

        // Validate slot duration
        val duration = binding.etSlotDuration.text.toString().toIntOrNull()
        if (duration == null || duration <= 0) {
            binding.tilSlotDuration.error = "Valid slot duration is required"
            isValid = false
        } else {
            binding.tilSlotDuration.error = null
        }

        // Validate capacity
        val capacity = binding.etCapacity.text.toString().toIntOrNull()
        if (capacity == null || capacity <= 0) {
            binding.tilCapacity.error = "Valid capacity is required"
            isValid = false
        } else {
            binding.tilCapacity.error = null
        }

        return isValid
    }

    private fun generateAndSaveSlots(request: SlotCreationRequest) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCreateSlots.isEnabled = false

        try {
            val slots = generateSlots(request)

            if (slots.isEmpty()) {
                binding.progressBar.visibility = View.GONE
                binding.btnCreateSlots.isEnabled = true
                Toast.makeText(this, "No slots were generated. Please check your settings.", Toast.LENGTH_LONG).show()
                return
            }

            // Save to Firebase RTDB
            saveSlotsToRTDB(slots) { success ->
                binding.progressBar.visibility = View.GONE
                binding.btnCreateSlots.isEnabled = true

                if (success) {
                    Toast.makeText(this, "Slots created successfully! (${slots.size} slots)", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error saving slots to database. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.btnCreateSlots.isEnabled = true
            Toast.makeText(this, "Error generating slots: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("AddAppointmentSlot", "Slot generation error", e)
        }
    }

    private fun generateSlots(request: SlotCreationRequest): List<AppointmentSlot> {
        val slots = mutableListOf<AppointmentSlot>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        try {
            // Parse dates with error handling
            val startDate = try {
                dateFormat.parse(request.startDate) ?: run {
                    Log.e("AddAppointmentSlot", "Failed to parse start date: ${request.startDate}")
                    return emptyList()
                }
            } catch (e: Exception) {
                Log.e("AddAppointmentSlot", "Error parsing start date: ${request.startDate}", e)
                return emptyList()
            }

            val endDate = request.endDate?.let {
                try {
                    dateFormat.parse(it) ?: startDate
                } catch (e: Exception) {
                    Log.e("AddAppointmentSlot", "Error parsing end date: $it", e)
                    startDate
                }
            } ?: startDate

            // Parse times with error handling - try both 12-hour and 24-hour formats
            val startTime = try {
                // Try 24-hour format first (HH:mm)
                try {
                    timeFormat.parse(request.startTime)
                } catch (e1: Exception) {
                    // If that fails, try 12-hour format (hh:mm a)
                    val twelveHourFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    twelveHourFormat.parse(request.startTime)
                }
            } catch (e: Exception) {
                Log.e("AddAppointmentSlot", "Failed to parse start time: ${request.startTime}", e)
                Toast.makeText(this, "Invalid start time format: ${request.startTime}", Toast.LENGTH_LONG).show()
                return emptyList()
            }

            val endTime = try {
                // Try 24-hour format first (HH:mm)
                try {
                    timeFormat.parse(request.endTime)
                } catch (e1: Exception) {
                    // If that fails, try 12-hour format (hh:mm a)
                    val twelveHourFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    twelveHourFormat.parse(request.endTime)
                }
            } catch (e: Exception) {
                Log.e("AddAppointmentSlot", "Failed to parse end time: ${request.endTime}", e)
                Toast.makeText(this, "Invalid end time format: ${request.endTime}", Toast.LENGTH_LONG).show()
                return emptyList()
            }

            val breakStart = request.breakStart?.let {
                try {
                    // Try 24-hour format first (HH:mm)
                    try {
                        timeFormat.parse(it)
                    } catch (e1: Exception) {
                        // If that fails, try 12-hour format (hh:mm a)
                        val twelveHourFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        twelveHourFormat.parse(it)
                    }
                } catch (e: Exception) {
                    Log.e("AddAppointmentSlot", "Error parsing break start time: $it", e)
                    null
                }
            }

            val breakEnd = request.breakEnd?.let {
                try {
                    // Try 24-hour format first (HH:mm)
                    try {
                        timeFormat.parse(it)
                    } catch (e1: Exception) {
                        // If that fails, try 12-hour format (hh:mm a)
                        val twelveHourFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        twelveHourFormat.parse(it)
                    }
                } catch (e: Exception) {
                    Log.e("AddAppointmentSlot", "Error parsing break end time: $it", e)
                    null
                }
            }

            val calendar = Calendar.getInstance()
            calendar.time = startDate

            Log.d("AddAppointmentSlot", "Generating slots from ${request.startDate} to ${request.endDate ?: request.startDate}")
            Log.d("AddAppointmentSlot", "Time range: ${request.startTime} - ${request.endTime}")

            while (!calendar.time.after(endDate)) {
                val currentDate = calendar.time
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                // Skip weekends if excluded
                if (request.excludeWeekends && (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    continue
                }

                // Generate slots for this day
                var currentSlotStart = Calendar.getInstance()
                currentSlotStart.time = currentDate
                currentSlotStart.set(Calendar.HOUR_OF_DAY, startTime.hours)
                currentSlotStart.set(Calendar.MINUTE, startTime.minutes)
                currentSlotStart.set(Calendar.SECOND, 0)
                currentSlotStart.set(Calendar.MILLISECOND, 0)

                val dayEndTime = Calendar.getInstance()
                dayEndTime.time = currentDate
                dayEndTime.set(Calendar.HOUR_OF_DAY, endTime.hours)
                dayEndTime.set(Calendar.MINUTE, endTime.minutes)
                dayEndTime.set(Calendar.SECOND, 0)
                dayEndTime.set(Calendar.MILLISECOND, 0)

                var slotsCreatedForDay = 0
                while (currentSlotStart.before(dayEndTime)) {
                    val slotEnd = Calendar.getInstance()
                    slotEnd.timeInMillis = currentSlotStart.timeInMillis
                    slotEnd.add(Calendar.MINUTE, request.slotDuration)

                    // Skip slots that would end after day end time
                    if (slotEnd.after(dayEndTime)) break

                    // Check if slot conflicts with break time
                    val conflictsWithBreak = breakStart != null && breakEnd != null &&
                        currentSlotStart.timeInMillis < breakEnd.time &&
                        slotEnd.timeInMillis > breakStart.time

                    if (!conflictsWithBreak) {
                        val slot = AppointmentSlot(
                            hospitalId = request.hospitalId,
                            hospitalName = hospitalName,
                            date = dateFormat.format(currentDate),
                            startTime = currentSlotStart.timeInMillis,
                            endTime = slotEnd.timeInMillis,
                            slotDuration = request.slotDuration,
                            capacity = request.capacity,
                            breakStart = breakStart?.time,
                            breakEnd = breakEnd?.time
                        )
                        slots.add(slot)
                        slotsCreatedForDay++
                    }

                    currentSlotStart.add(Calendar.MINUTE, request.slotDuration)
                }

                Log.d("AddAppointmentSlot", "Created $slotsCreatedForDay slots for ${dateFormat.format(currentDate)}")

                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            Log.d("AddAppointmentSlot", "Total slots generated: ${slots.size}")
            return slots

        } catch (e: Exception) {
            Log.e("AddAppointmentSlot", "Error generating slots", e)
            return emptyList()
        }
    }

    private fun saveSlotsToRTDB(slots: List<AppointmentSlot>, callback: (Boolean) -> Unit) {
        if (auth.currentUser == null) {
            Log.e("AddAppointmentSlot", "User not authenticated - cannot save slots")
            Toast.makeText(this, "Authentication error - please log in again", Toast.LENGTH_LONG).show()
            callback(false)
            return
        }

        val userId = auth.currentUser?.uid ?: ""
        if (userId.isEmpty()) {
            Log.e("AddAppointmentSlot", "Empty user ID - cannot save slots")
            Toast.makeText(this, "User ID error - please log in again", Toast.LENGTH_LONG).show()
            callback(false)
            return
        }

        Log.d("AddAppointmentSlot", "Saving ${slots.size} slots for user: $userId")

        // Debug Firebase configuration
        Log.d("AddAppointmentSlot", "Firebase Auth current user: ${auth.currentUser?.email}")
        Log.d("AddAppointmentSlot", "Firebase RTDB instance: ${realtimeDb.app.name}")

        val slotsRef = realtimeDb.getReference("appointment_slots")

        // Group slots by date for better organization
        val slotsByDate = slots.groupBy { it.date }

        Log.d("AddAppointmentSlot", "Slots grouped by dates: ${slotsByDate.keys}")

        var completedOperations = 0
        var failedOperations = 0
        val totalOperations = slotsByDate.size

        slotsByDate.forEach { (date, dateSlots) ->
            val dateRef = slotsRef.child(date).child(userId)

            Log.d("AddAppointmentSlot", "Saving ${dateSlots.size} slots for date: $date at path: appointment_slots/$date/$userId")

            // Convert slots to map for Firebase
            val slotsMap = dateSlots.associate { slot ->
                Log.d("AddAppointmentSlot", "Creating slot entry: ${slot.id} -> ${slot.date} ${slot.getTimeRange()}")
                slot.id to mapOf(
                    "hospitalId" to slot.hospitalId,
                    "hospitalName" to slot.hospitalName,
                    "date" to slot.date,
                    "startTime" to slot.startTime,
                    "endTime" to slot.endTime,
                    "slotDuration" to slot.slotDuration,
                    "capacity" to slot.capacity,
                    "currentBookings" to slot.currentBookings,
                    "breakStart" to slot.breakStart,
                    "breakEnd" to slot.breakEnd,
                    "isActive" to slot.isActive,
                    "createdAt" to slot.createdAt,
                    "bookedBy" to slot.bookedBy
                )
            }

            Log.d("AddAppointmentSlot", "Saving slots map with ${slotsMap.size} entries")

            dateRef.setValue(slotsMap)
                .addOnSuccessListener {
                    completedOperations++
                    Log.d("AddAppointmentSlot", "Successfully saved slots for date: $date (Operation $completedOperations/$totalOperations)")
                    checkAllOperationsComplete(callback, completedOperations, failedOperations, totalOperations)
                }
                .addOnFailureListener { e ->
                    failedOperations++
                    Log.e("AddAppointmentSlot", "Error saving slots for date: $date - ${e.message}", e)

                    // Show specific error to user based on error type
                    val errorMessage = when {
                        e.message?.contains("permission_denied") == true -> "Permission denied - check Firebase security rules"
                        e.message?.contains("network") == true -> "Network error - check internet connection"
                        e.message?.contains("auth") == true -> "Authentication error - please log in again"
                        else -> "Database error: ${e.message}"
                    }

                    Toast.makeText(this@AddAppointmentSlotActivity, errorMessage, Toast.LENGTH_LONG).show()
                    checkAllOperationsComplete(callback, completedOperations, failedOperations, totalOperations)
                }
        }
    }

    private fun checkAllOperationsComplete(callback: (Boolean) -> Unit, completed: Int, failed: Int, total: Int) {
        if (completed + failed >= total) {
            val success = failed == 0
            Log.d("AddAppointmentSlot", "All operations complete. Success: $success (Completed: $completed, Failed: $failed, Total: $total)")
            callback(success)
        }
    }

    private fun previewSlots() {
        if (!validateInputs()) {
            Toast.makeText(this, "Please fix the validation errors before previewing slots.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val request = SlotCreationRequest(
                hospitalId = hospitalId,
                scheduleType = if (binding.rbWeeklySchedule.isChecked) ScheduleType.WEEKLY else ScheduleType.SINGLE_DAY,
                startDate = if (binding.rbSingleDay.isChecked) {
                    binding.etStartDateSingle.text.toString()
                } else {
                    binding.etStartDate.text.toString()
                },
                endDate = if (binding.rbWeeklySchedule.isChecked) {
                    binding.etEndDate.text?.toString()
                } else {
                    null
                },
                startTime = binding.etStartTime.text.toString(),
                endTime = binding.etEndTime.text.toString(),
                slotDuration = binding.etSlotDuration.text.toString().toIntOrNull() ?: 30,
                capacity = binding.etCapacity.text.toString().toIntOrNull() ?: 10,
                breakStart = binding.etBreakStart.text?.toString(),
                breakEnd = binding.etBreakEnd.text?.toString(),
                excludeWeekends = binding.cbExcludeWeekends.isChecked,
                workingDays = listOf(1, 2, 3, 4, 5, 6, 7) // Monday to Sunday for now
            )

            Log.d("AddAppointmentSlot", "Preview request: $request")

            val slots = generateSlots(request)

            if (slots.isEmpty()) {
                Toast.makeText(this, "No slots were generated. Please check your time settings.", Toast.LENGTH_LONG).show()
                return
            }

            // Show preview dialog
            val previewMessage = buildString {
                append("Preview: ${slots.size} slots will be created\n\n")
                slots.take(5).forEach { slot ->
                    append("${slot.date}: ${slot.getTimeRange()} (${slot.capacity} slots)\n")
                }
                if (slots.size > 5) {
                    append("... and ${slots.size - 5} more slots")
                }
            }

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Slot Preview")
                .setMessage(previewMessage)
                .setPositiveButton("Create Slots") { _, _ -> createSlots() }
                .setNegativeButton("Cancel", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error previewing slots: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("AddAppointmentSlot", "Preview error", e)
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getDateAfterDays(startDate: String, daysToAdd: Int): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(startDate)
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
            dateFormat.format(calendar.time)
        } catch (e: Exception) {
            startDate // Return original date if parsing fails
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}


