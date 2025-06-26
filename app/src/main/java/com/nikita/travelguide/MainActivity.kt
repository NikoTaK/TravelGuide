package com.nikita.travelguide

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
import com.nikita.travelguide.network.GEOAPIFY_KEY
import com.nikita.travelguide.network.Network
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.runtime.remember
import coil.compose.AsyncImage
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.room.Room
import com.nikita.travelguide.storage.TravelGuideDatabase
import com.nikita.travelguide.storage.FavoritePoi
import com.nikita.travelguide.storage.RecentSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Ok(val list: List<com.nikita.travelguide.network.PoiFeature>) : UiState
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

sealed class BottomNavScreen(val label: String, val icon: ImageVector) {
    object Home : BottomNavScreen("Home", Icons.Filled.Home)
    object Favorites : BottomNavScreen("Favorites", Icons.Filled.Favorite)
    object Account : BottomNavScreen("Account", Icons.Filled.Person)
}

@Composable
fun MainScreen(apiKey: String, db: TravelGuideDatabase) {
    var selectedScreen by remember { mutableStateOf<BottomNavScreen>(BottomNavScreen.Home) }
    var city by remember { mutableStateOf("Paris") }
    var recentSearches by remember { mutableStateOf(listOf<String>()) }
    var favorites by remember { mutableStateOf(listOf<FavoritePoi>()) }
    val vm: GuideVM = androidx.lifecycle.viewmodel.compose.viewModel()
    var triggerSearch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load favorites and recent searches from DB on start
    LaunchedEffect(Unit) {
        favorites = withContext(Dispatchers.IO) { db.favoritePoiDao().getAll() }
        recentSearches = withContext(Dispatchers.IO) { db.recentSearchDao().getAll().map { it.searchTerm } }
    }

    // When triggerSearch is set, perform the search and add to recent
    LaunchedEffect(triggerSearch) {
        if (triggerSearch) {
            vm.fetch(city, apiKey)
            if (city.isNotBlank() && !recentSearches.contains(city)) {
                // Add to DB and state
                scope.launch {
                    withContext(Dispatchers.IO) {
                        db.recentSearchDao().insert(RecentSearch(searchTerm = city, timestamp = System.currentTimeMillis()))
                        recentSearches = db.recentSearchDao().getAll().map { it.searchTerm }
                    }
                }
            }
            triggerSearch = false
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(BottomNavScreen.Home, BottomNavScreen.Favorites, BottomNavScreen.Account)
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedScreen) {
                is BottomNavScreen.Home -> HomeWithRecentSearches(
                    city = city,
                    onCityChange = { city = it },
                    recentSearches = recentSearches,
                    onRecentSearch = { search ->
                        city = search
                        triggerSearch = true
                    },
                    onSearch = { triggerSearch = true },
                    vm = vm,
                    apiKey = apiKey,
                    favorites = favorites,
                    onToggleFavorite = { poi ->
                        val fav = FavoritePoi(name = poi.properties.name, lat = poi.geometry.coordinates.getOrNull(1), lon = poi.geometry.coordinates.getOrNull(0), city = city)
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val dao = db.favoritePoiDao()
                                val exists = dao.getAll().any { it == fav }
                                if (exists) {
                                    dao.delete(fav)
                                } else {
                                    dao.insert(fav)
                                }
                                favorites = dao.getAll()
                            }
                        }
                    }
                )
                is BottomNavScreen.Favorites -> FavoritesPage(favorites)
                is BottomNavScreen.Account -> AccountPage()
            }
        }
    }
}

@Composable
fun HomeWithRecentSearches(
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
        if (recentSearches.isNotEmpty()) {
            Text("Recently Searched", style = MaterialTheme.typography.titleMedium)
            LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                items(recentSearches) { search ->
                    OutlinedButton(
                        onClick = { onRecentSearch(search) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text(search) }
                }
            }
        }
        OutlinedTextField(
            value = city,
            onValueChange = onCityChange,
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onSearch,
            enabled = apiKey.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Fetch POIs") }
        Spacer(Modifier.height(20.dp))
        when (val s = vm.state) {
            UiState.Idle     -> Text("Enter a city and tap the button.")
            UiState.Loading  -> CircularProgressIndicator()
            is UiState.Err   -> Text("❌ ${s.msg}", color = MaterialTheme.colorScheme.error)
            is UiState.Ok    -> POIListWithFavorites(s.list, favorites, onToggleFavorite)
        }
    }
}

@Composable
fun POIListWithFavorites(
    pois: List<com.nikita.travelguide.network.PoiFeature>,
    favorites: List<FavoritePoi>,
    onToggleFavorite: (com.nikita.travelguide.network.PoiFeature) -> Unit
) {
    val expandedIndex = remember { mutableStateOf(-1) }
    LazyColumn {
        itemsIndexed(pois) { idx, poi ->
            val isFavorite = favorites.any {
                it.name == poi.properties.name &&
                it.lat == poi.geometry.coordinates.getOrNull(1) &&
                it.lon == poi.geometry.coordinates.getOrNull(0)
            }
            Column(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(poi.properties.name ?: "(no name)") },
                    trailingContent = {
                        IconButton(onClick = { onToggleFavorite(poi) }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
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

@Composable
fun FavoritesPage(favorites: List<FavoritePoi>) {
    if (favorites.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No favorites yet.")
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
            items(favorites) { fav ->
                Column(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(fav.name ?: "(no name)") },
                        supportingContent = {
                            Text("City: ${fav.city ?: "Unknown"}")
                        }
                    )
                    if (fav.lat != null && fav.lon != null) {
                        val mapUrl = "https://maps.geoapify.com/v1/staticmap?style=osm-carto&width=400&height=200&center=lonlat:${fav.lon},${fav.lat}&zoom=15&marker=lonlat:${fav.lon},${fav.lat};color:%23ff0000;size:large&apiKey=${GEOAPIFY_KEY}"
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = mapUrl,
                            contentDescription = "Map for ${fav.name}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun AccountPage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Account Page")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { FirebaseAuth.getInstance().signOut() }) {
                Text("Log Out")
            }
        }
    }
}

@Composable
fun SignInScreen(onSignInSuccess: (FirebaseUser) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (isRegistering) "Register" else "Sign In", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(16.dp))
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
            else Text(if (isRegistering) "Register" else "Sign In")
        }
        TextButton(onClick = { isRegistering = !isRegistering }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isRegistering) "Already have an account? Sign In" else "Don't have an account? Register")
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun AppEntry(apiKey: String, db: TravelGuideDatabase) {
    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { authInstance ->
            user = authInstance.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }
    if (user == null) {
        SignInScreen(onSignInSuccess = { user = it })
    } else {
        MainScreen(apiKey, db)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext,
            TravelGuideDatabase::class.java,
            "travel_guide_db"
        ).build()
        setContent { MaterialTheme { AppEntry(GEOAPIFY_KEY, db) } }
    }
}