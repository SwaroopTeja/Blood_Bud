package com.example.blood_bud.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ActivityViewDonationsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ViewDonationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewDonationsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var donationsAdapter: DonationsAdapter
    private val allDonations = mutableListOf<Donation>()
    private var currentFilter = "all" // all, pending, completed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewDonationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupFilterButtons()
        loadDonations()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "View Donations"
    }

    private fun setupRecyclerView() {
        donationsAdapter = DonationsAdapter()
        binding.donationsRecyclerView.apply {
            adapter = donationsAdapter
            layoutManager = LinearLayoutManager(this@ViewDonationsActivity)
        }
    }

    private fun setupFilterButtons() {
        binding.filterAllButton.setOnClickListener {
            setFilter("all")
        }
        
        binding.filterPendingButton.setOnClickListener {
            setFilter("pending")
        }
        
        binding.filterCompletedButton.setOnClickListener {
            setFilter("completed")
        }
        
        // Set initial filter
        setFilter("all")
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        
        // Update button states
        binding.filterAllButton.apply {
            if (filter == "all") {
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.white))
            } else {
                setBackgroundColor(getColor(R.color.white))
                setTextColor(getColor(R.color.primary))
            }
        }
        
        binding.filterPendingButton.apply {
            if (filter == "pending") {
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.white))
            } else {
                setBackgroundColor(getColor(R.color.white))
                setTextColor(getColor(R.color.primary))
            }
        }
        
        binding.filterCompletedButton.apply {
            if (filter == "completed") {
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.white))
            } else {
                setBackgroundColor(getColor(R.color.white))
                setTextColor(getColor(R.color.primary))
            }
        }
        
        applyFilter()
    }

    private fun loadDonations() {
        binding.progressBar.visibility = View.VISIBLE
        allDonations.clear()

        Log.d("ViewDonations", "Loading all donations from appointments_active and appointments_history")

        // Load from appointments_active (pending/confirmed)
        database.getReference("appointments_active")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (appointmentSnapshot in snapshot.children) {
                        try {
                            val donation = parseDonation(appointmentSnapshot)
                            donation?.let { allDonations.add(it) }
                        } catch (e: Exception) {
                            Log.e("ViewDonations", "Error parsing active appointment", e)
                        }
                    }
                    
                    // After loading active, load history
                    loadDonationsHistory()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ViewDonations", "Error loading active appointments", error.toException())
                    loadDonationsHistory()
                }
            })
    }

    private fun loadDonationsHistory() {
        database.getReference("appointments_history")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (appointmentSnapshot in snapshot.children) {
                        try {
                            val donation = parseDonation(appointmentSnapshot)
                            donation?.let { allDonations.add(it) }
                        } catch (e: Exception) {
                            Log.e("ViewDonations", "Error parsing history appointment", e)
                        }
                    }
                    
                    // Sort by date descending (most recent first)
                    allDonations.sortByDescending { it.timestamp }
                    
                    applyFilter()
                    binding.progressBar.visibility = View.GONE
                    
                    Log.d("ViewDonations", "Loaded ${allDonations.size} total donations")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ViewDonations", "Error loading donation history", error.toException())
                    binding.progressBar.visibility = View.GONE
                    updateEmptyState()
                }
            })
    }

    private fun parseDonation(snapshot: DataSnapshot): Donation? {
        val donorName = snapshot.child("donorName").getValue(String::class.java) ?: "Unknown Donor"
        val hospitalName = snapshot.child("hospitalName").getValue(String::class.java) ?: "Unknown Hospital"
        val appointmentDate = snapshot.child("appointmentDate").getValue(String::class.java) ?: ""
        val status = snapshot.child("status").getValue(String::class.java) ?: "pending"
        val timestamp = snapshot.child("updatedAt").getValue(Long::class.java) 
            ?: snapshot.child("createdAt").getValue(Long::class.java) 
            ?: System.currentTimeMillis()
        
        return Donation(
            donorName = donorName,
            hospitalName = hospitalName,
            date = appointmentDate,
            status = status,
            timestamp = timestamp
        )
    }

    private fun applyFilter() {
        val filteredList = when (currentFilter) {
            "pending" -> allDonations.filter { it.status == "pending" }
            "completed" -> allDonations.filter { it.status == "completed" }
            else -> allDonations
        }
        
        donationsAdapter.submitList(filteredList)
        updateEmptyState()
        
        Log.d("ViewDonations", "Applied filter: $currentFilter, showing ${filteredList.size} donations")
    }

    private fun updateEmptyState() {
        val filteredList = when (currentFilter) {
            "pending" -> allDonations.filter { it.status == "pending" }
            "completed" -> allDonations.filter { it.status == "completed" }
            else -> allDonations
        }
        
        if (filteredList.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.donationsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.donationsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

data class Donation(
    val donorName: String = "",
    val hospitalName: String = "",
    val date: String = "",
    val status: String = "",
    val timestamp: Long = 0
)
