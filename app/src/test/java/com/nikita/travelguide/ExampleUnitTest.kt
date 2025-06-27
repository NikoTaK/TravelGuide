package com.nikita.travelguide

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun favoritePoi_dataClass_equalityAndConstruction() {
        val fav1 = com.nikita.travelguide.storage.FavoritePoi(
            id = 0,
            name = "Eiffel Tower",
            lat = 48.8584,
            lon = 2.2945,
            city = "Paris"
        )
        val fav2 = com.nikita.travelguide.storage.FavoritePoi(
            id = 0,
            name = "Eiffel Tower",
            lat = 48.8584,
            lon = 2.2945,
            city = "Paris"
        )
        assertEquals(fav1, fav2)
        assertEquals("Eiffel Tower", fav1.name)
        assertEquals("Paris", fav1.city)
    }

    @Test
    fun poiFavorite_toggleLogic() {
        val poi = com.nikita.travelguide.network.PoiFeature(
            properties = com.nikita.travelguide.network.PoiProps(name = "Louvre Museum"),
            geometry = com.nikita.travelguide.network.Geometry(listOf(2.3364, 48.8606))
        )
        val city = "Paris"
        val fav = com.nikita.travelguide.storage.FavoritePoi(
            name = poi.properties.name,
            lat = poi.geometry.coordinates.getOrNull(1),
            lon = poi.geometry.coordinates.getOrNull(0),
            city = city
        )
        val favorites = mutableListOf<com.nikita.travelguide.storage.FavoritePoi>()
        // Add to favorites
        if (!favorites.contains(fav)) favorites.add(fav)
        assertTrue(favorites.contains(fav))
        // Remove from favorites
        if (favorites.contains(fav)) favorites.remove(fav)
        assertFalse(favorites.contains(fav))
    }
}