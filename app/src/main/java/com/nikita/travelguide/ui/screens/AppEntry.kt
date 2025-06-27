package com.nikita.travelguide.ui.screens

import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.nikita.travelguide.storage.TravelGuideDatabase
import kotlinx.coroutines.delay
import com.nikita.travelguide.MainScreen
import androidx.compose.runtime.Composable

@Composable
fun AppEntry(apiKey: String, db: TravelGuideDatabase) {
    var showSplash by remember { mutableStateOf(true) }
    var user by remember { mutableStateOf<FirebaseUser?>(null) }
    var isDarkTheme by remember { mutableStateOf(false) }

    // Splash logic
    LaunchedEffect(Unit) {
        delay(1500)
        showSplash = false
        user = FirebaseAuth.getInstance().currentUser
    }

    // Listen for auth state changes
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { authInstance ->
            user = authInstance.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    when {
        showSplash -> SplashScreen()
        user == null -> SignInScreen(onSignInSuccess = { user = it }, darkTheme = isDarkTheme)
        else -> MainScreen(apiKey, db)
    }
} 