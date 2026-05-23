package com.example.ruralhealthsync.data.remote

import android.content.Context
import com.example.ruralhealthsync.data.local.PreferenceManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provides a configured [ApiService] using the server URL saved in preferences.
 * Use [getApiService] so login can change the PC IP without rebuilding the app.
 */
object RetrofitClient {

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedApiService: ApiService? = null

    fun getApiService(context: Context): ApiService {
        val baseUrl = PreferenceManager(context.applicationContext).getServerUrl()
        val existing = cachedApiService
        if (existing != null && cachedBaseUrl == baseUrl) {
            return existing
        }
        return synchronized(this) {
            if (cachedApiService != null && cachedBaseUrl == baseUrl) {
                cachedApiService!!
            } else {
                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                cachedBaseUrl = baseUrl
                Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)
                    .also { cachedApiService = it }
            }
        }
    }

    fun invalidateCache() {
        synchronized(this) {
            cachedBaseUrl = null
            cachedApiService = null
        }
    }
}
