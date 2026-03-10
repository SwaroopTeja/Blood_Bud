package com.example.blood_bud.utils

sealed class Screen(val route: String) {
    // Initial screen
    object Loading : Screen("loading")

    // Auth screens
    object Login : Screen("login")
    object RegisterType : Screen("register/type")
    object RegisterDonor : Screen("register/donor")
    object RegisterHospital : Screen("register/hospital")
    object ForgotPassword : Screen("forgot_password")
    object Test : Screen("test")

    // Main app screens
    object UserHome : Screen("user_home")
    object DonorDashboard : Screen("donor/dashboard")
    object HospitalDashboard : Screen("hospital/dashboard")
    object Hospitals : Screen("hospitals")
    object HospitalDetail : Screen("hospital_detail/{hospitalId}") {
        fun createRoute(hospitalId: String) = "hospital_detail/$hospitalId"
    }

    // Admin screens
    object AdminDashboard : Screen("admin/dashboard")
    object ManageHospitals : Screen("admin/hospitals")
    object ManageAdmins : Screen("admin/admins")

    // Hospital screens
    object HospitalInventory : Screen("hospital/inventory")
    object HospitalRequests : Screen("hospital/requests")
    object HospitalAppointments : Screen("hospital/appointments")

    // Donor screens
    object DonationHistory : Screen("donor/history")
    object FindHospitals : Screen("donor/find_hospitals")

    // Common screens
    object Profile : Screen("profile")
    object Settings : Screen("settings")

    companion object {
        // List of all screens that should be accessible when not logged in
        val publicScreens by lazy {
            listOf(
                Login.route,
                RegisterType.route,
                ForgotPassword.route,
                Loading.route
            )
        }
    }
}
