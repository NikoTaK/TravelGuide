package com.example.travelguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelguide.network.GEOAPIFY_KEY
import com.example.travelguide.network.Network
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.runtime.remember
import coil.compose.AsyncImage

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Ok(val list: List<com.example.travelguide.network.PoiFeature>) : UiState
    data class Err(val msg: String) : UiState
}

class GuideVM : ViewModel() {
    var state by mutableStateOf<UiState>(UiState.Idle)
        private set

    fun fetch(city: String, apiKey: String) = viewModelScope.launch {
        state = UiState.Loading
        try {
            val geo = Network.api.geocode(city.trim(), apiKey)
                .results
                ?.firstOrNull()
                ?: throw IllegalStateException("City not found")

            val placeId = geo.place_id
                ?: throw IllegalStateException("No place_id for city")

            val filterParam = "place:$placeId"

            val pois = Network.api
                .poisByPlaceId(
                    categories = "tourism.attraction",
                    filter = filterParam,
                    limit = 20,
                    key = apiKey
                )
                .features
                .take(20)

            state = UiState.Ok(pois)

        } catch (e: Exception) {
            state = UiState.Err(e.message ?: "unknown error")
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { DemoScreen(GEOAPIFY_KEY) } }
    }
}

@Composable
fun DemoScreen(
    apiKey: String,
    vm: GuideVM = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var city by remember { mutableStateOf("Paris") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { vm.fetch(city, apiKey) },
            enabled = apiKey.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Fetch POIs") }
        Spacer(Modifier.height(20.dp))
        when (val s = vm.state) {
            UiState.Idle     -> Text("Enter a city and tap the button.")
            UiState.Loading  -> CircularProgressIndicator()
            is UiState.Err   -> Text("❌ ${s.msg}", color = MaterialTheme.colorScheme.error)
            is UiState.Ok    -> POIList(s.list)
        }
    }
}

@Composable
fun POIList(pois: List<com.example.travelguide.network.PoiFeature>) {
    val expandedIndex = remember { mutableStateOf(-1) }
    LazyColumn {
        itemsIndexed(pois) { idx, poi ->
            Column(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(poi.properties.name ?: "(no name)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedIndex.value = if (expandedIndex.value == idx) -1 else idx }
                )
                if (expandedIndex.value == idx) {
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
                Divider()
            }
        }
    }
}