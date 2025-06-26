package com.example.travelguide.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.travelguide.storage.RecentSearch

@Dao
interface RecentSearchDao {
    @Insert
    suspend fun insert(search: RecentSearch)

    @Delete
    suspend fun delete(search: RecentSearch)

    @Query("SELECT * FROM recent_search ORDER BY timestamp DESC")
    suspend fun getAll(): List<RecentSearch>

    @Query("DELETE FROM recent_search")
    suspend fun clearAll()
}