package com.example.jaybhole

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tts = TextToSpeech(this, this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btnWeather = findViewById<Button>(R.id.btnWeather)
        val btnDashboard = findViewById<Button>(R.id.btnDashboard)
        val btnDisease = findViewById<Button>(R.id.btnDisease)
        val btnMap = findViewById<Button>(R.id.btnMap)
        val btnForecasts = findViewById<Button>(R.id.btnForecasts)

        btnWeather.setOnClickListener { fetchWeather() }
        btnDashboard.setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }
        btnDisease.setOnClickListener { startActivity(Intent(this, DiseaseDetectionActivity::class.java)) }
        btnMap.setOnClickListener { startActivity(Intent(this, DiseaseMapActivity::class.java)) }
        btnForecasts.setOnClickListener { startActivity(Intent(this, ForecastsActivity::class.java)) }
    }

    private fun fetchWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                speak("कृपया प्रतीक्षा करें, मौसम डेटा लाया जा रहा है।")
                CoroutineScope(Dispatchers.IO).launch {
                    val result = NetworkModule.getWeather(it.latitude, it.longitude)
                    withContext(Dispatchers.Main) {
                        speak("आपके इलाके का तापमान है ${result?.temp} डिग्री सेल्सियस और स्थिति ${result?.description}।")
                    }
                }
            } ?: Toast.makeText(this, "लोकेशन नहीं मिली", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale("hi", "IN")
    }

    override fun onDestroy() {
        tts.stop(); tts.shutdown()
        super.onDestroy()
    }
}
