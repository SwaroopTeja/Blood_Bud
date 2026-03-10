package com.example.blood_bud.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAdminsScreen(
    onBackClick: () -> Unit,
    onAddAdminClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Admins") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddAdminClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Admin")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // TODO: Replace with actual admin list from ViewModel
                val dummyAdmins = listOf(
                    "admin1@example.com" to true,
                    "admin2@example.com" to false,
                    "admin3@example.com" to true
                )

                if (dummyAdmins.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No admins found")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dummyAdmins) { (email, isActive) ->
                            AdminItem(
                                email = email,
                                isActive = isActive,
                                onStatusChange = { /* TODO: Handle status change */ },
                                onDelete = { /* TODO: Handle delete */ }
                            )
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminItem(
    email: String,
    isActive: Boolean,
    onStatusChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = email,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isActive) "Active" else "Inactive",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = isActive,
                    onCheckedChange = onStatusChange
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Admin",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ManageAdminsScreenPreview() {
    MaterialTheme {
        ManageAdminsScreen(
            onBackClick = {},
            onAddAdminClick = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun AdminItemPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminItem(
                email = "admin@example.com",
                isActive = true,
                onStatusChange = {},
                onDelete = {}
            )
            AdminItem(
                email = "inactive@example.com",
                isActive = false,
                onStatusChange = {},
                onDelete = {}
            )
        }
    }
}
