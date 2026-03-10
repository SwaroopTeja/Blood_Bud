package com.example.blood_bud.ui.donor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.blood_bud.R
import com.example.blood_bud.data.model.User
import com.example.blood_bud.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        loadUserData()
        setupClickListeners()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("HomeFragment", "No authenticated user")
            showDefaultData()
            return
        }

        Log.d("HomeFragment", "Loading user data for: ${currentUser.uid}")

        // Load user data from Firestore
        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        Log.d("HomeFragment", "User data loaded: ${user.name}")
                        updateUIWithUserData(user)
                    } else {
                        Log.e("HomeFragment", "Failed to parse user data")
                        showDefaultData()
                    }
                } else {
                    Log.e("HomeFragment", "User document does not exist")
                    showDefaultData()
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error loading user data", e)
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
                showDefaultData()
            }
    }

    private fun updateUIWithUserData(user: User) {
        binding.tvWelcomeName.text = "Welcome, ${user.name}"
        binding.tvUserEmail.text = user.email
        
        // Display blood group or blood type
        val bloodType = user.bloodGroup ?: user.bloodType ?: "Not Set"
        binding.tvBloodType.text = bloodType
        
        // Update eligibility status (you can add more logic here)
        binding.tvEligibilityStatus.text = if (user.isActive) {
            "You are eligible to donate blood"
        } else {
            "Please update your profile to check eligibility"
        }
    }

    private fun showDefaultData() {
        binding.tvWelcomeName.text = "Welcome, Donor"
        binding.tvUserEmail.text = auth.currentUser?.email ?: "user@example.com"
        binding.tvBloodType.text = "Not Set"
        binding.tvEligibilityStatus.text = "Please update your profile"
    }

    private fun setupClickListeners() {
        binding.cardFindCenters.setOnClickListener {
            // Navigate to search/donate fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DonateFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardScheduleAppointment.setOnClickListener {
            // Navigate to donate fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DonateFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardViewAppointments.setOnClickListener {
            // Navigate to appointments fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AppointmentsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardViewProfile.setOnClickListener {
            // Navigate to profile fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}
