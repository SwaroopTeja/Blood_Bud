package com.example.blood_bud.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.blood_bud.data.model.Hospital

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.blood_bud.ui.auth.LoginScreen
import com.example.blood_bud.ui.home.HomeScreen
// import com.example.blood_bud.ui.hospitals.HospitalDetailScreen
// import com.example.blood_bud.ui.hospitals.HospitalsScreen
import com.example.blood_bud.utils.Screen
import com.example.blood_bud.viewmodel.AuthViewModel
import com.example.blood_bud.viewmodel.AuthState
import com.example.blood_bud.data.model.UserType
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.blood_bud.ui.donor.DonorDashboard
import com.example.blood_bud.ui.hospital.HospitalDashboard
import com.example.blood_bud.ui.hospital.HospitalAppointmentsFragment
import com.example.blood_bud.ui.admin.AdminDashboard
import com.example.blood_bud.ui.admin.AdminScreen
import com.example.blood_bud.ui.admin.ManageAdminsScreen
// import com.example.blood_bud.viewmodel.HospitalDetailViewModel
// import com.example.blood_bud.viewmodel.HospitalsViewModel

@Composable
fun MainNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    // Check if user is already logged in from shared preferences
    LaunchedEffect(Unit) {
        if (startDestination == Screen.UserHome.route) {
            // User is already logged in, check current user
            authViewModel.checkCurrentUser()
        }
    }
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                // Only proceed with navigation if we have a valid user
                val user = state.user
                
                // Determine the appropriate destination based on user type
                val destination = when (user.userType) {
                    UserType.ADMIN -> Screen.AdminDashboard.route
                    UserType.HOSPITAL -> Screen.HospitalDashboard.route
                    UserType.DONOR -> Screen.DonorDashboard.route
                    else -> Screen.DonorDashboard.route // Default to donor dashboard
                }
                
                // Only navigate if not already on the destination
                val currentRoute = navController.currentDestination?.route
                val isNotOnForgotPassword = currentRoute?.startsWith("forgot_password") != true
                if (currentRoute != destination && isNotOnForgotPassword) {
                    navController.navigate(destination) {
                        // Clear back stack to prevent going back to login
                        popUpTo(0) { inclusive = true }
                        // Ensure we don't have multiple copies of the same destination
                        launchSingleTop = true
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                // Navigate to login if not already there
                if (navController.currentDestination?.route != Screen.Login.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Error -> {
                // Show error message and stay on current screen
                println("Auth error: ${state.message}")
                // If we're on the login screen, show the error there
                if (navController.currentDestination?.route == Screen.Login.route) {
                    // You can add error handling here to show in the UI
                }
            }
            is AuthState.Initial -> {
                // Initial state - do nothing, let the loading screen show
            }
            is AuthState.Loading -> {
                // Show loading state if not already showing
                if (navController.currentDestination?.route != Screen.Loading.route) {
                    navController.navigate(Screen.Loading.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {
                // Handle any other states that might be added in the future
                println("Unknown auth state: $authState")
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Loading Screen
        composable(Screen.Loading.route) {
            // Simple loading screen
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        }
        
        // Auth Screens
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            
            LaunchedEffect(Unit) {
                // Check current user when login screen is shown
                authViewModel.checkCurrentUser()
            }
            
            LoginScreen(
                onLoginSuccess = { /* Handled by auth state observer */ },
                onNavigateToRegister = {
                    navController.navigate(Screen.RegisterType.route) {
                        popUpTo(Screen.Login.route) { saveState = true }
                        launchSingleTop = true
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
        
        // Donor Dashboard
        composable(Screen.DonorDashboard.route) {
            val user = (authState as? AuthState.Authenticated)?.user
            if (user != null) {
                DonorDashboard(
                    user = user,
                    onLogout = { authViewModel.logout() },
                    onNavigateToDonate = { /* TODO: Navigate to donation screen */ },
                    onNavigateToAppointments = { /* TODO: Navigate to appointments */ },
                    onNavigateToHistory = { /* TODO: Navigate to history */ },
                    onNavigateToProfile = { /* TODO: Navigate to profile */ }
                )
            }
        }
        
        // Hospital Dashboard
        composable(Screen.HospitalDashboard.route) {
            val user = (authState as? AuthState.Authenticated)?.user
            if (user != null) {
                HospitalDashboard(
                    user = user,
                    onLogout = { authViewModel.logout() },
                    onNavigateToInventory = { /* TODO: Navigate to inventory */ },
                    onNavigateToRequests = { /* TODO: Navigate to requests */ },
                    onNavigateToDonors = { /* TODO: Navigate to donors */ },
                    onNavigateToProfile = { /* TODO: Navigate to profile */ },
                    onNavigateToAppointments = {
                        navController.navigate(Screen.HospitalAppointments.route)
                    }
                )
            } else {
                // Handle case where user is not authenticated
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HospitalDashboard.route) { inclusive = true }
                    }
                }
            }
        }
        
        // Hospital Appointments
        composable(Screen.HospitalAppointments.route) {
            HospitalAppointmentsFragment()
        }
        
        // Admin Dashboard
        composable(Screen.AdminDashboard.route) {
            val user = (authState as? AuthState.Authenticated)?.user
            if (user != null) {
                AdminDashboard(
                    onLogout = { authViewModel.logout() },
                    onNavigate = { screen ->
                        when (screen) {
                            is AdminScreen.Admins -> {
                                navController.navigate(AdminScreen.Admins.route) {
                                    launchSingleTop = true
                                }
                            }
                            // Add other admin screens here as needed
                            else -> { /* Handle other admin screens */ }
                        }
                    }
                )
            }
        }
        
        // Legacy UserHome (kept for backward compatibility)
        composable(Screen.UserHome.route) {
            val user = (authState as? AuthState.Authenticated)?.user
                ?: return@composable // Show loading or error state if user is null
            
            HomeScreen(
                user = user,
                onLogout = {
                    authViewModel.logout()
                },
                onNavigateToHospitals = {
                    navController.navigate(Screen.Hospitals.route)
                },
                onNavigateToInventory = {
                    // TODO: Navigate to inventory management
                },
                onNavigateToRequests = {
                    // TODO: Navigate to requests list
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.AdminDashboard.route)
                }
            )
        }
        
        // Admin Dashboard
        composable(Screen.AdminDashboard.route) {
            // TODO: Implement Admin Dashboard
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Admin Dashboard", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { authViewModel.logout() }) {
                    Text("Logout")
                }
            }
        }
        
        // Hospitals List Screen (temporarily disabled)
        // composable(Screen.Hospitals.route) {
        //     val viewModel: HospitalsViewModel = hiltViewModel()
        //     HospitalsScreen(
        //         onBackClick = { navController.navigateUp() },
        //         onHospitalClick = { hospitalId ->
        //             navController.navigate("${Screen.HospitalDetail.route}/$hospitalId")
        //         }
        //     )
        // }
        
        // Hospital Detail Screen (temporarily disabled)
        // composable(
        //     route = "${Screen.HospitalDetail.route}/{hospitalId}",
        //     arguments = listOf(navArgument("hospitalId") { type = NavType.StringType })
        // ) { backStackEntry ->
        //     val hospitalId = backStackEntry.arguments?.getString("hospitalId") ?: ""
        //     val viewModel: HospitalDetailViewModel = hiltViewModel()
        //     val context = LocalContext.current
        //     HospitalDetailScreen(
        //         hospitalId = hospitalId,
        //         onBackClick = { navController.navigateUp() },
        //         onGetDirections = { hospital: Hospital ->
        //             val address = "${'$'}{hospital.street}, ${'$'}{hospital.city}, ${'$'}{hospital.state} ${'$'}{hospital.postalCode}"
        //             val mapUri = Uri.parse("geo:0,0?q=${'$'}{Uri.encode(address)}")
        //             val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
        //             mapIntent.setPackage("com.google.android.apps.maps")
        //             context.startActivity(mapIntent)
        //         },
        //         onCallHospital = { phoneNumber: String ->
        //             val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${'$'}phoneNumber") }
        //             context.startActivity(intent)
        //         }
        //     )
        // }
        
        // Admin Screens
        composable(AdminScreen.Admins.route) {
            ManageAdminsScreen(
                onBackClick = { navController.navigateUp() },
                onAddAdminClick = {
                    // TODO: Implement add admin flow
                }
            )
        }
        
        // Add more screens for the main app flow here
    }
}
