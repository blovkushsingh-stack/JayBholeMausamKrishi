package com.example.jaybhole

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class DiseaseMapActivity : AppCompatActivity(), OnMapReadyCallback, TextToSpeech.OnInitListener {

    private lateinit var mMap: GoogleMap
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        tts = TextToSpeech(this, this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        // 🔹 Starting position: Porsa, Morena
        val porsa = LatLng(26.67, 78.38)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(porsa, 7f))

        fetchDiseaseAlerts()
    }

    private fun fetchDiseaseAlerts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("disease_alerts")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    speak("इस समय कोई बीमारी रिपोर्ट नहीं है।")
                    return@addOnSuccessListener
                }
                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    val lat = (data["lat"] as? Number)?.toDouble() ?: continue
                    val lon = (data["lon"] as? Number)?.toDouble() ?: continue
                    val crop = data["crop"] as? String ?: "फसल"
                    val disease = data["disease"] as? String ?: "अज्ञात"
                    val severity = data["severity"] as? String ?: "Medium"

                    val color = when (severity) {
                        "High" -> BitmapDescriptorFactory.HUE_RED
                        "Medium" -> BitmapDescriptorFactory.HUE_ORANGE
                        else -> BitmapDescriptorFactory.HUE_GREEN
                    }

                    val marker = MarkerOptions()
                        .position(LatLng(lat, lon))
                        .title("$crop: $disease")
                        .snippet("गंभीरता: $severity")
                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                    mMap.addMarker(marker)
                }

                speak("मानचित्र पर बीमारी की रिपोर्टें दिखा दी गई हैं। लाल निशान गंभीर स्थिति को दर्शाते हैं।")
            }
            .addOnFailureListener {
                speak("मानचित्र डेटा लोड करने में समस्या आई।")
            }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS)
            tts.language = Locale("hi", "IN")
    }

    override fun onDestroy() {
        tts.stop(); tts.shutdown()
        super.onDestroy()
    }
}
