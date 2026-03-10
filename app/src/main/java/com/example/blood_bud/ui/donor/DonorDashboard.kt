package com.example.blood_bud.ui.donor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blood_bud.data.model.User
import com.example.blood_bud.data.model.UserType
import com.example.blood_bud.ui.theme.BloodBudTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorDashboard(
    user: User,
    onLogout: () -> Unit,
    onNavigateToDonate: () -> Unit = {},
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val donorActions = listOf(
        DonorAction("Donate", Icons.Default.Favorite, "Find and schedule blood donations") { onNavigateToDonate() },
        DonorAction("Search", Icons.Default.Search, "Search for blood banks and centers") { /* TODO: Implement search */ },
        DonorAction("Appointments", Icons.Default.DateRange, "Manage your donation appointments") { onNavigateToAppointments() },
        DonorAction("Profile", Icons.Default.Person, "View and update your profile") { onNavigateToProfile() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("BloodBud", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.ExitToApp, 
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Welcome Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Profile icon with initial
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                "Welcome back,",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                user.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("3", "Donations")
                        StatItem("9", "Lives Saved")
                        StatItem("A+", "Blood Type")
                    }
                }
            }
            
            // Quick Actions
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            // Action grid
            donorActions.chunked(2).forEach { rowActions ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowActions.forEach { action ->
                        DonorActionCard(
                            action = action,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Upcoming Appointments (if any)
            // TODO: Add upcoming appointments section
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonorActionCard(
    action: DonorAction,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 8.dp.value else 2.dp.value,
        label = "elevation"
    )
    
    Card(
        onClick = action.onClick,
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2
            )
        }
    }
}

data class DonorAction(
    val title: String,
    val icon: ImageVector,
    val description: String = "",
    val onClick: () -> Unit = {}
)

@Preview(showBackground = true)
@Composable
fun DonorDashboardPreview() {
    BloodBudTheme {
        DonorDashboard(
            user = User(
                id = "1",
                name = "John Doe",
                email = "john@example.com",
                phone = "1234567890",
                userType = UserType.DONOR
            ),
            onLogout = {},
            onNavigateToDonate = {},
            onNavigateToAppointments = {},
            onNavigateToHistory = {},
            onNavigateToProfile = {}
        )
    }
}
