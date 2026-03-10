package com.example.blood_bud.ui.test

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TestScreen(
    onNavigateBack: () -> Unit
) {
    var buttonPressCount by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Test Button 1 - Simple Button
        Button(
            onClick = {
                buttonPressCount++
                Log.d("TestScreen", "Button 1 clicked! Count: $buttonPressCount")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Test Button 1 (Count: $buttonPressCount)")
        }
        
        // Test Button 2 - Box with clickable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.Green)
                .clickable {
                    buttonPressCount++
                    Log.d("TestScreen", "Green Box clicked! Count: $buttonPressCount")
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Green Test Box (Count: $buttonPressCount)", color = Color.White)
        }
        
        // Back button
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Back to Previous Screen")
        }
    }
}
