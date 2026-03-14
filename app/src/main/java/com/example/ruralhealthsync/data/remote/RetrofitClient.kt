package com.example.ruralhealthsync.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton that provides the configured [Retrofit] instance and [ApiService].
 *
 * The base URL points to the local Android emulator's loopback address (10.0.2.2),
 * which maps to the host machine's localhost where XAMPP/Apache is running.
 * Update [BASE_URL] to your production server URL before release.
 */
object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2/ruralhealth_api/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
