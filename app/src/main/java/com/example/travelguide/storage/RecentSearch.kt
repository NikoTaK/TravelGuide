package com.example.travelguide.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_search")
data class RecentSearch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val searchTerm: String,
    val timestamp: Long
)