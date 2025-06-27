package com.nikita.travelguide.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nikita.travelguide.network.GEOAPIFY_KEY
import com.nikita.travelguide.storage.FavoritePoi

@Composable
fun FavoritesScreen(
    favorites: List<FavoritePoi>,
    onRemoveFavorite: (FavoritePoi) -> Unit,
    darkTheme: Boolean
) {
    MainBackground(darkTheme = darkTheme) {
        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No favorites yet!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Start exploring and add places you love.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Text("❤️", style = MaterialTheme.typography.displayLarge)
                }
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp)
            ) {
                items(favorites) { fav ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (darkTheme) Color(0xFF23243A) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            if (fav.lat != null && fav.lon != null) {
                                val mapUrl = "https://maps.geoapify.com/v1/staticmap?style=osm-carto&width=400&height=200&center=lonlat:${fav.lon},${fav.lat}&zoom=15&marker=lonlat:${fav.lon},${fav.lat};color:%23ff0000;size:large&apiKey=${GEOAPIFY_KEY}"
                                AsyncImage(
                                    model = mapUrl,
                                    contentDescription = "Map for ${fav.name}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                            Text(fav.name ?: "(no name)", style = MaterialTheme.typography.titleLarge)
                            Text("City: ${fav.city ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onRemoveFavorite(fav) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                                Spacer(Modifier.width(4.dp))
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }
} 