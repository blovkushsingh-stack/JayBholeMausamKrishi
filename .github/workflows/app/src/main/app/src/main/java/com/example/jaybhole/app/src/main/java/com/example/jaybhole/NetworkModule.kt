package com.example.jaybhole

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request

// 🌦️ Weather API Data Model
data class WeatherResponse(
    @SerializedName("main") val main: MainWeather,
    @SerializedName("weather") val weather: List<WeatherDesc>
)

data class MainWeather(
    @SerializedName("temp") val temp: Float
)

data class WeatherDesc(
    @SerializedName("description") val description: String
)

// Retrofit Interface for OpenWeather
interface WeatherService {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "hi",
        @Query("appid") apiKey: String = BuildConfig.OPENWEATHER_KEY
    ): WeatherResponse
}

object NetworkModule {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val weatherService by lazy {
        retrofit.create(WeatherService::class.java)
    }

    // 🌦️ Get Weather Function
    suspend fun getWeather(lat: Double, lon: Double): WeatherResult? {
        return try {
            val response = weatherService.getWeather(lat, lon)
            val desc = response.weather.firstOrNull()?.description ?: "अज्ञात"
            WeatherResult(response.main.temp.toInt(), desc)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 🧠 OpenAI simple request (for AI Krishi advice)
    fun askAI(prompt: String): String {
        return try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_KEY}")
                .post(
                    okhttp3.RequestBody.create(
                        okhttp3.MediaType.parse("application/json"),
                        """
                        {
                            "model": "gpt-3.5-turbo-instruct",
                            "prompt": "$prompt",
                            "max_tokens": 100
                        }
                        """.trimIndent()
                    )
                )
                .build()
            val response = client.newCall(request).execute()
            response.body()?.string() ?: "Error"
        } catch (e: Exception) {
            "त्रुटि हुई: ${e.message}"
        }
    }
}

data class WeatherResult(val temp: Int, val description: String)
