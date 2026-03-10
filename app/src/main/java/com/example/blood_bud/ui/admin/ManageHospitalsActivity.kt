package com.example.blood_bud.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Hospital
import com.example.blood_bud.databinding.ActivityManageHospitalsBinding
import com.example.blood_bud.ui.admin.adapter.HospitalActionListener
import com.example.blood_bud.ui.admin.adapter.HospitalsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageHospitalsActivity : AppCompatActivity(), HospitalActionListener {
    private lateinit var binding: ActivityManageHospitalsBinding
    private val viewModel: ManageHospitalsViewModel by viewModels()
    private lateinit var hospitalsAdapter: HospitalsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageHospitalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check authentication state
        if (viewModel.isUserAuthenticated()) {
            setupToolbar()
            setupRecyclerView()
            observeViewModel()
            loadHospitals()
        } else {
            // Handle unauthenticated state
            Toast.makeText(this, "Please sign in to continue", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.manage_hospitals)
        }
    }

    private fun setupRecyclerView() {
        hospitalsAdapter = HospitalsAdapter(this)
        
        binding.recyclerView.apply {
            adapter = hospitalsAdapter
            layoutManager = LinearLayoutManager(this@ManageHospitalsActivity)
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                when (uiState) {
                    is ManageHospitalsUiState.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.recyclerView.isVisible = false
                    }
                    is ManageHospitalsUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        if (uiState.hospitals.isNotEmpty()) {
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.emptyView.visibility = View.GONE
                            hospitalsAdapter.submitList(uiState.hospitals)
                        } else {
                            binding.recyclerView.visibility = View.GONE
                            binding.emptyView.visibility = View.VISIBLE
                        }
                    }
                    is ManageHospitalsUiState.Error -> {
                        binding.progressBar.isVisible = false
                        binding.recyclerView.isVisible = false
                        Toast.makeText(this@ManageHospitalsActivity, uiState.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadHospitals() {
        viewModel.loadHospitals()
    }

    override fun onHospitalClick(hospital: Hospital) {
        // For now, just show a toast since we don't have a proper activity
        // TODO: Create a proper hospital details activity
        Toast.makeText(
            this, 
            "Viewing details for ${hospital.name}", 
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onApproveHospital(hospital: Hospital) {
        hospital.id?.let { hospitalId ->
            viewModel.approveHospital(hospitalId).observe(this) { result ->
                result.onSuccess {
                    Toast.makeText(this, R.string.hospital_approved, Toast.LENGTH_SHORT).show()
                }.onFailure { exception ->
                    Toast.makeText(this, "${getString(R.string.approval_error)}: ${exception.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRejectHospital(hospital: Hospital) {
        hospital.id?.let { hospitalId ->
            viewModel.rejectHospital(hospitalId).observe(this) { result ->
                result.onSuccess {
                    Toast.makeText(this, R.string.hospital_rejected, Toast.LENGTH_SHORT).show()
                }.onFailure { exception ->
                    Toast.makeText(this, "${getString(R.string.approval_error)}: ${exception.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDeleteHospital(hospital: Hospital) {
        hospital.id?.let { hospitalId ->
            viewModel.deleteHospital(hospitalId)
            Toast.makeText(this, R.string.hospital_deleted, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
