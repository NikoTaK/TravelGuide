package com.nikita.travelguide.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import com.nikita.travelguide.storage.FavoritePoi

@Dao
interface FavoritePoiDao {
    @Insert
    suspend fun insert(favorite: FavoritePoi)

    @Delete
    suspend fun delete(favorite: FavoritePoi)

    @Query("SELECT * FROM favorite_poi")
    suspend fun getAll(): List<FavoritePoi>

}
 