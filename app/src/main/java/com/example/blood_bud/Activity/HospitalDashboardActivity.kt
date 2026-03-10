package com.example.blood_bud.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blood_bud.Adapter.StockAdapter
import com.example.blood_bud.R
import com.example.blood_bud.base.BaseHospitalActivity
import com.example.blood_bud.data.model.Hospital
import com.example.blood_bud.data.model.StockItem
import com.example.blood_bud.databinding.ActivityHospitalDashboardBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HospitalDashboardActivity : BaseHospitalActivity<ActivityHospitalDashboardBinding>() {
    
    override fun setupBottomNavigation() {
        val bottomNav = binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.let { nav ->
            nav.menu.clear()
            nav.inflateMenu(R.menu.hospital_bottom_nav_menu)
            nav.selectedItemId = R.id.nav_home
            
            nav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        // Already on home
                        true
                    }
                    R.id.nav_profile -> {
                        if (this::class != HospitalProfileActivity::class) {
                            startActivity(Intent(this, HospitalProfileActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            })
                            finish()
                        }
                        true
                    }
                    R.id.nav_appointments -> {
                        if (this::class != AppointmentsActivity::class) {
                            startActivity(Intent(this, AppointmentsActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
    private var registration: ListenerRegistration? = null

    override fun getViewBinding() = ActivityHospitalDashboardBinding.inflate(layoutInflater)
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var stockAdapter: StockAdapter
    private var currentStockItem: StockItem? = null
    private val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    private var hospitalId: String = ""
    private var progressDialog: AlertDialog? = null

    companion object {
        private const val TAG = "HospitalDashboard"
    }

    private fun showProgress() {
        hideProgress()
        progressDialog = AlertDialog.Builder(this)
            .setView(layoutInflater.inflate(R.layout.progress_dialog, null))
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun hideProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        hospitalId = auth.currentUser?.uid ?: ""

        setupToolbar()
        setupStockManagement()  // This will set up RecyclerView and load data
        setupBloodTypeDropdown()
        setupClickListeners()
        loadHospitalData()
        loadAppointmentStatistics()
        
        // Set up bottom navigation
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Ensure the correct navigation item is selected
        binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_home
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupBloodTypeDropdown() {
        val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodTypes)
        (binding.tilBloodType.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        // Get references to views using their IDs
        val btnAddUpdate =
            binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddUpdate)
        val etBloodType = binding.root.findViewById<AutoCompleteTextView>(R.id.etBloodType)
        val etUnits =
            binding.root.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUnits)

        btnAddUpdate?.setOnClickListener {
            if (validateInputs()) {
                val bloodType = etBloodType?.text?.toString()?.trim() ?: ""
                val units = etUnits?.text?.toString()?.toIntOrNull() ?: 0

                val stockItem = currentStockItem?.copy(
                    bloodType = bloodType,
                    units = units,
                    lastUpdated = System.currentTimeMillis()
                ) ?: StockItem(
                    bloodType = bloodType,
                    units = units,
                    lastUpdated = System.currentTimeMillis()
                )

                saveStockItem(stockItem)
            }
        }
    }

    private fun loadHospitalData() {
        if (hospitalId.isEmpty()) {
            showToast("Error: No hospital ID found")
            return
        }

        showProgress()
        db.collection("hospitals")
            .document(hospitalId)
            .get()
            .addOnSuccessListener { document ->
                hideProgress()
                if (document != null && document.exists()) {
                    val hospital = document.toObject(Hospital::class.java)
                    hospital?.let {
                        binding.tvHospitalName.text = it.name
                        binding.tvWelcome.text = getString(R.string.welcome)
                    }
                } else {
                    showToast("No hospital data found")
                }
            }
            .addOnFailureListener { e ->
                hideProgress()
                showToast("Error loading hospital data")
                Log.e(TAG, "Error loading hospital data", e)
            }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun setupStockManagement() {
        Log.d(TAG, "Setting up stock management")
        
        try {
            // Setup blood type dropdown
            val bloodTypeAdapter =
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, StockItem.BLOOD_TYPES)
            (binding.tilBloodType.editText as? AutoCompleteTextView)?.apply {
                setAdapter(bloodTypeAdapter)
                setOnItemClickListener { _, _, position, _ ->
                    // Clear error when user selects an item
                    binding.tilBloodType.error = null
                }
            }

            // Setup units input validation
            binding.etUnits.addTextChangedListener {
                val units = it?.toString()?.toIntOrNull() ?: 0
                if (units < 0) {
                    binding.tilUnits.error = getString(R.string.error_positive_units)
                } else {
                    binding.tilUnits.error = null
                }
            }

            // Setup add/update button click
            binding.btnAddUpdate.setOnClickListener {
                if (validateInputs()) {
                    val bloodType = binding.etBloodType.text.toString().trim()
                    val units = binding.etUnits.text.toString().toIntOrNull() ?: 0

                    val stockItem = currentStockItem?.copy(
                        bloodType = bloodType,
                        units = units,
                        lastUpdated = System.currentTimeMillis()
                    ) ?: StockItem.create(bloodType, units)

                    saveStockItem(stockItem)
                }
            }

            // Setup RecyclerView first
            Log.d(TAG, "Setting up RecyclerView")
            setupRecyclerView()

            // Then load stock data after a short delay to ensure RecyclerView is ready
            binding.rvBloodStock.postDelayed({
                Log.d(TAG, "Loading stock data after RecyclerView setup")
                loadStockData()
            }, 100) // Small delay to ensure RecyclerView is fully initialized
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupStockManagement", e)
            showToast("Error initializing stock management")
        }
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        
        try {
            // Initialize the adapter
            stockAdapter = StockAdapter(
                onEditClick = { stockItem ->
                    Log.d(TAG, "Edit clicked for ${stockItem.bloodType}")
                    // Scroll to top
                    binding.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollView)
                        ?.smoothScrollTo(0, 0)

                    // Set current stock item and update UI
                    currentStockItem = stockItem

                    // Update UI with selected stock item
                    binding.etBloodType.setText(stockItem.bloodType, false)
                    binding.etUnits.setText(stockItem.units.toString())
                    binding.btnAddUpdate.text = getString(R.string.update_stock)
                    
                    Log.d(TAG, "Set up form for editing: ${stockItem.bloodType} (${stockItem.units} units)")
                },
                onDeleteClick = { stockItem ->
                    Log.d(TAG, "Delete clicked for ${stockItem.bloodType}")
                    showDeleteConfirmationDialog(stockItem)
                }
            )

            // Configure RecyclerView
            val recyclerView = binding.rvBloodStock
            recyclerView.apply {
                Log.d(TAG, "Initializing RecyclerView with adapter")
                
                // Ensure we have a stable ID for each item
                setHasFixedSize(true)
                
                // Set the layout manager
                layoutManager = LinearLayoutManager(this@HospitalDashboardActivity).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
                
                // Set the adapter
                adapter = stockAdapter
                
                // Add item decoration (divider between items)
                addItemDecoration(
                    DividerItemDecoration(
                        this@HospitalDashboardActivity,
                        DividerItemDecoration.VERTICAL
                    ).apply {
                        setDrawable(
                            ContextCompat.getDrawable(
                                this@HospitalDashboardActivity,
                                android.R.drawable.divider_horizontal_dark
                            ) ?: return@apply
                        )
                    }
                )
                
                // Log RecyclerView properties
                Log.d(TAG, "RecyclerView initialized - " +
                      "LayoutManager: ${layoutManager?.javaClass?.simpleName}, " +
                      "Adapter: ${adapter?.javaClass?.simpleName}, " +
                      "ItemDecoration: ${itemDecorationCount} decorations")
            }
            
            // Set initial empty state
            updateEmptyState(true) // Start with empty state until data is loaded
            
            Log.d(TAG, "RecyclerView setup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
            showToast("Error initializing the list view")
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val bloodType = binding.etBloodType.text.toString().trim()
        val units = binding.etUnits.text.toString().toIntOrNull()

        // Validate blood type
        if (bloodType.isEmpty() || !StockItem.BLOOD_TYPES.contains(bloodType)) {
            binding.tilBloodType.error = getString(R.string.blood_type_required)
            isValid = false
        } else {
            binding.tilBloodType.error = null
        }

        // Validate units
        when {
            units == null -> {
                binding.tilUnits.error = getString(R.string.valid_units_required)
                isValid = false
            }

            units < 0 -> {
                binding.tilUnits.error = getString(R.string.error_negative_units)
                isValid = false
            }

            units == 0 && currentStockItem != null -> {
                // Allow 0 when updating to delete the stock
                binding.tilUnits.error = null
            }

            units <= 0 -> {
                binding.tilUnits.error = getString(R.string.error_positive_units)
                isValid = false
            }

            else -> {
                binding.tilUnits.error = null
            }
        }

        return isValid
    }

    private fun saveStockItem(stockItem: StockItem) {
        if (hospitalId.isEmpty()) {
            showToast("Error: No hospital ID found")
            return
        }

        showProgress()

        // Get a reference to the hospital document
        val docRef = db.collection("hospitals").document(hospitalId)

        docRef.get().addOnSuccessListener { document ->
            try {
                val currentInventory =
                    (document.get("bloodInventory") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()

                // If this is an update, we replace the existing value
                // If this is a new item, we add to the existing value
                val currentUnits = (currentInventory[stockItem.bloodType] as? Number)?.toInt() ?: 0
                val newUnits =
                    if (currentStockItem != null) stockItem.units else currentUnits + stockItem.units

                // If units are 0, we'll remove the entry (delete operation)
                val updates = if (newUnits <= 0) {
                    hashMapOf<String, Any>(
                        "bloodInventory.${stockItem.bloodType}" to com.google.firebase.firestore.FieldValue.delete(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                } else {
                    hashMapOf(
                        "bloodInventory.${stockItem.bloodType}" to newUnits,
                        "updatedAt" to System.currentTimeMillis()
                    )
                }

                val isUpdate = currentStockItem != null
                val isDelete = newUnits <= 0

                docRef.update(updates)
                    .addOnSuccessListener {
                        // Reset the form
                        resetStockForm()

                        // Show appropriate message
                        val message = when {
                            isDelete -> getString(R.string.stock_removed, stockItem.bloodType)
                            isUpdate -> getString(R.string.stock_updated)
                            else -> getString(R.string.stock_added)
                        }

                        showToast(message)

                        // Hide keyboard
                        hideKeyboard()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating stock", e)
                        showToast(
                            getString(
                                R.string.error_updating_stock,
                                e.message ?: "Unknown error"
                            )
                        )
                    }
                    .addOnCompleteListener {
                        hideProgress()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing stock update", e)
                hideProgress()
                showToast(getString(R.string.error_processing_request))
            }
        }.addOnFailureListener { e ->
            hideProgress()
            Log.e(TAG, "Error loading current stock", e)
            showToast(getString(R.string.error_loading_stock))
        }
    }

    private fun resetStockForm() {
        currentStockItem = null

        // Get references to views
        val etBloodType = binding.root.findViewById<AutoCompleteTextView>(R.id.etBloodType)
        val etUnits =
            binding.root.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUnits)
        val btnAddUpdate =
            binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddUpdate)
        val tilBloodType =
            binding.root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilBloodType)
        val tilUnits =
            binding.root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilUnits)

        // Update UI
        etBloodType?.text?.clear()
        etUnits?.text?.clear()
        btnAddUpdate?.text = getString(R.string.add_stock)
        tilBloodType?.error = null
        tilUnits?.error = null

        // Set focus back to blood type for new entry
        etBloodType?.requestFocus()
    }

    private fun showDeleteConfirmationDialog(stockItem: StockItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_stock_title))
            .setMessage(getString(R.string.delete_stock_message, stockItem.bloodType))
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteStockItem(stockItem)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteStockItem(stockItem: StockItem) {
        if (hospitalId.isEmpty()) {
            showToast("Error: No hospital ID found")
            return
        }

        showProgress()
        val updates = hashMapOf<String, Any>(
            "bloodInventory.${stockItem.bloodType}" to com.google.firebase.firestore.FieldValue.delete(),
            "updatedAt" to System.currentTimeMillis()
        )
        
        db.collection("hospitals")
            .document(hospitalId)
            .update(updates)
            .addOnSuccessListener {
                hideProgress()
                showToast("${stockItem.bloodType} stock removed successfully")
                // Force refresh the data
                loadStockData()
            }
            .addOnFailureListener { e ->
                hideProgress()
                showToast("Error deleting stock item")
                Log.e(TAG, "Error deleting stock item", e)
            }
    }

    private fun loadStockData() {
        Log.d(TAG, "loadStockData() called for hospital: $hospitalId")
        
        if (hospitalId.isBlank()) {
            val errorMsg = "Hospital ID is blank, cannot load stock data"
            Log.e(TAG, errorMsg)
            runOnUiThread {
                showToast(errorMsg)
                updateEmptyState(true)
            }
            return
        }

        // Show loading indicator
        showProgress()
        Log.d(TAG, "Showing progress dialog")

        try {
            // First, check if the document exists
            Log.d(TAG, "Checking if hospital document exists: $hospitalId")
            db.collection("hospitals").document(hospitalId).get()
                .addOnSuccessListener { snapshot ->
                    Log.d(TAG, "Document check completed. Exists: ${snapshot.exists()}")
                    hideProgress()
                    
                    if (snapshot.exists()) {
                        Log.d(TAG, "Hospital document exists, setting up real-time listener")
                        // Document exists, set up real-time listener
                        setupStockDataListener()
                    } else {
                        Log.d(TAG, "Hospital document does not exist, showing empty state")
                        // Document doesn't exist, show empty state
                        runOnUiThread {
                            updateEmptyState(true)
                            showToast(getString(R.string.hospital_data_not_found))
                        }
                    }
                }
                .addOnFailureListener { e ->
                    val errorMsg = "Error checking if hospital document exists: ${e.message}"
                    Log.e(TAG, errorMsg, e)
                    runOnUiThread {
                        hideProgress()
                        updateEmptyState(true)
                        showToast(getString(R.string.error_loading_data))
                    }
                }
        } catch (e: Exception) {
            val errorMsg = "Exception in loadStockData: ${e.message}"
            Log.e(TAG, errorMsg, e)
            runOnUiThread {
                hideProgress()
                updateEmptyState(true)
                showToast(getString(R.string.error_loading_data))
            }
        }
    }

    private fun setupStockDataListener() {
        Log.d(TAG, "setupStockDataListener() called")
        
        // Remove any existing listener to avoid duplicates
        registration?.remove()
        
        try {
            Log.d(TAG, "Creating new Firestore snapshot listener")
            registration = db.collection("hospitals").document(hospitalId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        val errorMsg = "Firestore listen failed: ${e.message}"
                        Log.e(TAG, errorMsg, e)
                        runOnUiThread {
                            updateEmptyState(true)
                            showToast(getString(R.string.error_loading_data))
                        }
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.d(TAG, "Received snapshot with ID: ${snapshot.id}")
                        Log.d(TAG, "Snapshot data: ${snapshot.data}")
                        updateStockListFromSnapshot(snapshot)
                    } else {
                        Log.d(TAG, "Snapshot is null or document doesn't exist")
                        runOnUiThread {
                            updateEmptyState(true)
                        }
                    }
                }
            
            Log.d(TAG, "Firestore listener registered successfully")
        } catch (e: Exception) {
            val errorMsg = "Exception in setupStockDataListener: ${e.message}"
            Log.e(TAG, errorMsg, e)
            runOnUiThread {
                updateEmptyState(true)
                showToast(getString(R.string.error_loading_data))
            }
        }
    }

    private fun updateStockListFromSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot) {
        try {
            Log.d(TAG, "updateStockListFromSnapshot called")
            
            // Log the entire snapshot data for debugging
            val data = snapshot.data
            Log.d(TAG, "Snapshot data: $data")
            
            if (data == null) {
                Log.d(TAG, "No data in snapshot")
                runOnUiThread {
                    updateEmptyState(true)
                }
                return
            }

            // Log all fields in the document to help with debugging
            data.keys.forEach { key ->
                Log.d(TAG, "Document field - $key: ${data[key]}")
            }

            // Get the bloodInventory map from the document
            val bloodInventory = data["bloodInventory"] as? Map<String, Any> ?: run {
                Log.d(TAG, "No bloodInventory field in document. Available fields: ${data.keys}")
                runOnUiThread {
                    updateEmptyState(true)
                    showToast("No blood inventory data found")
                }
                return
            }

            Log.d(TAG, "Blood inventory map: $bloodInventory")

            // If the map is empty, show empty state
            if (bloodInventory.isEmpty()) {
                Log.d(TAG, "Blood inventory is empty")
                runOnUiThread {
                    updateEmptyState(true)
                }
                return
            }

            // Convert the map to a list of StockItem objects
            val stockList = mutableListOf<StockItem>()
            
            bloodInventory.forEach { (bloodType, units) ->
                try {
                    // Skip if the blood type is not in our valid list
                    if (bloodType !in StockItem.BLOOD_TYPES) {
                        Log.w(TAG, "Skipping invalid blood type: $bloodType")
                        return@forEach
                    }
                    
                    val unitsValue = when (units) {
                        is Number -> units.toInt()
                        is String -> units.toIntOrNull() ?: 0
                        is Boolean -> if (units) 1 else 0
                        else -> 0
                    }
                    
                    // Only add if units are positive
                    if (unitsValue > 0) {
                        // Create a new StockItem and add it to the list
                        val stockItem = StockItem.create(bloodType, unitsValue)
                        stockList.add(stockItem)
                        Log.d(TAG, "Added stock item: $stockItem")
                    } else {
                        Log.d(TAG, "Skipping $bloodType with zero or negative units: $unitsValue")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing blood type $bloodType with value $units", e)
                }
            }

            // Sort the list by blood type for consistent display
            val sortedList = stockList.sortedBy { it.bloodType }
            
            Log.d(TAG, "Processed ${sortedList.size} stock items")
            
            // Update the UI on the main thread
            runOnUiThread {
                try {
                    Log.d(TAG, "Updating UI with ${sortedList.size} items")
                    
                    // Update the adapter with the new data
                    stockAdapter.submitList(sortedList) {
                        Log.d(TAG, "Adapter list updated with ${sortedList.size} items")
                        
                        // Ensure the RecyclerView is visible
                        binding.rvBloodStock.visibility = View.VISIBLE
                        binding.tvEmptyStock.visibility = View.GONE
                        
                        // Force a layout pass to ensure proper measurement
                        binding.rvBloodStock.post {
                            binding.rvBloodStock.requestLayout()
                        }
                        
                        // Update the empty state based on the list size
                        updateEmptyState(sortedList.isEmpty())
                        
                        // Log the current state of the RecyclerView
                        binding.rvBloodStock.let { rv ->
                            Log.d(TAG, "RecyclerView state - " +
                                  "Adapter count: ${rv.adapter?.itemCount}, " +
                                  "LayoutManager: ${rv.layoutManager?.itemCount} items, " +
                                  "Visibility: ${rv.visibility}, " +
                                  "isShown: ${rv.isShown}, " +
                                  "Has fixed size: ${rv.hasFixedSize()}, " +
                                  "Adapter: ${rv.adapter?.javaClass?.simpleName}")
                            
                            // Log the first few items in the adapter
                            (rv.adapter as? StockAdapter)?.let { adapter ->
                                Log.d(TAG, "Adapter item count: ${adapter.itemCount}")
                                val items = adapter.currentList
                                val itemCount = minOf(5, items.size)
                                for (i in 0 until itemCount) {
                                    Log.d(TAG, "Item $i: ${items[i]}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating UI with stock data", e)
                    updateEmptyState(true)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing stock data", e)
            runOnUiThread {
                showToast("Error processing stock data: ${e.message}")
                updateEmptyState(true)
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        try {
            Log.d(TAG, "Updating empty state: isEmpty=$isEmpty")
            
            // Ensure we're on the main thread for UI updates
            runOnUiThread {
                try {
                    // Update RecyclerView visibility
                    val recyclerVisibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.rvBloodStock.visibility = recyclerVisibility
                    Log.d(TAG, "RecyclerView visibility set to: $recyclerVisibility")
                    
                    // Update empty state text visibility
                    val emptyVisibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.tvEmptyStock.visibility = emptyVisibility
                    Log.d(TAG, "Empty state visibility set to: $emptyVisibility")
                    
                    // Log the current state of the views
                    Log.d(TAG, "RecyclerView isShown: ${binding.rvBloodStock.isShown}, " +
                            "EmptyView isShown: ${binding.tvEmptyStock.isShown}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in updateEmptyState UI updates", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateEmptyState", e)
        }
    }

    private fun loadAppointmentStatistics() {
        val realtimeDb = com.google.firebase.database.FirebaseDatabase.getInstance()
        val appointmentsRef = realtimeDb.getReference("appointments_active")
            .orderByChild("hospitalId")
            .equalTo(hospitalId)
        
        appointmentsRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                var activeCount = 0
                var completedCount = 0
                
                for (appointmentSnapshot in snapshot.children) {
                    val status = appointmentSnapshot.child("status").getValue(String::class.java) ?: "pending"
                    when (status) {
                        "pending" -> activeCount++
                        "completed" -> completedCount++
                    }
                }
                
                // Update UI
                binding.tvActiveAppointments.text = activeCount.toString()
                binding.tvCompletedDonations.text = completedCount.toString()
                
                Log.d(TAG, "Appointment stats - Active: $activeCount, Completed: $completedCount")
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e(TAG, "Error loading appointment statistics", error.toException())
            }
        })
    }

    // Clean up resources when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        // Remove the Firestore listener when the activity is destroyed
        registration?.remove()
        registration = null
        hideProgress()
    }

    // Removed BloodStockItem - using data.model.StockItem instead
}
