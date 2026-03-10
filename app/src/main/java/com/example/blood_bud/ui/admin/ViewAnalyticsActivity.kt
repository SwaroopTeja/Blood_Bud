package com.example.blood_bud.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ActivityViewAnalyticsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class ViewAnalyticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewAnalyticsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupToolbar()
        loadAnalytics()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Analytics"
        }
    }

    private fun loadAnalytics() {
        binding.progressBar.visibility = View.VISIBLE
        
        var totalDonations = 0
        var pendingAppointments = 0
        var completedDonations = 0
        var activeDonors = 0
        var totalHospitals = 0
        
        // Load completed donations from history
        database.getReference("appointments_history")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (appointmentSnapshot in snapshot.children) {
                        val status = appointmentSnapshot.child("status").getValue(String::class.java)
                        if (status == "completed") {
                            completedDonations++
                        }
                    }
                    
                    // Load pending appointments
                    loadPendingAppointments { pending ->
                        pendingAppointments = pending
                        totalDonations = completedDonations + pendingAppointments
                        
                        // Load user counts
                        loadUserCounts { donors, hospitals ->
                            activeDonors = donors
                            totalHospitals = hospitals
                            
                            // Update UI
                            updateUI(totalDonations, activeDonors, pendingAppointments, totalHospitals, completedDonations)
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ViewAnalytics", "Error loading history", error.toException())
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    private fun loadPendingAppointments(callback: (Int) -> Unit) {
        database.getReference("appointments_active")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var count = 0
                    for (appointmentSnapshot in snapshot.children) {
                        val status = appointmentSnapshot.child("status").getValue(String::class.java)
                        if (status == "pending") {
                            count++
                        }
                    }
                    callback(count)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ViewAnalytics", "Error loading pending", error.toException())
                    callback(0)
                }
            })
    }

    private fun loadUserCounts(callback: (Int, Int) -> Unit) {
        var donorCount = 0
        var hospitalCount = 0
        
        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val userType = document.getString("userType") ?: ""
                    when (userType.lowercase()) {
                        "donor" -> donorCount++
                        "hospital" -> hospitalCount++
                    }
                }
                callback(donorCount, hospitalCount)
            }
            .addOnFailureListener { e ->
                Log.e("ViewAnalytics", "Error loading users", e)
                callback(0, 0)
            }
    }

    private fun updateUI(totalDonations: Int, activeDonors: Int, pending: Int, hospitals: Int, completed: Int) {
        binding.tvTotalDonations.text = totalDonations.toString()
        binding.tvActiveDonors.text = activeDonors.toString()
        binding.tvPendingAppointments.text = pending.toString()
        binding.tvTotalHospitals.text = hospitals.toString()
        
        val summary = buildString {
            appendLine("📊 System Statistics")
            appendLine()
            appendLine("✅ Completed Donations: $completed")
            appendLine("⏳ Pending Appointments: $pending")
            appendLine("👥 Registered Donors: $activeDonors")
            appendLine("🏥 Registered Hospitals: $hospitals")
            appendLine()
            appendLine("The blood donation system is actively connecting donors with hospitals to save lives.")
        }
        
        binding.tvAnalyticsSummary.text = summary
        
        Log.d("ViewAnalytics", "Analytics loaded - Total: $totalDonations, Donors: $activeDonors, Hospitals: $hospitals")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
