package com.example.blood_bud.Activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ActivityEditDonorProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class EditDonorProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEditDonorProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDonorProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        setupToolbar()
        setupBloodTypeDropdown()
        setupDatePicker()
        loadUserData()
        
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
        
        binding.btnChangePhoto.setOnClickListener {
            Toast.makeText(this, "Photo upload feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"
    }
    
    private fun setupBloodTypeDropdown() {
        val bloodTypes = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodTypes)
        binding.spinnerBloodType.setAdapter(adapter)
    }
    
    private fun setupDatePicker() {
        binding.etDateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val date = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    binding.etDateOfBirth.setText(date)
                },
                year,
                month,
                day
            )
            
            // Set max date to 18 years ago (minimum age for blood donation)
            calendar.add(Calendar.YEAR, -18)
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis
            
            datePickerDialog.show()
        }
    }
    
    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    binding.etName.setText(document.getString("name") ?: "")
                    binding.etEmail.setText(document.getString("email") ?: "")
                    binding.etPhone.setText(document.getString("phone") ?: "")
                    binding.spinnerBloodType.setText(document.getString("bloodType") ?: "", false)
                    binding.etDateOfBirth.setText(document.getString("dateOfBirth") ?: "")
                    // Address in Firestore may be stored as a Map, not a String
                    val addressValue = document.get("address")
                    val addressText = when (addressValue) {
                        is String -> addressValue
                        is Map<*, *> -> {
                            val street = addressValue["street"]?.toString()?.takeIf { it.isNotBlank() }
                            val city = addressValue["city"]?.toString()?.takeIf { it.isNotBlank() }
                            val state = addressValue["state"]?.toString()?.takeIf { it.isNotBlank() }
                            val postal = addressValue["postalCode"]?.toString()?.takeIf { it.isNotBlank() }
                            listOfNotNull(street, city, state, postal).joinToString(", ")
                        }
                        else -> document.getString("addressLine") ?: ""
                    }
                    binding.etAddress.setText(addressText)
                    
                    val photoUrl = document.getString("photoUrl")
                    if (!photoUrl.isNullOrBlank()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.ivProfile)
                    }
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e("EditDonorProfile", "Error loading user data", e)
                Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }
    
    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val bloodType = binding.spinnerBloodType.text.toString().trim()
        val dateOfBirth = binding.etDateOfBirth.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        
        // Validation
        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return
        }
        
        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone number is required"
            return
        }
        
        if (bloodType.isEmpty()) {
            Toast.makeText(this, "Please select blood type", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = auth.currentUser?.uid ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSaveProfile.isEnabled = false
        
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "bloodType" to bloodType,
            "dateOfBirth" to dateOfBirth,
            // Do not overwrite structured 'address' map; store free-form address separately
            "addressLine" to address,
            "updatedAt" to System.currentTimeMillis()
        )
        
        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("EditDonorProfile", "Profile updated successfully")
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("EditDonorProfile", "Error updating profile", e)
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.btnSaveProfile.isEnabled = true
            }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
