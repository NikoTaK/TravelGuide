package com.nikita.travelguide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@Composable
fun GradientBackground(darkTheme: Boolean, content: @Composable BoxScope.() -> Unit) {
    val backgroundColor = if (darkTheme) {
        Color(0xFF181824) // Almost black
    } else {
        Color(0xFFF8F8FA) // Almost white
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        content()
    }
}

@Composable
fun SplashScreen() {
    GradientBackground(darkTheme = false) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🌍", // Replace with Image if you have a logo
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "TravelGuide",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            CircularProgressIndicator()
        }
    }
} 