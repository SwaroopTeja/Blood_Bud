package com.example.blood_bud.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blood_bud.ui.test.TestScreen
import com.example.blood_bud.utils.Screen
import com.example.blood_bud.Activity.DonorRegistrationActivity
import com.example.blood_bud.Activity.HospitalRegistrationActivity
import com.example.blood_bud.Activity.RegistrationTypeActivity
import com.example.blood_bud.viewmodel.AuthState
import com.example.blood_bud.viewmodel.AuthViewModel

@Composable
fun AuthNavGraph(
    navController: NavHostController,
    onAuthSuccess: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    navigateToActivity: (Class<*>) -> Unit
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = onAuthSuccess,
                onNavigateToRegister = {
                    // Navigate to the registration type selection screen
                    navController.navigate(Screen.RegisterType.route) {
                        popUpTo(Screen.Login.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route) {
                        launchSingleTop = true
                    }
                },
                authState = authState,
                onLogin = { email, password ->
                    authViewModel.login(email, password) {}
                },
                viewModel = authViewModel,
                navController = navController
            )
        }
        
        // Registration type selection screen
        composable(Screen.RegisterType.route) {
            // This is a placeholder - we'll navigate to the actual activity
            // The actual UI is handled by RegistrationTypeActivity
            LaunchedEffect(Unit) {
                navigateToActivity(RegistrationTypeActivity::class.java)
                navController.popBackStack()
            }
        }
        
        // Donor registration screen (handled by activity)
        composable(Screen.RegisterDonor.route) {
            LaunchedEffect(Unit) {
                navigateToActivity(DonorRegistrationActivity::class.java)
                navController.popBackStack()
            }
        }
        
        // Hospital registration screen (handled by activity)
        composable(Screen.RegisterHospital.route) {
            LaunchedEffect(Unit) {
                navigateToActivity(HospitalRegistrationActivity::class.java)
                navController.popBackStack()
            }
        }
        
        // Forgot Password Screen
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                viewModel = authViewModel
            )
        }
        
        // Test screen (for debugging)
        composable(Screen.Test.route) {
            TestScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
