package com.example.blood_bud.ui.hospital

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.blood_bud.data.model.User
import com.example.blood_bud.ui.components.ActionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalDashboard(
    user: User,
    onLogout: () -> Unit,
    onNavigateToInventory: () -> Unit = {},
    onNavigateToRequests: () -> Unit = {},
    onNavigateToDonors: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAppointments: () -> Unit = {}
) {
    val actions = listOf(
        HospitalAction("Appointments", Icons.Default.DateRange, "View and manage appointments") { onNavigateToAppointments() },
        HospitalAction("Blood Inventory", Icons.Default.Favorite, "Manage your blood bank inventory") { onNavigateToInventory() },
        HospitalAction("Blood Requests", Icons.Default.List, "View and manage blood requests") { onNavigateToRequests() },
        HospitalAction("Donor List", Icons.Default.Person, "View and manage registered donors") { onNavigateToDonors() },
        HospitalAction("Hospital Profile", Icons.Default.Info, "Manage hospital information") { onNavigateToProfile() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${user.name}") },
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
                .padding(16.dp)
        ) {
            // Quick stats or alerts
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Quick Stats", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 5 pending requests", style = MaterialTheme.typography.bodyMedium)
                    Text("• 3 critical blood types low", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Action grid
            actions.chunked(2).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowActions.forEach { action ->
                        ActionCard(
                            title = action.title,
                            icon = action.icon,
                            description = action.description,
                            onClick = action.onClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

data class HospitalAction(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit
)
