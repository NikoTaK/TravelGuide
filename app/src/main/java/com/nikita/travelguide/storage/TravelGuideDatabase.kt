package com.nikita.travelguide.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoritePoi::class, RecentSearch::class],
    version = 1,
    exportSchema = false
)
abstract class TravelGuideDatabase : RoomDatabase() {
    abstract fun favoritePoiDao(): FavoritePoiDao
    abstract fun recentSearchDao(): RecentSearchDao
} 