package com.example.travelguide.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_poi")
data class FavoritePoi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String?,
    val lat: Double?,
    val lon: Double?,
    val placeId: String?
)