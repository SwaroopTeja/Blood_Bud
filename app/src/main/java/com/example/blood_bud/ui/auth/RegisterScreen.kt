package com.example.blood_bud.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blood_bud.R
import com.example.blood_bud.data.model.Address
import com.example.blood_bud.data.model.User
import com.example.blood_bud.data.model.UserType
import com.example.blood_bud.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Form fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf(UserType.DONOR) }
    
    // Address fields
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("India") }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    
    // Handle registration
    fun handleRegistration() {
        // Reset error message
        errorMessage = null
        
        // Validate passwords match
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match"
            return
        }
        
        // Validate required fields based on user type
        if (email.isBlank() || password.isBlank() || phone.isBlank()) {
            errorMessage = "Please fill in all required fields"
            return
        }
        
        // For donors, name and blood group are required
        if (userType == UserType.DONOR && name.isBlank()) {
            errorMessage = "Please enter your full name"
            return
        }
        
        if (userType == UserType.DONOR && bloodGroup.isBlank()) {
            errorMessage = "Please select your blood group"
            return
        }
        
        // For hospitals, ensure hospital name is provided instead of personal name
        if (userType == UserType.HOSPITAL && name.isBlank()) {
            name = "Hospital - ${email.split("@")[0]}" // Default hospital name if not provided
        }
        
        // Validate password strength
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }
        
        // Create address
        val address = Address(
            street = street,
            city = city,
            state = state,
            postalCode = postalCode,
            country = country
        )
        
        // Create user object
        val user = User(
            id = "", // Will be set by Firestore
            email = email.trim(),
            name = name.trim(),
            phone = phone.trim(),
            bloodGroup = if (userType == UserType.DONOR) bloodGroup.trim() else null,
            userType = userType,
            address = address,
            isActive = true // Set default active status
        )
        
        // Show loading indicator
        isLoading = true
        
        // Call ViewModel to register user
        viewModel.register(user, password) { result ->
            isLoading = false
            
            result.onSuccess {
                // Registration successful
                onRegisterSuccess()
            }.onFailure { exception ->
                // Show error message to user
                errorMessage = when {
                    exception.message?.contains("email address is already in use", ignoreCase = true) == true ->
                        "This email is already registered. Please use a different email or log in."
                    exception.message?.contains("network error", ignoreCase = true) == true ->
                        "Network error. Please check your internet connection and try again."
                    else -> exception.message ?: "Registration failed. Please try again."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                // Test button at the very top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.Green)
                        .clickable {
                            println("DEBUG: TOP TEST BUTTON CLICKED!")
                            errorMessage = "Top test button clicked!"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("TOP TEST BUTTON - TAP ME", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                TopAppBar(
                    title = { Text("Create Account") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Error message
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Name field (optional for hospitals, required for donors)
            if (userType == UserType.DONOR) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hospital Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Will use email prefix if not provided") }
                )
            }

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (showPassword) {
                    PasswordVisualTransformation()
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Confirm Password field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = if (showPassword) {
                    PasswordVisualTransformation()
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Phone field
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Blood Group field (only for donors)
            if (userType == UserType.DONOR) {
                var expanded by remember { mutableStateOf(false) }
                val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        readOnly = true,
                        label = { Text("Blood Group") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        bloodGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group) },
                                onClick = {
                                    bloodGroup = group
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // User Type Selection
            Text("I am a:", modifier = Modifier.padding(top = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = userType == UserType.DONOR,
                        onClick = { userType = UserType.DONOR }
                    )
                    Text("Donor")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = userType == UserType.HOSPITAL,
                        onClick = { userType = UserType.HOSPITAL }
                    )
                    Text("Hospital")
                }
            }

            // Address Section
            Text("Address", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            
            // Street
            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { Text("Street Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // City and State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Postal Code and Country
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { postalCode = it },
                    label = { Text("Postal Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    singleLine = true,
                    readOnly = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Debug info
            Text("Debug - Button enabled: ${!isLoading}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Debug - Email: $email", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Debug - Password: ${if (password.isNotBlank()) "[set]" else "[empty]"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Debug - Confirm: ${if (confirmPassword.isNotBlank()) "[set]" else "[empty]"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Debug - Phone: $phone", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            // Debug info
            Text("Debug - User Type: $userType", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Debug - Button enabled: true", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            // Simple test button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.Red, shape = MaterialTheme.shapes.medium)
                    .clickable {
                        println("DEBUG: TEST BUTTON CLICKED - UserType: $userType")
                        if (email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && phone.isNotBlank()) {
                            if (userType == UserType.DONOR && name.isBlank()) {
                                errorMessage = "Please enter your full name"
                            } else {
                                handleRegistration()
                            }
                        } else {
                            errorMessage = "Please fill in all required fields"
                        }
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text("TEST REGISTER BUTTON", color = Color.White)
                }
            }
        }
    }
}
