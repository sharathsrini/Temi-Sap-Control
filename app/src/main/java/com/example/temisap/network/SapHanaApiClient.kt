package com.example.temisap.network

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SapHanaApiClient {
    /**
     * LOCAL NETWORK OPTION:
     * If your robot is on the SAME network (192.168.1.10), 
     * and your SAP BAS is running locally or via a local bridge,
     * you could use a local IP here.
     * 
     * CURRENT: Still using the stable Cloudflare tunnel to reach SAP BAS.
     */
    private const val BASE_URL = "https://taste-wow-garage-angle.trycloudflare.com/odata/v4/robot/"

    private const val USERNAME = "alice" 
    private const val PASSWORD = ""

    val instance: SapHanaService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("Authorization", Credentials.basic(USERNAME, PASSWORD))
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()

        retrofit.create(SapHanaService::class.java)
    }
}
