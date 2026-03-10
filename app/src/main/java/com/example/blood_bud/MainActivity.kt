package com.example.blood_bud

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.blood_bud.Activity.IntroActivity
import com.example.blood_bud.ui.theme.BloodBudTheme
import com.example.blood_bud.utils.UserManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private fun navigateToLogin(context: Context) {
        val intent = Intent(context, IntroActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        finish()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContent {
                val context = LocalContext.current
                var isLoading by remember { mutableStateOf(true) }
                
                LaunchedEffect(Unit) {
                    // Check if user is already logged in using shared preferences first
                    val sharedPref = context.getSharedPreferences("BloodBudPrefs", MODE_PRIVATE)
                    val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
                    
                    if (isLoggedIn) {
                        // If we think we're logged in, verify with Firebase
                        val auth = Firebase.auth
                        auth.currentUser?.let { user ->
                            // User is logged in, check their role and redirect
                            UserManager.checkUserRoleAndRedirect(context) {
                                // This callback runs after the role check is complete
                                isLoading = false
                            }
                        } ?: run {
                            // User not actually logged in, clear shared prefs and go to login
                            with(sharedPref.edit()) {
                                clear()
                                apply()
                            }
                            navigateToLogin(context)
                        }
                    } else {
                        // Not logged in, go to login screen
                        navigateToLogin(context)
                    }
                }
                
                // Show a loading screen while checking auth state
                BloodBudTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (isLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Loading...")
                                CircularProgressIndicator()
                            }
                        } else {
                            // This will be shown briefly while the app is navigating away
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "Error in MainActivity", e)
            setContent {
                BloodBudTheme {
                    ErrorScreen(error = e)
                }
            }
        }
    }
}

@Composable
fun BloodBudAppContent() {
    // This is now handled in the MainActivity's onCreate
    // The content here is just a fallback and should not be reached
    BloodBudTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Loading application...")
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ErrorScreen(error: Throwable? = null) {
    val padding = 16.dp
    val spacerHeight = 16.dp
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(spacerHeight))
        
        error?.message?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BloodBudAppPreview() {
    BloodBudTheme {
        BloodBudAppContent()
    }
}
