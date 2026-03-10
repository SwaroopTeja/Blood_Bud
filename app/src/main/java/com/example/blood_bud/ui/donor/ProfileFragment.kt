package com.example.blood_bud.ui.donor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.blood_bud.R
import com.example.blood_bud.data.model.User
import com.example.blood_bud.databinding.FragmentProfileBinding
import com.example.blood_bud.Activity.AccountActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = auth.currentUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        loadUserData()
        
        binding.btnEditProfile?.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.blood_bud.Activity.EditDonorProfileActivity::class.java))
        }

        binding.cardDonationHistory?.setOnClickListener {
            // TODO: Show donation history
        }

        binding.cardSettings?.setOnClickListener {
            // TODO: Navigate to settings
        }

        binding.btnLogout?.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), AccountActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }
    }
    
    private fun loadUserData() {
        val user = currentUser ?: run {
            // If no user is logged in, navigate to login
            startActivity(Intent(requireContext(), AccountActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
            return@loadUserData
        }

        try {
            // Set basic user info
            binding.apply {
                tvUserName?.text = user.displayName?.takeIf { it.isNotBlank() } ?: "User"
                tvUserEmail?.text = user.email ?: "No email"
                
                // Load profile picture
                val photoUrl = user.photoUrl?.toString()
                if (!photoUrl.isNullOrBlank()) {
                    Glide.with(this@ProfileFragment)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(binding.ivProfile)
                } else {
                    ivProfile?.setImageResource(R.drawable.ic_person)
                }
            }
            
            // Load additional user data from Firestore
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userData = document.toObject(User::class.java)
                        userData?.let { data ->
                            binding.apply {
                                // Update name and email from Firestore if available
                                if (!data.name.isNullOrBlank()) {
                                    tvUserName?.text = data.name
                                }
                                if (!data.email.isNullOrBlank()) {
                                    tvUserEmail?.text = data.email
                                }
                                
                                // Update profile picture from Firestore if available
                                if (!data.photoUrl.isNullOrBlank()) {
                                    Glide.with(this@ProfileFragment)
                                        .load(data.photoUrl)
                                        .placeholder(R.drawable.ic_person)
                                        .error(R.drawable.ic_person)
                                        .into(binding.ivProfile)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Failed to load user data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
