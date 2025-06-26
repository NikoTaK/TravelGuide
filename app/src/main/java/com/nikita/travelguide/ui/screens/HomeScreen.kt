package com.nikita.travelguide.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nikita.travelguide.UiState
import com.nikita.travelguide.GuideVM
import com.nikita.travelguide.network.GEOAPIFY_KEY
import com.nikita.travelguide.storage.FavoritePoi
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardActions

@Composable
fun HomeScreen(
    city: String,
    onCityChange: (String) -> Unit,
    recentSearches: List<String>,
    onRecentSearch: (String) -> Unit,
    onSearch: () -> Unit,
    vm: GuideVM,
    apiKey: String,
    favorites: List<FavoritePoi>,
    onToggleFavorite: (com.nikita.travelguide.network.PoiFeature) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Find Attractions", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = city,
                onValueChange = onCityChange,
                label = { Text("Search city...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (city.isNotEmpty()) {
                        IconButton(onClick = { onCityChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = onSearch, enabled = apiKey.isNotEmpty() && city.isNotBlank()) {
                Text("Search")
            }
        }
        if (recentSearches.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Recent Searches", style = MaterialTheme.typography.titleMedium)
            LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                items(recentSearches) { search ->
                    AssistChip(
                        onClick = { onRecentSearch(search) },
                        label = { Text(search) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        when (val s = vm.state) {
            UiState.Idle -> Text("Enter a city and tap Search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Err -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("❌ ${s.msg}", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onSearch) { Text("Retry") }
            }
            is UiState.Ok -> POIListWithFavorites(
                pois = s.list,
                favorites = favorites,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}

@Composable
fun POIListWithFavorites(
    pois: List<com.nikita.travelguide.network.PoiFeature>,
    favorites: List<FavoritePoi>,
    onToggleFavorite: (com.nikita.travelguide.network.PoiFeature) -> Unit
) {
    var expandedIndex by remember { mutableStateOf(-1) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(pois) { idx, poi ->
            val isFavorite = favorites.any {
                it.name == poi.properties.name &&
                it.lat == poi.geometry.coordinates.getOrNull(1) &&
                it.lon == poi.geometry.coordinates.getOrNull(0)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(poi.properties.name ?: "(no name)", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onToggleFavorite(poi) }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { expandedIndex = if (expandedIndex == idx) -1 else idx },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (expandedIndex == idx) "Hide Details" else "Show Details")
                    }
                    if (expandedIndex == idx) {
                        val coords = poi.geometry.coordinates
                        if (coords.size >= 2) {
                            val lon = coords[0]
                            val lat = coords[1]
                            val mapUrl = "https://maps.geoapify.com/v1/staticmap?style=osm-carto&width=400&height=200&center=lonlat:$lon,$lat&zoom=15&marker=lonlat:$lon,$lat;color:%23ff0000;size:large&apiKey=${GEOAPIFY_KEY}"
                            Spacer(Modifier.height(8.dp))
                            AsyncImage(
                                model = mapUrl,
                                contentDescription = "Map for ${poi.properties.name}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
} 