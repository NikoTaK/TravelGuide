package com.nikita.travelguide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun MainBackground(darkTheme: Boolean, content: @Composable BoxScope.() -> Unit) {
    val backgroundColor = if (darkTheme) {
        Color(0xFF181824)
    } else {
        Color(0xFFF8F8FA)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        content()
    }
}