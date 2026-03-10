package com.example.blood_bud.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.blood_bud.R
import com.example.blood_bud.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Enable standard action bar back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set up click listeners
        binding.resetPasswordButton.setOnClickListener {
            resetPassword()
        }

        binding.backToLoginText.setOnClickListener {
            onBackPressed()
        }
    }

    private fun resetPassword() {
        val email = binding.emailEditText.text.toString().trim()

        // Validate email
        if (email.isEmpty()) {
            binding.emailLayout.error = getString(R.string.email_required)
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.invalid_email)
            return
        }

        // Clear any previous errors
        binding.emailLayout.error = null

        // Show progress and disable UI
        binding.resetPasswordButton.isEnabled = false
        binding.resetPasswordButton.text = getString(R.string.checking_email)

        // First check if email exists and is verified
        android.util.Log.d("ForgotPassword", "Checking if email exists: $email")
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    val isEmailRegistered = !signInMethods.isNullOrEmpty()
                    android.util.Log.d("ForgotPassword", "Sign-in methods for $email: $signInMethods")
                    
                    if (isEmailRegistered) {
                        // Email exists, now check verification status
                        auth.signInWithEmailAndPassword(email, "temporaryPassword")
                            .addOnCompleteListener { signInTask ->
                                if (signInTask.isSuccessful) {
                                    val user = auth.currentUser
                                    if (user != null && user.isEmailVerified) {
                                        // Email is verified, send reset email
                                        sendPasswordResetEmail(email)
                                    } else {
                                        // Email not verified
                                        binding.resetPasswordButton.isEnabled = true
                                        binding.resetPasswordButton.text = getString(R.string.reset_password)
                                        Toast.makeText(
                                            this,
                                            getString(R.string.email_not_verified),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        // Sign out the temporary user
                                        auth.signOut()
                                    }
                                } else {
                                    // Couldn't sign in, but email exists (password might be wrong)
                                    sendPasswordResetEmail(email)
                                }
                            }
                    } else {
                        // Email not registered or error occurred
                        binding.resetPasswordButton.isEnabled = true
                        binding.resetPasswordButton.text = getString(R.string.reset_password)
                        if (task.exception != null) {
                            android.util.Log.e("ForgotPassword", "Error checking email", task.exception)
                            Toast.makeText(
                                this,
                                "Error checking email: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            android.util.Log.d("ForgotPassword", "No account found with email: $email")
                            // Try to send reset email anyway, as a fallback
                            sendPasswordResetEmail(email)
                        }
                    }
                } else {
                    // Error checking email
                    binding.resetPasswordButton.isEnabled = true
                    binding.resetPasswordButton.text = getString(R.string.reset_password)
                    Toast.makeText(
                        this,
                        getString(R.string.error_checking_email, task.exception?.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        binding.resetPasswordButton.text = getString(R.string.sending_email)
        
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.resetPasswordButton.isEnabled = true
                binding.resetPasswordButton.text = getString(R.string.reset_password)

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        getString(R.string.reset_email_sent, email),
                        Toast.LENGTH_LONG
                    ).show()
                    // Close the activity after a short delay
                    binding.emailEditText.postDelayed({ finish() }, 1500)
                } else {
                    val errorMessage = task.exception?.message ?: getString(R.string.unknown_error)
                    Toast.makeText(
                        this,
                        getString(R.string.reset_email_failed, errorMessage),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
