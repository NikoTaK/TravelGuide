package com.example.travelguide.network

import com.example.travelguide.data.Results
import retrofit2.http.GET
import retrofit2.http.Query

/* ---------- generic wrapper ---------- */
data class FeatureCollection<T>(val features: List<T>)


/* ---------- places feature (!! new) ---------- */
data class PoiFeature(
    val properties: PoiProps,
    val geometry: Geometry
)
data class PoiProps(val name: String?)
data class Geometry(
    val coordinates: List<Double>
)

const val GEOAPIFY_KEY="6c3bdc26967b4e81957aa33b9aeac4cd"

/* ---------- Retrofit endpoints ---------- */
interface GeoapifyService {

    @GET("v1/geocode/search?format=json")
    suspend fun geocode(
        @Query("text")     city: String,
        @Query("apiKey")   key:  String           // ← parameter name is **key**
    ): Results

    @GET("v2/places")
    suspend fun poisByPlaceId(
        @Query("categories") categories: String = "tourism.attraction",
        @Query("filter") filter: String,
        @Query("limit") limit: Int = 20,
        @Query("apiKey") key: String
    ): FeatureCollection<PoiFeature>
}
