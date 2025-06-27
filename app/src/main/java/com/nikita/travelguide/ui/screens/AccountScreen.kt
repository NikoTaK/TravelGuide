package com.nikita.travelguide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccountScreen(
    userEmail: String?,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: (() -> Unit)? = null,
    appVersion: String = "1.0"
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    MainBackground(darkTheme = isDarkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(userEmail ?: "Guest", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dark Mode", modifier = Modifier.weight(1f))
                Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = "Sign Out")
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
            Spacer(Modifier.height(32.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            Text("About", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("TravelGuide v$appVersion", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Developed by Nikita", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            if (onDeleteAccount != null) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Account")
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Account")
                }
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("Are you sure you want to delete your account? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteAccount?.invoke()
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
} 