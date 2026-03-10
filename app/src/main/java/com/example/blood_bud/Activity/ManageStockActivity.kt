package com.example.blood_bud.Activity

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.blood_bud.Adapter.StockAdapter
import com.example.blood_bud.R
import com.example.blood_bud.base.BaseHospitalActivity
import com.example.blood_bud.data.model.StockItem
import com.example.blood_bud.databinding.ActivityManageStockBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase

class ManageStockActivity : BaseHospitalActivity<ActivityManageStockBinding>() {
    
    companion object {
        private const val TAG = "ManageStockActivity"
        
        fun newIntent(context: android.content.Context): Intent {
            return Intent(context, ManageStockActivity::class.java)
        }
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var registration: ListenerRegistration? = null
    private lateinit var stockAdapter: StockAdapter
    private var currentStockItem: StockItem? = null
    private val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    override fun getViewBinding() = ActivityManageStockBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupToolbar()
        setupBloodTypeDropdown()
        setupRecyclerView()
        setupClickListeners()
        loadStockData()
        setupKeyboardListener()
    }

    private fun setupKeyboardListener() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            binding.root.getWindowVisibleDisplayFrame(r)
            val screenHeight = binding.root.rootView.height
            val keypadHeight = screenHeight - r.bottom

            if (keypadHeight > screenHeight * 0.15) {
                val focusedView = currentFocus
                focusedView?.let { view ->
                    val scrollTo = Rect()
                    view.getHitRect(scrollTo)
                    binding.nestedScrollView.requestChildRectangleOnScreen(
                        view,
                        scrollTo,
                        false
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // No need to select any nav item as this is not a bottom nav activity
        // The stock management is accessed from the hospital dashboard
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter(
            onEditClick = { stockItem ->
                currentStockItem = stockItem
                binding.apply {
                    etBloodType.setText(stockItem.bloodType, false)
                    etUnits.setText(stockItem.units.toString())
                    btnAddUpdate.text = getString(R.string.update_stock)
                    
                    // Scroll to the form when editing
                    nestedScrollView.post {
                        nestedScrollView.smoothScrollTo(0, 0)
                    }
                    
                    // Show snackbar with cancel option
                    Snackbar.make(root, "Editing ${stockItem.bloodType}", Snackbar.LENGTH_LONG)
                        .setAction(R.string.cancel) {
                            etBloodType.text?.clear()
                            etUnits.text?.clear()
                            btnAddUpdate.text = getString(R.string.add_stock)
                            currentStockItem = null
                            hideKeyboard()
                        }
                        .show()
                }
            },
            onDeleteClick = { stockItem ->
                showDeleteConfirmation(stockItem)
            }
        )

        binding.rvStock.apply {
            layoutManager = LinearLayoutManager(this@ManageStockActivity).apply {
                // This ensures the RecyclerView takes full height
                isAutoMeasureEnabled = true
            }
            setHasFixedSize(true)
            adapter = stockAdapter
            itemAnimator = DefaultItemAnimator()
            
            // Add item decoration for dividers
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                private val spacing = resources.getDimensionPixelSize(R.dimen.item_spacing)

                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    val itemCount = parent.adapter?.itemCount ?: 0

                    outRect.top = if (position == 0) spacing else 0
                    outRect.bottom = if (position == itemCount - 1) spacing * 2 else spacing
                    outRect.left = spacing
                    outRect.right = spacing
                }
            })

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(recyclerView.windowToken, 0)
                }
            })
        }
    }

    private fun setupBloodTypeDropdown() {
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            bloodTypes
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                view.findViewById<TextView>(android.R.id.text1).text = bloodTypes[position]
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.findViewById<TextView>(android.R.id.text1).text = bloodTypes[position]
                return view
            }
        }

        binding.etBloodType.setAdapter(adapter)
        binding.etBloodType.keyListener = null
        binding.etBloodType.setOnItemClickListener { _, _, position, _ ->
            binding.etBloodType.setText(bloodTypes[position])
            hideKeyboard()
        }
        binding.etBloodType.setOnClickListener {
            if (adapter.count > 0) {
                binding.etBloodType.showDropDown()
            } else {
                showToast("Error loading blood types")
            }
        }
        binding.etBloodType.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && adapter.count > 0) {
                binding.etBloodType.post {
                    binding.etBloodType.showDropDown()
                }
            }
        }
        binding.etBloodType.hint = getString(R.string.select_blood_type)
    }

    private fun setupClickListeners() {
        binding.btnAddUpdate.setOnClickListener {
            val bloodType = binding.etBloodType.text.toString().trim()
            val units = binding.etUnits.text.toString().trim()

            if (bloodType.isEmpty()) {
                binding.etBloodType.error = "Please select blood type"
                return@setOnClickListener
            }

            if (units.isEmpty()) {
                binding.etUnits.error = "Please enter units"
                return@setOnClickListener
            }

            val unitsInt = try {
                units.toInt()
            } catch (e: NumberFormatException) {
                binding.etUnits.error = "Please enter a valid number"
                return@setOnClickListener
            }

            if (unitsInt <= 0) {
                binding.etUnits.error = "Units must be greater than 0"
                return@setOnClickListener
            }

            val stockItem = StockItem(
                bloodType = bloodType,
                units = unitsInt,
                lastUpdated = System.currentTimeMillis()
            )

            saveStockItem(stockItem)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.root.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            currentFocus?.clearFocus()
        }
    }

    private fun loadStockData() {
        val userId = auth.currentUser?.uid ?: run {
            showToast("User not logged in")
            Log.w(TAG, "loadStockData: User not logged in")
            return
        }

        Log.d(TAG, "Loading stock data for user: $userId")
        
        registration = db.collection("hospitals")
            .document(userId)
            .addSnapshotListener { document, e ->
                if (e != null) {
                    val errorMsg = "Error loading stock data: ${e.message}"
                    Log.e(TAG, errorMsg, e)
                    showToast(errorMsg)
                    return@addSnapshotListener
                }

                if (document == null || !document.exists()) {
                    Log.d(TAG, "No document found for user: $userId")
                    stockAdapter.submitList(emptyList())
                    return@addSnapshotListener
                }

                try {
                    val bloodInventory = document.get("bloodInventory") as? Map<String, Any>
                    if (bloodInventory == null) {
                        Log.d(TAG, "No blood inventory found in document")
                        stockAdapter.submitList(emptyList())
                        return@addSnapshotListener
                    }
                    
                    Log.d(TAG, "Raw blood inventory data: $bloodInventory")
                    
                    val stockItems = bloodInventory.mapNotNull { (bloodType, units) ->
                        try {
                            val unitValue = when (units) {
                                is Number -> units.toInt()
                                is String -> units.toIntOrNull() ?: 0
                                else -> 0
                            }
                            
                            if (unitValue <= 0) {
                                Log.d(TAG, "Skipping $bloodType with 0 or invalid units")
                                return@mapNotNull null
                            }
                            
                            StockItem(
                                id = "${userId}_${bloodType}",
                                bloodType = bloodType,
                                units = unitValue,
                                lastUpdated = document.getLong("updatedAt") ?: 0
                            ).also {
                                Log.d(TAG, "Created stock item: $it")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error creating stock item for $bloodType: ${e.message}", e)
                            null
                        }
                    }.sortedBy { it.bloodType }

                    Log.d(TAG, "Submitting ${stockItems.size} stock items to adapter")
                    stockAdapter.submitList(stockItems) {
                        Log.d(TAG, "Stock list updated in adapter")
                        // Scroll to top after data is loaded
                        binding.rvStock.post {
                            if (stockItems.isNotEmpty()) {
                                binding.rvStock.smoothScrollToPosition(0)
                            }
                        }
                    }
                } catch (e: Exception) {
                    val errorMsg = "Error processing stock data: ${e.message}"
                    Log.e(TAG, errorMsg, e)
                    showToast(errorMsg)
                    stockAdapter.submitList(emptyList())
                }
            }
    }

    private fun saveStockItem(stockItem: StockItem) {
        val userId = auth.currentUser?.uid ?: run {
            showToast("User not logged in")
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        
        // First, get the current stock to add to existing value
        val docRef = db.collection("hospitals").document(userId)
        
        docRef.get().addOnSuccessListener { document ->
            val currentInventory = (document.get("bloodInventory") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
            val currentUnits = (currentInventory[stockItem.bloodType] as? Number)?.toInt() ?: 0
            val newUnits = if (currentStockItem != null) stockItem.units else currentUnits + stockItem.units
            
            val updates = hashMapOf<String, Any>(
                "bloodInventory.${stockItem.bloodType}" to newUnits,
                "updatedAt" to System.currentTimeMillis()
            )

            val isUpdate = currentStockItem != null
            docRef.update(updates)
                .addOnSuccessListener {
                    binding.etBloodType.text?.clear()
                    binding.etUnits.text?.clear()
                    binding.etBloodType.requestFocus()
                    currentStockItem = null
                    binding.btnAddUpdate.text = getString(R.string.add_stock)
                    showToast(if (isUpdate) "Stock updated successfully" else "Stock added successfully")
                    loadStockData()
                }
                .addOnFailureListener { e ->
                    showToast("Failed to update stock: ${e.message}")
                    Log.e("ManageStockActivity", "Error updating stock", e)
                }
                .addOnCompleteListener {
                    binding.progressBar.visibility = View.GONE
                }
        }.addOnFailureListener { e ->
            binding.progressBar.visibility = View.GONE
            showToast("Failed to load current stock: ${e.message}")
            Log.e("ManageStockActivity", "Error loading current stock", e)
        }
    }

    private fun showDeleteConfirmation(stockItem: StockItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Stock")
            .setMessage("Are you sure you want to delete ${stockItem.bloodType}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteStockItem(stockItem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteStockItem(stockItem: StockItem) {
        val userId = auth.currentUser?.uid ?: run {
            showToast("User not logged in")
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        val updates = hashMapOf<String, Any>(
            "bloodInventory.${stockItem.bloodType}" to 0,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("hospitals")
            .document(userId)
            .update(updates)
            .addOnSuccessListener {
                if (currentStockItem?.bloodType == stockItem.bloodType) {
                    binding.etBloodType.text?.clear()
                    binding.etUnits.text?.clear()
                    binding.btnAddUpdate.text = getString(R.string.add_stock)
                    currentStockItem = null
                }
                showToast("Stock deleted successfully")
                loadStockData()
            }
            .addOnFailureListener { e ->
                showToast("Failed to delete stock")
                Log.e("ManageStockActivity", "Error deleting stock item", e)
            }
            .addOnCompleteListener {
                binding.progressBar.visibility = View.GONE
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        registration?.remove()
    }
}
