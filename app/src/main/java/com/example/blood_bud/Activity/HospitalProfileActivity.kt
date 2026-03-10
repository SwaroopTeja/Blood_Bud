package com.example.blood_bud.Activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.app.AlertDialog
import android.app.ProgressDialog
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.example.blood_bud.R
import com.example.blood_bud.Activity.AppointmentsActivity
import com.example.blood_bud.Activity.HospitalDashboardActivity
import com.example.blood_bud.base.BaseHospitalActivity
import com.example.blood_bud.data.model.Hospital
import com.example.blood_bud.data.model.LocationData
import com.example.blood_bud.databinding.ActivityHospitalProfileBinding
import com.example.blood_bud.databinding.DialogChangePasswordBinding
import com.example.blood_bud.utils.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HospitalProfileActivity : BaseHospitalActivity<ActivityHospitalProfileBinding>(), View.OnClickListener {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val locationData = LocationData.indianLocations
    private var selectedState: String = ""
    private var selectedCity: String = ""
    private var hospitalId: String = ""
    private lateinit var stateAdapter: ArrayAdapter<String>
    private lateinit var cityAdapter: ArrayAdapter<String>

    override fun getViewBinding(): ActivityHospitalProfileBinding {
        return ActivityHospitalProfileBinding.inflate(layoutInflater)
    }

    override fun onResume() {
        super.onResume()
        // Ensure the correct navigation item is selected
        binding.root.findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId =
            R.id.nav_profile
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            finish()
            return
        }

        db = FirebaseFirestore.getInstance()

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Hospital Profile"

        // Initialize dropdowns and setup state/city selection
        setupStateAndCitySelection()

        // Set up click listeners
        setupClickListeners()

        // Set up bottom navigation
        setupBottomNavigation()

        // Load hospital data
        loadHospitalData()
    }

    private fun setupStateAndCitySelection() {
        try {
            Log.d("HospitalProfile", "Setting up state and city selection")

            // Set up state selection
            binding.etState.setOnClickListener {
                showStateSelectionDialog()
            }

            // Set up city selection (initially disabled until state is selected)
            binding.etCity.setOnClickListener {
                if (selectedState.isNotEmpty()) {
                    showCitySelectionDialog()
                } else {
                    showToast("Please select a state first")
                }
            }

            // Enable pincode field
            binding.etPincode.isEnabled = true
            binding.etPincode.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            binding.etPincode.setFilters(arrayOf(InputFilter.LengthFilter(6)))

            Log.d("HospitalProfile", "State and city selection setup complete")
        } catch (e: Exception) {
            Log.e("HospitalProfile", "Error setting up state and city selection", e)
        }
    }

    private fun showStateSelectionDialog() {
        val states = locationData.states.map { it.name }.toTypedArray()
        val checkedItem = states.indexOfFirst { it == selectedState }.takeIf { it >= 0 } ?: -1

        MaterialAlertDialogBuilder(this)
            .setTitle("Select State")
            .setSingleChoiceItems(states, checkedItem) { dialog, which ->
                selectedState = states[which]
                binding.etState.setText(selectedState)
                selectedCity = "" // Reset city when state changes
                binding.etCity.text?.clear()
                dialog.dismiss()
                // Enable city selection after state is selected
                binding.etCity.isEnabled = true
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCitySelectionDialog() {
        val state = locationData.states.find { it.name.equals(selectedState, true) }
        val cities = state?.cities?.toTypedArray() ?: emptyArray()
        val checkedItem = cities.indexOfFirst { it == selectedCity }.takeIf { it >= 0 } ?: -1

        MaterialAlertDialogBuilder(this)
            .setTitle("Select City in $selectedState")
            .setSingleChoiceItems(cities, checkedItem) { dialog, which ->
                selectedCity = cities[which]
                binding.etCity.setText(selectedCity)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCitiesAdapter() {
        // This method is no longer needed with the new dialog-based approach
        // The city selection is now handled directly in showCitySelectionDialog()
    }

    private fun setupClickListeners() {
        // Update Profile Button
        binding.btnUpdateProfile.setOnClickListener {
            if (validateFormInputs()) {
                updateHospitalProfile()
            }
        }

        // Change Password Button
        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Logout Button
        binding.btnLogout.setOnClickListener(this)
    }

    override fun setupBottomNavigation() {
        // Initialize bottom navigation
        val bottomNav = binding.root.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.let { nav ->
            nav.menu.clear()
            nav.inflateMenu(R.menu.hospital_bottom_nav_menu)
            nav.selectedItemId = R.id.nav_profile

            nav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        if (this::class != HospitalDashboardActivity::class) {
                            startActivity(
                                Intent(
                                    this,
                                    HospitalDashboardActivity::class.java
                                ).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                })
                            finish()
                        }
                        true
                    }

                    R.id.nav_profile -> {
                        // Already on profile
                        true
                    }

                    R.id.nav_appointments -> {
                        if (this::class != AppointmentsActivity::class) {
                            startActivity(Intent(this, AppointmentsActivity::class.java).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            })
                            finish()
                        }
                        true
                    }

                    R.id.nav_history -> {
                        if (this::class != HospitalHistoryActivity::class) {
                            startActivity(Intent(this, HospitalHistoryActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            })
                            finish()
                        }
                        true
                    }

                    R.id.nav_request_blood -> {
                        // TODO: Implement request blood
                        showToast("Request blood feature coming soon")
                        false
                    }

                    else -> false
                }
            }
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnUpdateProfile -> updateHospitalProfile()
            R.id.btnChangePassword -> showChangePasswordDialog()
            R.id.btnLogout -> showLogoutConfirmation()
        }
    }

    private fun loadHospitalData() {
        Log.d("HospitalProfile", "Loading hospital data...")

        val currentUser = auth.currentUser ?: run {
            Log.e("HospitalProfile", "No current user found")
            return
        }

        db.collection("hospitals")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("HospitalProfile", "Document data: ${document.data}")

                    try {
                        // Get address map
                        val addressMap =
                            document.get("address") as? Map<*, *> ?: emptyMap<String, Any>()
                        val street = addressMap["street"]?.toString() ?: ""
                        val city = addressMap["city"]?.toString() ?: ""
                        val state = addressMap["state"]?.toString() ?: ""
                        val pincode = addressMap["postalCode"]?.toString() ?: ""

                        runOnUiThread {
                            // Update UI with hospital data
                            binding.tvHospitalName.text = document.getString("name") ?: ""
                            binding.tvHospitalEmail.text = document.getString("email") ?: ""
                            binding.tvHospitalPhone.text = document.getString("phone") ?: ""
                            binding.etAddress.setText(street)
                            binding.etPincode.setText(pincode)

                            // Handle state and city selection
                            if (state.isNotEmpty()) {
                                binding.etState.setText(state)
                                selectedState = state

                                // Set city after a small delay to ensure adapter is ready
                                binding.etCity.postDelayed({
                                    if (city.isNotEmpty()) {
                                        binding.etCity.setText(city)
                                        selectedCity = city
                                    }

                                    // Ensure fields remain enabled
                                    binding.etState.isEnabled = true
                                    binding.etCity.isEnabled = true
                                    binding.etPincode.isEnabled = true
                                }, 300)
                            } else {
                                binding.etCity.setText(city)
                                binding.etState.isEnabled = true
                                binding.etCity.isEnabled = true
                                binding.etPincode.isEnabled = true
                            }

                            Log.d(
                                "HospitalProfile",
                                "UI updated - Street: '$street', City: '$city', State: '$state', Pincode: '$pincode'"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("HospitalProfile", "Error processing document data", e)
                        showToast("Error processing profile data")
                    }
                } else {
                    Log.d("HospitalProfile", "No document found for user: ${currentUser.uid}")
                    // No existing data, initialize with user info
                    runOnUiThread {
                        binding.tvHospitalName.text = currentUser.displayName ?: ""
                        binding.tvHospitalEmail.text = currentUser.email ?: ""
                        binding.tvHospitalPhone.text = currentUser.phoneNumber ?: ""

                        // Enable all fields for new profile
                        binding.etState.isEnabled = true
                        binding.etCity.isEnabled = true
                        binding.etPincode.isEnabled = true
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HospitalProfile", "Error getting document", e)
                showToast("Failed to load hospital data: ${e.message}")
            }
    }

    private fun updateHospitalProfile() {
        if (!validateFormInputs()) {
            return
        }

        showProgressDialog("Updating profile...")

        val address = binding.etAddress.text.toString().trim()
        val pincode = binding.etPincode.text.toString().trim()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            hideProgressDialog()
            showToast("User not authenticated")
            return
        }

        val hospitalData = hashMapOf(
            "address" to hashMapOf(
                "street" to address,
                "city" to selectedCity,
                "state" to selectedState,
                "postalCode" to pincode
            ),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        // Use set() with merge to handle both new and existing documents
        db.collection("hospitals").document(currentUser.uid)
            .set(hospitalData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                hideProgressDialog()
                showToast("Profile updated successfully")
                // Reload the data to ensure UI is in sync
                loadHospitalData()
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                showToast("Failed to update profile: ${e.message}")
                Log.e("HospitalProfile", "Error updating profile", e)
            }
    }

    private fun validateFormInputs(): Boolean {
        var isValid = true

        // Get current selections
        selectedState = binding.etState.text.toString().trim()
        selectedCity = binding.etCity.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val pincode = binding.etPincode.text.toString().trim()

        // Validate state and city
        if (selectedState.isEmpty()) {
            binding.tilState.error = "Please select a state"
            isValid = false
        } else {
            binding.tilState.error = null
        }

        if (selectedCity.isEmpty()) {
            binding.tilCity.error = "Please select a city"
            isValid = false
        } else {
            binding.tilCity.error = null
        }

        // Validate address
        if (address.isEmpty()) {
            binding.tilAddress.error = "Address is required"
            isValid = false
        } else {
            binding.tilAddress.error = null
        }

        // Validate pincode
        if (pincode.isEmpty()) {
            binding.tilPincode.error = "Pincode is required"
            isValid = false
        } else if (pincode.length != 6 || !pincode.matches(Regex("\\d+"))) {
            binding.tilPincode.error = "Please enter a valid 6-digit pincode"
            isValid = false
        } else {
            binding.tilPincode.error = null
        }

        return isValid
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, com.example.blood_bud.Activity.AccountActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private var progressDialog: ProgressDialog? = null

    private fun showProgressDialog(message: String) {
        hideProgressDialog()
        progressDialog = ProgressDialog(this).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val binding = DialogChangePasswordBinding.bind(dialogView)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val currentPassword = binding.etCurrentPassword.text.toString()
                val newPassword = binding.etNewPassword.text.toString()
                val confirmPassword = binding.etConfirmPassword.text.toString()

                when {
                    newPassword.length < 6 -> {
                        binding.tilNewPassword.error = "Password must be at least 6 characters"
                    }

                    newPassword != confirmPassword -> {
                        binding.tilNewPassword.error = "Passwords do not match"
                    }

                    else -> {
                        binding.tilNewPassword.error = null
                        changePassword(currentPassword, newPassword)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        showProgressDialog("Changing password...")

        val user = auth.currentUser
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
            user?.email ?: "",
            currentPassword
        )

        user?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                user.updatePassword(newPassword)
                    .addOnCompleteListener { updateTask ->
                        hideProgressDialog()
                        if (updateTask.isSuccessful) {
                            showToast("Password changed successfully")
                        } else {
                            showToast("Failed to change password: ${updateTask.exception?.message}")
                        }
                    }
            } else {
                hideProgressDialog()
                showToast("Current password is incorrect")
            }
        }?.addOnFailureListener {
            hideProgressDialog()
            showToast("Authentication failed: ${it.message}")
        }
    }

    companion object {
        private const val TAG = "HospitalProfile"
    }
}
