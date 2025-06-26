package com.example.travelguide.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import com.example.travelguide.storage.FavoritePoi

@Dao
interface FavoritePoiDao {
    @Insert
    suspend fun insert(favorite: FavoritePoi)

    @Delete
    suspend fun delete(favorite: FavoritePoi)

    @Query("SELECT * FROM favorite_poi")
    suspend fun getAll(): List<FavoritePoi>

    @Query("SELECT * FROM favorite_poi WHERE placeId = :placeId LIMIT 1")
    suspend fun getByPlaceId(placeId: String): FavoritePoi?
}
 