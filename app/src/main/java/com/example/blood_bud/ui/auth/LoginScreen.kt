package com.example.blood_bud.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.example.blood_bud.R
import com.example.blood_bud.utils.Screen
import com.example.blood_bud.viewmodel.AuthState
import com.example.blood_bud.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val context = LocalContext.current
    val (email, setEmail) = remember { mutableStateOf("") }
    val (password, setPassword) = remember { mutableStateOf("") }
    
    // Track if we've shown the login error
    var hasShownError by remember { mutableStateOf(false) }
    
    // Handle login button click
    val handleLogin: () -> Unit = {
        if (email.isBlank() || password.isBlank()) {
            // Show error for empty fields
            hasShownError = true
        } else {
            // For all users including potential admins, call onLogin
            // The AuthViewModel will handle the actual authentication and user type check
            onLogin(email, password)
        }
    }
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // The UserManager will handle the redirection based on user role
                // The authentication state is already being handled by the auth flow
                onLoginSuccess()
            }
            is AuthState.Error -> {
                // Only show the error once to avoid showing it on every recomposition
                if (!hasShownError) {
                    hasShownError = true
                    // Show error message to user
                    // The error state is already handled by the AuthState.Error case
                    android.util.Log.d("LoginScreen", "AuthState.Error: ${authState.message}")
                }
            }
            is AuthState.Loading -> {
                // Show loading state if needed
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large test button at the top
        Button(
            onClick = {
                navController?.navigate(Screen.Test.route) {
                    popUpTo(Screen.Login.route) { saveState = true }
                    launchSingleTop = true
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(80.dp)
        ) {
            Text("TEST BUTTON (DEBUG)", 
                color = Color.White, 
                style = MaterialTheme.typography.titleLarge
            )
        }
        // App Logo/Title
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Error message
        if (hasShownError && authState is AuthState.Error) {
            Text(
                text = authState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else if (email.isBlank() || password.isBlank()) {
            Text(
                text = "Please enter both email and password",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = setEmail,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { setPassword(it) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = authState is AuthState.Error,
            modifier = Modifier.fillMaxWidth()
        )

        // Forgot password link with better visibility
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onNavigateToForgotPassword,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "Forgot Password?",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login button
        Button(
            onClick = handleLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.login))
            }
        }

        // Test button (temporary for debugging)
        Button(
            onClick = {
                navController?.navigate(Screen.Test.route) {
                    popUpTo(Screen.Login.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("TEST BUTTON (DEBUG)", color = Color.Black)
        }
        
        // Register link
        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Don't have an account? Register here")
        }
    }
}
