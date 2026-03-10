package com.example.blood_bud.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    onLogout: () -> Unit,
    onNavigate: (AdminScreen) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome, Admin",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                // Admin Actions Grid
                val adminActions = listOf(
                    AdminAction("Manage Users", Icons.Default.Person, AdminScreen.Users),
                    AdminAction("View Donations", Icons.Default.Favorite, AdminScreen.Donations),
                    AdminAction("Manage Hospitals", Icons.Default.Home, AdminScreen.Hospitals),
                    AdminAction("View Analytics", Icons.Default.Info, AdminScreen.Analytics),
                    AdminAction("Manage Admins", Icons.Default.Settings, AdminScreen.Admins)
                )
                
                adminActions.chunked(2).forEach { rowActions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowActions.forEach { action ->
                            AdminActionCard(
                                action = action,
                                onClick = { onNavigate(action.screen) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun AdminActionCard(
    action: AdminAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class AdminAction(
    val title: String,
    val icon: ImageVector,
    val screen: AdminScreen
)

// Define admin screens for navigation
sealed class AdminScreen(val route: String) {
    object Dashboard : AdminScreen("admin/dashboard")
    object Users : AdminScreen("admin/users")
    object Donations : AdminScreen("admin/donations")
    object Hospitals : AdminScreen("admin/hospitals")
    object Analytics : AdminScreen("admin/analytics")
    object Admins : AdminScreen("admin/admins")
}

// Preview for AdminDashboard
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AdminDashboardPreview() {
    MaterialTheme {
        AdminDashboard(
            onLogout = {},
            onNavigate = {}
        )
    }
}
