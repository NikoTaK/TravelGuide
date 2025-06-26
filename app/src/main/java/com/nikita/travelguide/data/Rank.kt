package com.nikita.travelguide.data

data class Rank(
    val confidence: Int?,
    val confidence_city_level: Int?,
    val importance: Double?,
    val match_type: String?,
    val popularity: Double?
)