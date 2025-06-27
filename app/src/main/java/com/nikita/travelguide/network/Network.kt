package com.nikita.travelguide.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object Network {
    private val retrofit by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.geoapify.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }).build()
            )
            .build()
    }
    val api: GeoapifyService by lazy {
        retrofit.create(GeoapifyService::class.java)
    }
}
