package com.example.camera2api

import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import com.example.camera2api.ui.theme.ProcessedImageResponse
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit


// Retrofit client setup
object RetrofitClient {

    private const val BASE_URL = "https://yolov11-api-135428057415.us-central1.run.app"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Set connection timeout
        .readTimeout(30, TimeUnit.SECONDS)     // Set read timeout
        .writeTimeout(30, TimeUnit.SECONDS)    // Set write timeout
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())  // Convert JSON to Kotlin objects
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

// ApiService interface for network requests
interface ApiService {

    @Multipart
    @POST("detect/")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<ProcessedImageResponse>
}
