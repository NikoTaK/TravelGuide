package com.nikita.travelguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikita.travelguide.network.GEOAPIFY_KEY
import com.nikita.travelguide.network.Network
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.room.Room
import com.nikita.travelguide.storage.TravelGuideDatabase
import com.nikita.travelguide.storage.FavoritePoi
import com.nikita.travelguide.storage.RecentSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nikita.travelguide.ui.screens.HomeScreen
import com.nikita.travelguide.ui.screens.FavoritesScreen
import com.nikita.travelguide.ui.screens.AccountScreen
import com.nikita.travelguide.ui.theme.TravelGuideTheme
import androidx.compose.foundation.background
import androidx.core.view.WindowCompat
import android.graphics.Color as AndroidColor

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
    var isDarkTheme by remember { mutableStateOf(false) }

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
            Column {
                Divider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
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
        }
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (selectedScreen) {
                is BottomNavScreen.Home -> HomeScreen(
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
                    },
                    darkTheme = isDarkTheme
                )
                is BottomNavScreen.Favorites -> FavoritesScreen(
                    favorites = favorites,
                    onRemoveFavorite = { fav ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.favoritePoiDao().delete(fav)
                                favorites = db.favoritePoiDao().getAll()
                            }
                        }
                    },
                    darkTheme = isDarkTheme
                )
                is BottomNavScreen.Account -> AccountScreen(
                    userEmail = null,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onSignOut = { FirebaseAuth.getInstance().signOut() }
                )
            }
        }
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
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            TravelGuideTheme(darkTheme = isDarkTheme) {
                AppEntryWithTheme(GEOAPIFY_KEY, db, isDarkTheme, onToggleTheme = { isDarkTheme = !isDarkTheme })
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
    }
}

@Composable
fun AppEntryWithTheme(apiKey: String, db: TravelGuideDatabase, isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { authInstance ->
            user = authInstance.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    if (user == null) {
        com.nikita.travelguide.ui.screens.SignInScreen(onSignInSuccess = { user = it }, darkTheme = isDarkTheme)
    } else {
        MainScreenWithTheme(apiKey, db, isDarkTheme, onToggleTheme, user?.email)
    }
}

@Composable
fun MainScreenWithTheme(apiKey: String, db: TravelGuideDatabase, isDarkTheme: Boolean, onToggleTheme: () -> Unit, userEmail: String?) {
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
            Column {
                Divider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
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
        }
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (selectedScreen) {
                is BottomNavScreen.Home -> HomeScreen(
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
                    },
                    darkTheme = isDarkTheme
                )
                is BottomNavScreen.Favorites -> FavoritesScreen(
                    favorites = favorites,
                    onRemoveFavorite = { fav ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.favoritePoiDao().delete(fav)
                                favorites = db.favoritePoiDao().getAll()
                            }
                        }
                    },
                    darkTheme = isDarkTheme
                )
                is BottomNavScreen.Account -> AccountScreen(
                    userEmail = userEmail,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onSignOut = { FirebaseAuth.getInstance().signOut() }
                )
            }
        }
    }
}