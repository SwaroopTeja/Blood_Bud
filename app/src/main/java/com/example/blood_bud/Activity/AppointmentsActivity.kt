package com.example.blood_bud.Activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blood_bud.R
import com.example.blood_bud.base.BaseHospitalActivity
import android.content.Intent
import com.example.blood_bud.databinding.ActivityAppointmentsBinding
import com.example.blood_bud.ui.hospital.HospitalAppointmentAdapter
import com.example.blood_bud.utils.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AppointmentsActivity : BaseHospitalActivity<ActivityAppointmentsBinding>() {

    private lateinit var auth: FirebaseAuth
    private lateinit var realtimeDb: FirebaseDatabase
    private lateinit var appointmentsAdapter: HospitalAppointmentAdapter
    private var appointmentsListener: ChildEventListener? = null
    private val appointmentsList = mutableListOf<com.example.blood_bud.data.model.Appointment>()
    private var showHistory = false

    override fun getViewBinding() = ActivityAppointmentsBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("AppointmentsActivity", "=== APPOINTMENTS ACTIVITY STARTED ===")

        try {
            auth = FirebaseAuth.getInstance()
            realtimeDb = FirebaseDatabase.getInstance()

            Log.d("AppointmentsActivity", "Firebase initialized successfully")
            Log.d("AppointmentsActivity", "Current user: ${auth.currentUser?.uid}")
            Log.d("AppointmentsActivity", "Current user email: ${auth.currentUser?.email}")

            setupToolbar()
            setupRecyclerView()
            setupHistoryButton()
            loadAppointments()
            setupViews()

            // Set up bottom navigation
            setupBottomNavigation(R.id.bottomNavigation)
        } catch (e: Exception) {
            Log.e("AppointmentsActivity", "Error initializing AppointmentsActivity", e)
            updateDebugInfo("❌ Error initializing: ${e.message}")
            showToast("Error loading appointments page")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure the correct navigation item is selected
        binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.nav_appointments
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.appointments)
    }

    private fun setupHistoryButton() {
        binding.btnViewHistory.setOnClickListener {
            showHistory = !showHistory
            binding.btnViewHistory.text = if (showHistory) "Upcoming" else "History"
            
            // Reload appointments with new filter
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            loadFromAppointmentsActive(userId)
        }
    }

    private fun setupRecyclerView() {
        try {
            Log.d("AppointmentsActivity", "🔧 Setting up RecyclerView...")

            appointmentsAdapter = HospitalAppointmentAdapter(
                context = this@AppointmentsActivity,
                onStatusUpdate = { appointment, newStatus -> updateAppointmentStatus(appointment, newStatus) }
            )

            Log.d("AppointmentsActivity", "✅ HospitalAppointmentAdapter created successfully")

            binding.rvAppointments.apply {
                adapter = appointmentsAdapter
                layoutManager = LinearLayoutManager(this@AppointmentsActivity)
                setHasFixedSize(false)  // Changed to false for wrap_content
                
                // Force layout update
                requestLayout()
            }
            
            Log.d("AppointmentsActivity", "✅ RecyclerView setup completed successfully")
            Log.d("AppointmentsActivity", "📋 RecyclerView initial state:")
            Log.d("AppointmentsActivity", "   - Width: ${binding.rvAppointments.width}, Height: ${binding.rvAppointments.height}")
            Log.d("AppointmentsActivity", "   - Visibility: ${binding.rvAppointments.visibility}")
            Log.d("AppointmentsActivity", "   - Adapter: ${binding.rvAppointments.adapter != null}")
            Log.d("AppointmentsActivity", "   - LayoutManager: ${binding.rvAppointments.layoutManager != null}")
        } catch (e: Exception) {
            Log.e("AppointmentsActivity", "❌ Error setting up RecyclerView", e)
            updateDebugInfo("❌ RecyclerView setup error: ${e.message}")
            showToast("Error setting up appointments list")
        }
    }

    private fun loadAppointments() {
        Log.d("AppointmentsActivity", "=== LOAD APPOINTMENTS STARTED ===")
        updateDebugInfo("Starting appointment loading...")

        if (auth.currentUser == null) {
            Log.e("AppointmentsActivity", "❌ User not authenticated - Current user is null")
            updateDebugInfo("❌ No authenticated user")
            showToast("Please log in to view appointments")
            return
        }

        val userId = auth.currentUser?.uid ?: ""
        Log.d("AppointmentsActivity", "🔍 Current user ID: $userId")
        updateDebugInfo("Loading appointments for hospital: $userId")

        if (userId.isEmpty()) {
            Log.e("AppointmentsActivity", "❌ Empty user ID")
            updateDebugInfo("❌ Empty user ID")
            showToast("User ID error")
            return
        }

        // Load appointments from appointments_active
        Log.d("AppointmentsActivity", "📡 Loading appointments for hospital: $userId")
        Log.d("AppointmentsActivity", "🔄 Starting appointment loading process...")
        updateDebugInfo("Loading from appointments_active...")

        // Load from appointments_active collection
        loadFromAppointmentsActive(userId)
    }

    private fun isUpcomingAppointment(appointment: com.example.blood_bud.data.model.Appointment): Boolean {
        Log.d("AppointmentsActivity", "🔍 Checking if appointment is upcoming: ${appointment.appointmentId}")
        Log.d("AppointmentsActivity", "📅 Appointment date: '${appointment.appointmentDate}'")

        if (appointment.appointmentDate.isBlank()) {
            Log.d("AppointmentsActivity", "❌ Appointment date is blank - filtering out")
            return false
        }

        try {
            // Handle both date formats: "MMM dd, yyyy" and "yyyy-MM-dd"
            val dateFormat1 = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            val dateFormat2 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

            val appointmentDate = try {
                // Try parsing as "MMM dd, yyyy" first
                Log.d("AppointmentsActivity", "🔄 Trying format 1 (MMM dd, yyyy): ${appointment.appointmentDate}")
                dateFormat1.parse(appointment.appointmentDate)
            } catch (e: Exception) {
                // If that fails, try parsing as "yyyy-MM-dd"
                Log.d("AppointmentsActivity", "🔄 Trying format 2 (yyyy-MM-dd): ${appointment.appointmentDate}")
                dateFormat2.parse(appointment.appointmentDate)
            }

            val today = java.util.Calendar.getInstance()

            // Set time to start of day for accurate comparison
            today.set(java.util.Calendar.HOUR_OF_DAY, 0)
            today.set(java.util.Calendar.MINUTE, 0)
            today.set(java.util.Calendar.SECOND, 0)
            today.set(java.util.Calendar.MILLISECOND, 0)

            val appointmentCalendar = java.util.Calendar.getInstance()
            appointmentCalendar.time = appointmentDate

            // Show appointments that are today or in the future
            val isUpcoming = appointmentCalendar.timeInMillis >= today.timeInMillis
            Log.d("AppointmentsActivity", "📊 Date check - Appointment: ${appointment.appointmentDate}")
            Log.d("AppointmentsActivity", "📊 Today: ${dateFormat2.format(today.time)}")
            Log.d("AppointmentsActivity", "📊 Appointment timestamp: ${appointmentCalendar.timeInMillis}")
            Log.d("AppointmentsActivity", "📊 Today timestamp: ${today.timeInMillis}")
            Log.d("AppointmentsActivity", "📊 Is upcoming: $isUpcoming")

            if (isUpcoming) {
                Log.d("AppointmentsActivity", "✅ Appointment is upcoming - will be displayed")
                updateDebugInfo("✅ Date check passed: ${appointment.appointmentDate}")
            } else {
                Log.d("AppointmentsActivity", "❌ Appointment is in the past - will be filtered out")
                updateDebugInfo("❌ Date check failed: ${appointment.appointmentDate} is past")
            }

            return isUpcoming
        } catch (e: Exception) {
            Log.e("AppointmentsActivity", "❌ Error parsing appointment date: ${appointment.appointmentDate}", e)
            return false
        }
    }

    private fun normalizeAppointmentData(appointment: com.example.blood_bud.data.model.Appointment, appointmentId: String): com.example.blood_bud.data.model.Appointment {
        // The Firebase data uses different field names in different collections
        // Main collection: appointmentDate, appointmentTime
        // Hospital collection: date (String/Long), time (String)

        Log.d("AppointmentsActivity", "🔄 Normalizing appointment data...")
        Log.d("AppointmentsActivity", "📋 Raw appointment data:")
        Log.d("AppointmentsActivity", "   - appointmentId: ${appointment.appointmentId}")
        Log.d("AppointmentsActivity", "   - appointmentDate: ${appointment.appointmentDate}")
        Log.d("AppointmentsActivity", "   - date field: ${appointment.date} (type: ${appointment.date::class.java.simpleName})")

        // Handle appointmentDate - if it's blank, try to use the date field
        val finalAppointmentDate = when {
            appointment.appointmentDate.isNotBlank() -> {
                Log.d("AppointmentsActivity", "✅ Using appointmentDate: ${appointment.appointmentDate}")
                appointment.appointmentDate
            }
            appointment.date.isNotBlank() -> {
                // date field contains a date string (like "2025-10-28" or timestamp)
                try {
                    if (appointment.date.contains("-")) {
                        // Looks like a date string (e.g., "2025-10-28")
                        Log.d("AppointmentsActivity", "✅ Using date field as date string: ${appointment.date}")
                        appointment.date
                    } else {
                        // Might be a timestamp as string, try to parse as date
                        val timestamp = appointment.date.toLongOrNull()
                        if (timestamp != null && timestamp > 0) {
                            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            val dateString = dateFormat.format(java.util.Date(timestamp))
                            Log.d("AppointmentsActivity", "✅ Converted timestamp string ${appointment.date} to date: $dateString")
                            dateString
                        } else {
                            Log.d("AppointmentsActivity", "⚠️ Invalid date format: ${appointment.date}")
                            "Unknown Date"
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AppointmentsActivity", "❌ Error parsing date field: ${appointment.date}", e)
                    "Unknown Date"
                }
            }
            else -> {
                Log.d("AppointmentsActivity", "⚠️ No valid date found, using default")
                "Unknown Date"
            }
        }

        // Handle appointmentTime - use default if blank
        val finalAppointmentTime = appointment.appointmentTime.takeIf { it.isNotBlank() } ?: "09:00 AM"
        Log.d("AppointmentsActivity", "⏰ Final time: $finalAppointmentTime")

        return appointment.copy(
            appointmentId = appointmentId,
            appointmentDate = finalAppointmentDate,
            appointmentTime = finalAppointmentTime
        )
    }

    private fun updateDebugInfo(message: String) {
        try {
            binding.tvDebugInfo.text = "🔍 Debug: $message"
            Log.d("AppointmentsActivity", "📱 Debug UI updated: $message")
        } catch (e: Exception) {
            Log.e("AppointmentsActivity", "Error updating debug info", e)
        }
    }

    private fun loadFromAppointmentsActive(hospitalId: String) {
        val appointmentsRef = realtimeDb.getReference("appointments_active")
            .orderByChild("hospitalId")
            .equalTo(hospitalId)

        Log.d("AppointmentsActivity", "🔍 Loading from appointments_active for hospital: $hospitalId")
        Log.d("AppointmentsActivity", "📊 Current appointments list size: ${appointmentsList.size}")

        appointmentsListener?.let { appointmentsRef.removeEventListener(it) }

        // Clear existing appointments
        appointmentsList.clear()
        appointmentsAdapter?.submitList(emptyList())
        updateEmptyState()

        appointmentsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("AppointmentsActivity", "📥 Child added: ${snapshot.key}")
                Log.d("AppointmentsActivity", "📋 Raw Firebase data: ${snapshot.value}")
                Log.d("AppointmentsActivity", "🔍 Data class: ${snapshot.value?.javaClass}")
                updateDebugInfo("📥 Found appointment: ${snapshot.key}")

                // Log all the individual fields to see what's actually in Firebase
                snapshot.value?.let { data ->
                    if (data is Map<*, *>) {
                        Log.d("AppointmentsActivity", "📊 Firebase fields:")
                        data.forEach { (key, value) ->
                            Log.d("AppointmentsActivity", "   $key: $value (type: ${value?.javaClass?.simpleName})")
                        }
                    }
                }

                try {
                    val appointment = snapshot.getValue(com.example.blood_bud.data.model.Appointment::class.java)
                    appointment?.let {
                        // Normalize appointment data - handle both data formats
                        val normalizedAppointment = normalizeAppointmentData(it, snapshot.key ?: "")

                        Log.d("AppointmentsActivity", "✅ Normalized appointment: ${normalizedAppointment.appointmentId}")
                        Log.d("AppointmentsActivity", "📅 Date: ${normalizedAppointment.appointmentDate}, Time: ${normalizedAppointment.appointmentTime}")
                        Log.d("AppointmentsActivity", "🏥 Hospital: ${normalizedAppointment.hospitalName}")
                        Log.d("AppointmentsActivity", "📊 Status: ${normalizedAppointment.status}")
                        updateDebugInfo("✅ Normalized: ${normalizedAppointment.appointmentDate} ${normalizedAppointment.appointmentTime}")

                        // Filter based on current view mode
                        val shouldShow = if (showHistory) {
                            // Show completed and cancelled appointments
                            normalizedAppointment.status in listOf("completed", "cancelled")
                        } else {
                            // Show upcoming appointments (pending)
                            isUpcomingAppointment(normalizedAppointment) && normalizedAppointment.status == "pending"
                        }
                        
                        if (shouldShow) {
                            Log.d("AppointmentsActivity", "✅ LOADED appointment: ${normalizedAppointment.appointmentId} - ${normalizedAppointment.appointmentDate} ${normalizedAppointment.appointmentTime} (${normalizedAppointment.status})")
                            updateDebugInfo("✅ Added appointment: ${normalizedAppointment.appointmentDate}")
                            appointmentsList.add(normalizedAppointment)

                            Log.d("AppointmentsActivity", "📋 Adapter state before submit:")
                            Log.d("AppointmentsActivity", "   - List size: ${appointmentsList.size}")
                            Log.d("AppointmentsActivity", "   - Adapter: ${if (appointmentsAdapter != null) "not null" else "null"}")

                            appointmentsAdapter?.submitList(appointmentsList.toList())

                            Log.d("AppointmentsActivity", "📋 Adapter state after submit:")
                            Log.d("AppointmentsActivity", "   - List size: ${appointmentsList.size}")

                            updateEmptyState()

                            // Force RecyclerView to update layout
                            binding.rvAppointments.post {
                                binding.rvAppointments.requestLayout()
                                appointmentsAdapter?.notifyDataSetChanged()
                                
                                Log.d("AppointmentsActivity", "🔄 Post-layout UI check:")
                                Log.d("AppointmentsActivity", "   - RecyclerView visibility: ${binding.rvAppointments.visibility}")
                                Log.d("AppointmentsActivity", "   - RecyclerView adapter: ${binding.rvAppointments.adapter != null}")
                                Log.d("AppointmentsActivity", "   - RecyclerView child count: ${binding.rvAppointments.childCount}")
                                Log.d("AppointmentsActivity", "   - RecyclerView width: ${binding.rvAppointments.width}, height: ${binding.rvAppointments.height}")
                                Log.d("AppointmentsActivity", "   - Adapter item count: ${appointmentsAdapter?.itemCount}")
                            }
                        } else {
                            Log.d("AppointmentsActivity", "❌ Filtered out past appointment: ${normalizedAppointment.appointmentId} - ${normalizedAppointment.appointmentDate}")
                            updateDebugInfo("❌ Past appointment filtered: ${normalizedAppointment.appointmentDate}")
                        }
                    } ?: run {
                        Log.e("AppointmentsActivity", "❌ Failed to deserialize appointment data from Firebase")
                        updateDebugInfo("❌ Failed to deserialize appointment data")
                    }
                } catch (e: Exception) {
                    Log.e("AppointmentsActivity", "❌ Error processing appointment data", e)
                    Log.e("AppointmentsActivity", "❌ Raw Firebase data: ${snapshot.value}")
                    Log.e("AppointmentsActivity", "❌ Exception type: ${e.javaClass.simpleName}")
                    Log.e("AppointmentsActivity", "❌ Exception message: ${e.message}")
                    updateDebugInfo("❌ Error processing: ${e.message}")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("AppointmentsActivity", "📝 Child changed: ${snapshot.key}")
                updateDebugInfo("📝 Appointment changed: ${snapshot.key}")
                try {
                    val updatedAppointment = snapshot.getValue(com.example.blood_bud.data.model.Appointment::class.java)
                    updatedAppointment?.let {
                        // Normalize appointment data
                        val normalizedAppointment = normalizeAppointmentData(it, snapshot.key ?: "")

                        // Check if it's an upcoming appointment before updating
                        if (isUpcomingAppointment(normalizedAppointment)) {
                            val index = appointmentsList.indexOfFirst { appointment: com.example.blood_bud.data.model.Appointment -> appointment.appointmentId == normalizedAppointment.appointmentId }
                            if (index != -1) {
                                appointmentsList[index] = normalizedAppointment
                                appointmentsAdapter?.submitList(appointmentsList.toList())
                            } else {
                                // Add if it wasn't in the list but is now upcoming
                                appointmentsList.add(normalizedAppointment)
                                appointmentsAdapter?.submitList(appointmentsList.toList())
                                updateEmptyState()
                            }
                        } else {
                            // Remove if it was in the list but is no longer upcoming
                            val index = appointmentsList.indexOfFirst { appointment: com.example.blood_bud.data.model.Appointment -> appointment.appointmentId == normalizedAppointment.appointmentId }
                            if (index != -1) {
                                appointmentsList.removeAt(index)
                                appointmentsAdapter?.submitList(appointmentsList.toList())
                                updateEmptyState()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AppointmentsActivity", "❌ Error updating appointment", e)
                    updateDebugInfo("❌ Error updating: ${e.message}")
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                Log.d("AppointmentsActivity", "🗑️ Child removed: ${snapshot.key}")
                updateDebugInfo("🗑️ Appointment removed: ${snapshot.key}")
                try {
                    val removedAppointment = snapshot.getValue(com.example.blood_bud.data.model.Appointment::class.java)
                    removedAppointment?.let {
                        // Normalize appointment data
                        val normalizedAppointment = normalizeAppointmentData(it, snapshot.key ?: "")
                        appointmentsList.removeAll { appointment: com.example.blood_bud.data.model.Appointment -> appointment.appointmentId == normalizedAppointment.appointmentId }
                        appointmentsAdapter?.submitList(appointmentsList.toList())
                        updateEmptyState()
                    }
                } catch (e: Exception) {
                    Log.e("AppointmentsActivity", "❌ Error removing appointment", e)
                    updateDebugInfo("❌ Error removing: ${e.message}")
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("AppointmentsActivity", "📦 Child moved: ${snapshot.key}")
                updateDebugInfo("📦 Appointment moved: ${snapshot.key}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("AppointmentsActivity", "❌ Error loading appointments from hospital collection", error.toException())
                Log.d("AppointmentsActivity", "🔄 Falling back to main appointments collection")
                updateDebugInfo("❌ Hospital collection error, trying main collection...")
                showToast("Error loading appointments: ${error.message}")
                // Fallback to main appointments collection
                loadFromMainCollection(hospitalId)
            }
        }

        try {
            appointmentsRef.addChildEventListener(appointmentsListener!!)
            Log.d("AppointmentsActivity", "✅ Firebase listener attached to hospital collection")
            updateDebugInfo("✅ Listening to hospital collection...")
        } catch (e: Exception) {
            Log.e("AppointmentsActivity", "❌ Error adding Firebase listener", e)
            updateDebugInfo("❌ Error connecting: ${e.message}")
            showToast("Error connecting to appointments data")
        }
    }

    private fun loadFromMainCollection(hospitalId: String) {
        val appointmentsRef = realtimeDb.getReference("appointments")
            .orderByChild("hospitalId")
            .equalTo(hospitalId)

        Log.d("AppointmentsActivity", "🔍 Loading from main appointments collection for hospital: $hospitalId")
        Log.d("AppointmentsActivity", "📊 Current appointments list size: ${appointmentsList.size}")

        appointmentsListener?.let { appointmentsRef.removeEventListener(it) }

        appointmentsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("AppointmentsActivity", "📥 Child added from main collection: ${snapshot.key}")
                Log.d("AppointmentsActivity", "📋 Main collection data: ${snapshot.value}")
                Log.d("AppointmentsActivity", "🔍 Data class: ${snapshot.value?.javaClass}")
                updateDebugInfo("📥 Found in main collection: ${snapshot.key}")

                // Log all the individual fields to see what's actually in Firebase
                snapshot.value?.let { data ->
                    if (data is Map<*, *>) {
                        Log.d("AppointmentsActivity", "📊 Main collection fields:")
                        data.forEach { (key, value) ->
                            Log.d("AppointmentsActivity", "   $key: $value (type: ${value?.javaClass?.simpleName})")
                        }
                    }
                }

                try {
                    val appointment = snapshot.getValue(com.example.blood_bud.data.model.Appointment::class.java)
                    appointment?.let {
                        // Normalize appointment data - handle both data formats
                        val normalizedAppointment = normalizeAppointmentData(it, snapshot.key ?: "")

                        Log.d("AppointmentsActivity", "✅ Normalized appointment from main: ${normalizedAppointment.appointmentId}")
                        Log.d("AppointmentsActivity", "📅 Date: ${normalizedAppointment.appointmentDate}, Time: ${normalizedAppointment.appointmentTime}")
                        Log.d("AppointmentsActivity", "🏥 Hospital: ${normalizedAppointment.hospitalName}")
                        Log.d("AppointmentsActivity", "📊 Status: ${normalizedAppointment.status}")
                        updateDebugInfo("✅ Main collection normalized: ${normalizedAppointment.appointmentDate}")

                        // Only show upcoming appointments (today or future)
                        if (isUpcomingAppointment(normalizedAppointment)) {
                            Log.d("AppointmentsActivity", "✅ LOADED upcoming appointment from main: ${normalizedAppointment.appointmentId} - ${normalizedAppointment.appointmentDate} ${normalizedAppointment.appointmentTime}")
                            updateDebugInfo("✅ Added from main: ${normalizedAppointment.appointmentDate}")
                            appointmentsList.add(normalizedAppointment)

                            Log.d("AppointmentsActivity", "📋 Main collection adapter state before submit:")
                            Log.d("AppointmentsActivity", "   - List size: ${appointmentsList.size}")
                            Log.d("AppointmentsActivity", "   - Adapter: ${if (appointmentsAdapter != null) "not null" else "null"}")

                            appointmentsAdapter?.submitList(appointmentsList.toList())

                            Log.d("AppointmentsActivity", "📋 Main collection adapter state after submit:")
                            Log.d("AppointmentsActivity", "   - List size: ${appointmentsList.size}")

                            updateEmptyState()

                            // Force UI refresh after a short delay
                            binding.rvAppointments.postDelayed({
                                Log.d("AppointmentsActivity", "🔄 Main collection post-delay UI check:")
                                Log.d("AppointmentsActivity", "   - RecyclerView visibility: ${binding.rvAppointments.visibility}")
                                Log.d("AppointmentsActivity", "   - RecyclerView adapter: ${binding.rvAppointments.adapter != null}")
                                Log.d("AppointmentsActivity", "   - RecyclerView child count: ${binding.rvAppointments.childCount}")
                                Log.d("AppointmentsActivity", "   - RecyclerView width: ${binding.rvAppointments.width}, height: ${binding.rvAppointments.height}")
                            }, 100)
                        } else {
                            Log.d("AppointmentsActivity", "❌ Filtered out past appointment from main: ${normalizedAppointment.appointmentId} - ${normalizedAppointment.appointmentDate}")
                            updateDebugInfo("❌ Past from main: ${normalizedAppointment.appointmentDate}")
                        }
                    } ?: run {
                        Log.e("AppointmentsActivity", "❌ Failed to deserialize appointment data from main collection")
                        updateDebugInfo("❌ Failed to deserialize from main collection")
                    }
                } catch (e: Exception) {
                    Log.e("AppointmentsActivity", "❌ Error processing appointment data from main collection", e)
                    Log.e("AppointmentsActivity", "❌ Raw Firebase data from main: ${snapshot.value}")
                    Log.e("AppointmentsActivity", "❌ Exception type: ${e.javaClass.simpleName}")
                    Log.e("AppointmentsActivity", "❌ Exception message: ${e.message}")
                    updateDebugInfo("❌ Main collection error: ${e.message}")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("AppointmentsActivity", "📝 Child changed in main collection: ${snapshot.key}")
                updateDebugInfo("📝 Main collection changed: ${snapshot.key}")
                try {
                    val updatedAppointment = snapshot.getValue(com.example.blood_bud.data.model.Appointment::class.java)
                    updatedAppointment?.let {
                        // Normalize appointment data
                        val normalizedAppointment = normalizeAppointmentData(it, snapshot.key ?: "")

                        // Check if it's an upcoming appointment before updating
                        if (isUpcomingAppointment(normalizedAppointment)) {
                            val index = appointmentsList.indexOfFirst { appointment: com.example.blood_bud.data.model.Appointment -> appointment.appointmentId == normalizedAppointment.appointmentId }
                            if (index != -1) {
                                appointmentsList[index] = normalizedAppointment
                                appointmentsAdapter?.submitList(appointmentsList.toList())
                            } else {
                                // Add if it wasn't in the list but is now upcoming
                                appointmentsList.add(normalizedAppointment)
                                appointmentsAdapter?.submitList(appointmentsList.toList())
                                updateEmptyState()
                            }
                        } else {
                            // Remove if it was in the list but is no longer upcoming
                            val index = appointmentsList.indexOfFirst { appointment: com.example.blood_bud.data.model.Appointment -> appointment.appointmentId == normalizedAppointment.appointmentId }
                            if (index != -1) {
                                appointmentsList.removeAt(index)
                                appointmentsAdapter?.submitList(appointmentsList.toList())
                                updateEmptyState()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AppointmentsActivity", "❌ Error updating appointment from main collection", e)
                    updateDebugInfo("❌ Main collection update error: ${e.message}")
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                Log.d("AppointmentsActivity", "🗑️ Child removed from main collection: ${snapshot.key}")
                updateDebugInfo("🗑️ Removed from main: ${snapshot.key}")
                try {
                    val removedAppointment = snapshot.getValue(com.example.blood_bud.data.model.Appointment::class.java)
                    removedAppointment?.let {
                        // Normalize appointment data
                        val normalizedAppointment = normalizeAppointmentData(it, snapshot.key ?: "")
                        appointmentsList.removeAll { appointment: com.example.blood_bud.data.model.Appointment -> appointment.appointmentId == normalizedAppointment.appointmentId }
                        appointmentsAdapter?.submitList(appointmentsList.toList())
                        updateEmptyState()
                    }
                } catch (e: Exception) {
                    Log.e("AppointmentsActivity", "❌ Error removing appointment from main collection", e)
                    updateDebugInfo("❌ Main collection remove error: ${e.message}")
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("AppointmentsActivity", "📦 Child moved in main collection: ${snapshot.key}")
                updateDebugInfo("📦 Main collection moved: ${snapshot.key}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("AppointmentsActivity", "❌ Error loading appointments from main collection", error.toException())
                updateDebugInfo("❌ Main collection cancelled: ${error.message}")
                showToast("Error loading appointments: ${error.message}")
            }
        }

        try {
            appointmentsRef.addChildEventListener(appointmentsListener!!)
            Log.d("AppointmentsActivity", "✅ Firebase listener attached to main appointments collection")
            updateDebugInfo("✅ Listening to main collection...")
        } catch (e: Exception) {
            Log.e("AppointmentsActivity", "❌ Error adding Firebase listener to main collection", e)
            updateDebugInfo("❌ Main collection connection error: ${e.message}")
            showToast("Error connecting to appointments data")
        }
    }

    private fun updateAppointmentStatus(appointment: com.example.blood_bud.data.model.Appointment, newStatus: String) {
        val appointmentId = appointment.appointmentId ?: return

        try {
            Log.d("AppointmentsActivity", "🔄 Updating appointment $appointmentId to status: $newStatus")

            // If status is completed or cancelled, directly move to history without updating status first
            if (newStatus == "completed" || newStatus == "cancelled") {
                Log.d("AppointmentsActivity", "📦 Directly moving to history for final status: $newStatus")
                moveAppointmentToHistory(appointment, newStatus)
            } else {
                // For other statuses, just update
                val hospitalRef = realtimeDb.getReference("hospitals")
                    .child(appointment.hospitalId ?: "")
                    .child("appointments")
                    .child(appointmentId)

                val mainRef = realtimeDb.getReference("appointments").child(appointmentId)

                val updates = hashMapOf<String, Any>(
                    "status" to newStatus,
                    "updatedAt" to System.currentTimeMillis()
                )

                hospitalRef.updateChildren(updates).addOnCompleteListener { hospitalTask: com.google.android.gms.tasks.Task<Void> ->
                    mainRef.updateChildren(updates).addOnCompleteListener { mainTask: com.google.android.gms.tasks.Task<Void> ->
                        if (hospitalTask.isSuccessful && mainTask.isSuccessful) {
                            Log.d("AppointmentsActivity", "✅ Appointment status updated successfully")
                            showToast("Appointment ${newStatus.lowercase().replaceFirstChar { it.uppercase() }}")
                        } else {
                            showToast("Failed to update appointment status")
                            Log.e("AppointmentsActivity", "Error updating appointment status", hospitalTask.exception ?: mainTask.exception)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppointmentsActivity", "Error in updateAppointmentStatus", e)
            showToast("Error updating appointment")
        }
    }

    private fun moveAppointmentToHistory(appointment: com.example.blood_bud.data.model.Appointment, finalStatus: String) {
        val appointmentId = appointment.appointmentId ?: return
        val hospitalId = appointment.hospitalId ?: ""
        
        if (hospitalId.isBlank()) {
            Log.e("AppointmentsActivity", "❌ Cannot move to history: hospitalId is blank")
            showToast("Error: Missing hospital information")
            return
        }
        
        Log.d("AppointmentsActivity", "📦 Moving appointment $appointmentId to history")
        Log.d("AppointmentsActivity", "   Hospital ID: $hospitalId")
        Log.d("AppointmentsActivity", "   Final Status: $finalStatus")

        // Create history entry with updated status
        val historyAppointment = appointment.copy(
            status = finalStatus,
            updatedAt = System.currentTimeMillis()
        )

        // Reference to appointments_history collection
        val historyRef = realtimeDb.getReference("appointments_history").child(appointmentId)

        // First, remove from active collections, then save to history
        Log.d("AppointmentsActivity", "🗑️ Step 1: Removing from active collections")
        removeFromActiveCollections(appointmentId, hospitalId)
        
        // Then save to history
        Log.d("AppointmentsActivity", "💾 Step 2: Saving to history")
        historyRef.setValue(historyAppointment)
            .addOnSuccessListener {
                Log.d("AppointmentsActivity", "✅ Appointment moved to history successfully")
                showToast("Appointment ${finalStatus.lowercase().replaceFirstChar { it.uppercase() }}")
            }
            .addOnFailureListener { e ->
                Log.e("AppointmentsActivity", "❌ Error moving appointment to history", e)
                showToast("Appointment removed but failed to save to history")
            }
    }

    private fun removeFromActiveCollections(appointmentId: String, hospitalId: String) {
        Log.d("AppointmentsActivity", "🗑️ Removing appointment $appointmentId from active collections")
        Log.d("AppointmentsActivity", "   Hospital ID: $hospitalId")
        Log.d("AppointmentsActivity", "   Appointment ID: $appointmentId")

        // Remove from appointments_active collection (primary source)
        val activeRef = realtimeDb.getReference("appointments_active").child(appointmentId)
        
        // Remove from hospital collection
        val hospitalRef = realtimeDb.getReference("hospitals")
            .child(hospitalId)
            .child("appointments")
            .child(appointmentId)

        // Remove from main appointments collection
        val mainRef = realtimeDb.getReference("appointments").child(appointmentId)

        Log.d("AppointmentsActivity", "🗑️ Removing from: appointments_active/$appointmentId")
        activeRef.removeValue().addOnSuccessListener {
            Log.d("AppointmentsActivity", "✅ Successfully removed from appointments_active")
            
            // Remove from UI list immediately
            val index = appointmentsList.indexOfFirst { it.appointmentId == appointmentId }
            if (index != -1) {
                appointmentsList.removeAt(index)
                appointmentsAdapter?.submitList(appointmentsList.toList())
                updateEmptyState()
                Log.d("AppointmentsActivity", "✅ Removed from UI list, remaining: ${appointmentsList.size}")
            }
        }.addOnFailureListener { e ->
            Log.e("AppointmentsActivity", "❌ Error removing from appointments_active", e)
        }

        Log.d("AppointmentsActivity", "🗑️ Removing from: hospitals/$hospitalId/appointments/$appointmentId")
        hospitalRef.removeValue().addOnSuccessListener {
            Log.d("AppointmentsActivity", "✅ Successfully removed from hospital collection")
        }.addOnFailureListener { e ->
            Log.e("AppointmentsActivity", "❌ Error removing from hospital collection", e)
        }

        Log.d("AppointmentsActivity", "🗑️ Removing from: appointments/$appointmentId")
        mainRef.removeValue().addOnSuccessListener {
            Log.d("AppointmentsActivity", "✅ Successfully removed from main appointments collection")
        }.addOnFailureListener { e ->
            Log.e("AppointmentsActivity", "❌ Error removing from main collection", e)
        }
    }

    private fun updateEmptyState() {
        val hasAppointments = appointmentsList.isNotEmpty()
        Log.d("AppointmentsActivity", "🔄 Updating empty state - Has appointments: $hasAppointments, List size: ${appointmentsList.size}")

        if (hasAppointments) {
            Log.d("AppointmentsActivity", "✅ Showing appointments list")
            appointmentsList.forEach { appointment ->
                Log.d("AppointmentsActivity", "   - ${appointment.appointmentId}: ${appointment.appointmentDate} ${appointment.appointmentTime} (${appointment.status})")
            }
            updateDebugInfo("✅ Showing ${appointmentsList.size} appointments")
        } else {
            Log.d("AppointmentsActivity", "❌ No appointments to show - displaying empty state")
            updateDebugInfo("❌ No appointments found")
        }

        // Debug UI state before changes
        Log.d("AppointmentsActivity", "📱 UI state before update:")
        Log.d("AppointmentsActivity", "   - RecyclerView visibility: ${binding.rvAppointments.visibility}")
        Log.d("AppointmentsActivity", "   - No appointments text visibility: ${binding.tvNoAppointments.visibility}")

        binding.rvAppointments.visibility = if (hasAppointments) View.VISIBLE else View.GONE
        binding.tvNoAppointments.visibility = if (hasAppointments) View.GONE else View.VISIBLE

        // Debug UI state after changes
        Log.d("AppointmentsActivity", "📱 UI state after update:")
        Log.d("AppointmentsActivity", "   - RecyclerView visibility: ${binding.rvAppointments.visibility}")
        Log.d("AppointmentsActivity", "   - No appointments text visibility: ${binding.tvNoAppointments.visibility}")
        Log.d("AppointmentsActivity", "   - RecyclerView adapter: ${binding.rvAppointments.adapter != null}")
        Log.d("AppointmentsActivity", "   - RecyclerView child count: ${binding.rvAppointments.childCount}")
    }

    private fun setupViews() {
        binding.apply {
            // Setup FAB click listener for adding appointment slots
            fabAddSlot.setOnClickListener {
                startActivity(Intent(this@AppointmentsActivity, AddAppointmentSlotActivity::class.java))
            }

            // Add a debug button to force refresh appointments
            fabAddSlot.setOnLongClickListener {
                Log.d("AppointmentsActivity", "🔄 Manual refresh triggered by long press")
                updateDebugInfo("🔄 Manual refresh triggered")
                loadAppointments()
                true
            }

            // Setup debug info click to show more details
            tvDebugInfo.setOnClickListener {
                showDebugDetails()
            }
        }
    }

    private fun showDebugDetails() {
        val debugMessage = """
        🔍 DEBUG INFO:
        - User: ${auth.currentUser?.uid ?: "null"}
        - Appointments count: ${appointmentsList.size}
        - Adapter: ${if (appointmentsAdapter != null) "initialized" else "null"}
        - Listener: ${if (appointmentsListener != null) "active" else "null"}
        - RecyclerView: ${if (binding.rvAppointments.visibility == View.VISIBLE) "visible" else "hidden"}
        """.trimIndent()

        Log.d("AppointmentsActivity", debugMessage)
        updateDebugInfo("📊 Debug details clicked - check logs")

        // Check Firebase access
        checkFirebaseAccess()

        // Add test appointment to verify UI display is working
        Log.d("AppointmentsActivity", "🧪 Testing UI display with test appointment...")
        addTestAppointmentForUI()

        showToast("Check debug logs for details")
    }

    private fun addTestAppointmentForUI() {
        Log.d("AppointmentsActivity", "🧪 Adding test appointment to verify UI display")

        val testAppointment = com.example.blood_bud.data.model.Appointment(
            appointmentId = "test_ui_${System.currentTimeMillis()}",
            donorId = "test_donor_ui",
            donorName = "UI Test Donor",
            hospitalId = auth.currentUser?.uid ?: "",
            hospitalName = "UI Test Hospital",
            hospitalAddress = "UI Test Address",
            appointmentDate = "2025-10-28",
            appointmentTime = "11:00 AM",
            status = "pending",
            slotId = "test_slot_ui"
        )

        // Clear existing appointments and add test one
        appointmentsList.clear()
        appointmentsList.add(testAppointment)

        Log.d("AppointmentsActivity", "📋 Test appointment added to list - size: ${appointmentsList.size}")

        appointmentsAdapter?.submitList(appointmentsList.toList())

        Log.d("AppointmentsActivity", "📋 Test appointment submitted to adapter")

        updateEmptyState()

        // Check UI state after update
        binding.rvAppointments.postDelayed({
            Log.d("AppointmentsActivity", "🧪 UI Test Results:")
            Log.d("AppointmentsActivity", "   - RecyclerView visibility: ${binding.rvAppointments.visibility}")
            Log.d("AppointmentsActivity", "   - RecyclerView adapter: ${binding.rvAppointments.adapter != null}")
            Log.d("AppointmentsActivity", "   - RecyclerView child count: ${binding.rvAppointments.childCount}")
            Log.d("AppointmentsActivity", "   - RecyclerView width: ${binding.rvAppointments.width}, height: ${binding.rvAppointments.height}")

            if (binding.rvAppointments.childCount > 0) {
                Log.d("AppointmentsActivity", "✅ UI Test PASSED - RecyclerView has child views!")
                updateDebugInfo("✅ UI Test PASSED - check RecyclerView")
            } else {
                Log.e("AppointmentsActivity", "❌ UI Test FAILED - RecyclerView has no child views")
                updateDebugInfo("❌ UI Test FAILED - RecyclerView empty")
            }
        }, 200)

        Log.d("AppointmentsActivity", "✅ Test appointment added: ${testAppointment.appointmentId}")
        updateDebugInfo("🧪 Test appointment added - should show in list")
    }

    private fun addTestAppointment() {
        Log.d("AppointmentsActivity", "🧪 Adding test appointment to verify UI display")

        val testAppointment = com.example.blood_bud.data.model.Appointment(
            appointmentId = "test_appointment_${System.currentTimeMillis()}",
            donorId = "test_donor",
            donorName = "Test Donor",
            hospitalId = auth.currentUser?.uid ?: "",
            hospitalName = "Test Hospital",
            hospitalAddress = "Test Address",
            appointmentDate = "2025-10-28",
            appointmentTime = "10:00 AM",
            status = "pending",
            slotId = "test_slot"
        )

        appointmentsList.clear()
        appointmentsList.add(testAppointment)
        appointmentsAdapter?.submitList(appointmentsList.toList())
        updateEmptyState()

        Log.d("AppointmentsActivity", "✅ Test appointment added: ${testAppointment.appointmentId}")
        updateDebugInfo("🧪 Test appointment added - should show in list")
    }

    private fun checkFirebaseAccess() {
        Log.d("AppointmentsActivity", "🔐 Checking Firebase access...")
        updateDebugInfo("🔐 Checking Firebase access...")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("AppointmentsActivity", "❌ No authenticated user for Firebase access check")
            updateDebugInfo("❌ No authenticated user")
            return
        }

        Log.d("AppointmentsActivity", "🔍 User authenticated: ${currentUser.uid}")
        Log.d("AppointmentsActivity", "📧 User email: ${currentUser.email}")

        // Test read access to both collections
        val hospitalRef = realtimeDb.getReference("hospitals").child(currentUser.uid).child("appointments")
        val mainRef = realtimeDb.getReference("appointments")

        Log.d("AppointmentsActivity", "📡 Testing hospital collection access...")
        updateDebugInfo("📡 Testing hospital collection...")

        hospitalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("AppointmentsActivity", "✅ Hospital collection access successful")
                Log.d("AppointmentsActivity", "📊 Hospital collection has ${snapshot.childrenCount} items")
                Log.d("AppointmentsActivity", "📋 Hospital data: ${snapshot.value}")

                if (snapshot.childrenCount > 0) {
                    Log.d("AppointmentsActivity", "🎯 Found ${snapshot.childrenCount} appointments in hospital collection!")
                    updateDebugInfo("✅ Hospital collection: ${snapshot.childrenCount} appointments found")
                } else {
                    Log.d("AppointmentsActivity", "⚠️ Hospital collection is empty")
                    updateDebugInfo("⚠️ Hospital collection empty")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AppointmentsActivity", "❌ Hospital collection access denied: ${error.message}")
                updateDebugInfo("❌ Hospital access denied: ${error.message}")
            }
        })

        Log.d("AppointmentsActivity", "📡 Testing main appointments collection access...")
        updateDebugInfo("📡 Testing main collection...")

        mainRef.orderByChild("hospitalId").equalTo(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("AppointmentsActivity", "✅ Main appointments collection access successful")
                    Log.d("AppointmentsActivity", "📊 Main collection has ${snapshot.childrenCount} items for hospital")
                    Log.d("AppointmentsActivity", "📋 Main data: ${snapshot.value}")

                    if (snapshot.childrenCount > 0) {
                        Log.d("AppointmentsActivity", "🎯 Found ${snapshot.childrenCount} appointments in main collection!")
                        updateDebugInfo("✅ Main collection: ${snapshot.childrenCount} appointments found")
                    } else {
                        Log.d("AppointmentsActivity", "⚠️ Main collection is empty for this hospital")
                        updateDebugInfo("⚠️ Main collection empty")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppointmentsActivity", "❌ Main appointments collection access denied: ${error.message}")
                    updateDebugInfo("❌ Main collection access denied: ${error.message}")
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        appointmentsListener?.let { listener ->
            // Remove listeners from both hospital and main collections
            realtimeDb.getReference("hospitals").removeEventListener(listener)
            realtimeDb.getReference("appointments").removeEventListener(listener)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
