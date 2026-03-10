package com.example.blood_bud.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blood_bud.R
import com.example.blood_bud.base.BaseHospitalActivity
import com.example.blood_bud.databinding.ActivityHospitalHistoryBinding
import com.example.blood_bud.ui.hospital.HospitalAppointmentAdapter
import com.example.blood_bud.utils.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HospitalHistoryActivity : BaseHospitalActivity<ActivityHospitalHistoryBinding>() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var historyAdapter: HospitalAppointmentAdapter
    private val historyList = mutableListOf<com.example.blood_bud.data.model.Appointment>()
    private var currentFilter = "all" // all, completed, cancelled

    override fun getViewBinding() = ActivityHospitalHistoryBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("HospitalHistory", "=== HOSPITAL HISTORY ACTIVITY STARTED ===")

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupFilterButtons()
        loadHistory()

        // Set up bottom navigation
        setupBottomNavigation(R.id.bottomNavigation)
    }

    override fun onResume() {
        super.onResume()
        // Ensure the correct navigation item is selected
        binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.nav_history
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Donation History"
    }

    private fun setupRecyclerView() {
        historyAdapter = HospitalAppointmentAdapter(
            context = this@HospitalHistoryActivity,
            onStatusUpdate = { _, _ -> 
                // No status updates in history - read-only
            }
        )

        binding.rvHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@HospitalHistoryActivity)
            setHasFixedSize(false)
        }
    }

    private fun setupFilterButtons() {
        binding.btnFilterAll.setOnClickListener {
            setFilter("all")
        }
        
        binding.btnFilterCompleted.setOnClickListener {
            setFilter("completed")
        }
        
        binding.btnFilterCancelled.setOnClickListener {
            setFilter("cancelled")
        }
        
        // Set initial filter
        setFilter("all")
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        
        // Update button states
        binding.btnFilterAll.apply {
            if (filter == "all") {
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.white))
            } else {
                setBackgroundColor(getColor(R.color.white))
                setTextColor(getColor(R.color.primary))
            }
        }
        
        binding.btnFilterCompleted.apply {
            if (filter == "completed") {
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.white))
            } else {
                setBackgroundColor(getColor(R.color.white))
                setTextColor(getColor(R.color.primary))
            }
        }
        
        binding.btnFilterCancelled.apply {
            if (filter == "cancelled") {
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.white))
            } else {
                setBackgroundColor(getColor(R.color.white))
                setTextColor(getColor(R.color.primary))
            }
        }
        
        // Reload history with filter
        loadHistory()
    }

    private fun loadHistory() {
        val hospitalId = auth.currentUser?.uid
        
        if (hospitalId == null) {
            Log.e("HospitalHistory", "No hospital ID - user not logged in")
            showToast("Please log in to view history")
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        historyList.clear()

        Log.d("HospitalHistory", "Loading history for hospital: $hospitalId, filter: $currentFilter")

        // Load from appointments_history
        database.getReference("appointments_history")
            .orderByChild("hospitalId")
            .equalTo(hospitalId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    historyList.clear()
                    
                    Log.d("HospitalHistory", "Found ${snapshot.childrenCount} history records")
                    
                    for (appointmentSnapshot in snapshot.children) {
                        try {
                            val appointment = appointmentSnapshot.getValue(com.example.blood_bud.data.model.Appointment::class.java)
                            appointment?.let {
                                // Apply filter
                                val shouldInclude = when (currentFilter) {
                                    "completed" -> it.status == "completed"
                                    "cancelled" -> it.status == "cancelled"
                                    else -> it.status in listOf("completed", "cancelled")
                                }
                                
                                if (shouldInclude) {
                                    historyList.add(it)
                                    Log.d("HospitalHistory", "Added: ${it.appointmentId} - ${it.donorName} - ${it.status}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HospitalHistory", "Error parsing appointment", e)
                        }
                    }
                    
                    // Sort by updatedAt descending (most recent first)
                    historyList.sortByDescending { it.updatedAt }
                    
                    historyAdapter.submitList(historyList.toList())
                    updateEmptyState()
                    binding.progressBar.visibility = View.GONE
                    
                    Log.d("HospitalHistory", "Loaded ${historyList.size} appointments")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HospitalHistory", "Error loading history", error.toException())
                    binding.progressBar.visibility = View.GONE
                    showToast("Error loading history")
                    updateEmptyState()
                }
            })
    }

    private fun updateEmptyState() {
        if (historyList.isEmpty()) {
            binding.rvHistory.visibility = View.GONE
            binding.tvNoHistory.visibility = View.VISIBLE
            
            binding.tvNoHistory.text = when (currentFilter) {
                "completed" -> "No completed donations"
                "cancelled" -> "No cancelled appointments"
                else -> "No donation history"
            }
        } else {
            binding.rvHistory.visibility = View.VISIBLE
            binding.tvNoHistory.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
