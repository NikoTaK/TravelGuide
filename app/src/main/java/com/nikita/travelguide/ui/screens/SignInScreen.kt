package com.nikita.travelguide.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun SignInScreen(onSignInSuccess: (FirebaseUser) -> Unit, darkTheme: Boolean) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    MainBackground(darkTheme = darkTheme) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (darkTheme) Color(0xFF23243A) else Color.White
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegistering) "Register" else "Log In",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = if (darkTheme) Color.White else Color.Black,
                            unfocusedTextColor = if (darkTheme) Color.White else Color.Black,
                            focusedContainerColor = if (darkTheme) Color(0xFF23243A) else Color.White,
                            unfocusedContainerColor = if (darkTheme) Color(0xFF23243A) else Color.White,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = if (darkTheme) Color.White else Color.Black,
                            unfocusedTextColor = if (darkTheme) Color.White else Color.Black,
                            focusedContainerColor = if (darkTheme) Color(0xFF23243A) else Color.White,
                            unfocusedContainerColor = if (darkTheme) Color(0xFF23243A) else Color.White,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            loading = true
                            error = null
                            if (isRegistering) {
                                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        loading = false
                                        if (task.isSuccessful) {
                                            val user = FirebaseAuth.getInstance().currentUser
                                            if (user != null) onSignInSuccess(user)
                                        } else {
                                            error = task.exception?.localizedMessage ?: "Registration failed"
                                        }
                                    }
                            } else {
                                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        loading = false
                                        if (task.isSuccessful) {
                                            val user = FirebaseAuth.getInstance().currentUser
                                            if (user != null) onSignInSuccess(user)
                                        } else {
                                            error = task.exception?.localizedMessage ?: "Sign in failed"
                                        }
                                    }
                            }
                        },
                        enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp))
                        else Text(if (isRegistering) "Register" else "Log In")
                    }
                    TextButton(
                        onClick = { isRegistering = !isRegistering },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isRegistering) "Already have an account? Log In" else "Don't have an account? Register")
                    }
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
