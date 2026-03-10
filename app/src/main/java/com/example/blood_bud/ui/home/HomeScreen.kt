package com.example.blood_bud.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.blood_bud.data.model.User
import com.example.blood_bud.data.model.UserType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: User?,
    onLogout: () -> Unit,
    onNavigateToHospitals: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BloodBud") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome, ${user?.name ?: "User"}!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Show different actions based on user type
            when (user?.userType) {
                UserType.DONOR -> {
                    // Donor actions
                    Button(
                        onClick = onNavigateToHospitals,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Find Hospitals Near Me")
                    }
                    
                    Button(
                        onClick = { /* TODO: Navigate to donation history */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("My Donation History")
                    }
                }
                
                UserType.HOSPITAL -> {
                    // Hospital actions
                    Button(
                        onClick = onNavigateToInventory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Blood Inventory")
                    }
                    
                    Button(
                        onClick = onNavigateToRequests,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Blood Requests")
                    }
                }
                
                UserType.ADMIN -> {
                    // Admin actions
                    Button(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Admin Dashboard")
                    }
                }
                
                null -> {
                    // Loading state
                    CircularProgressIndicator()
                }
            }
            
            // Common actions for all user types
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* TODO: Navigate to profile */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("My Profile")
            }
            
            Button(
                onClick = { /* TODO: Show emergency contacts */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emergency Contacts")
            }
        }
    }
}
