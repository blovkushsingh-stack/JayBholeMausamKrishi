package com.example.jaybhole

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ForecastsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val items = mutableListOf<String>()
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecasts)

        listView = findViewById(R.id.listReports)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter

        tts = TextToSpeech(this, this)

        loadReports()

        listView.setOnItemClickListener { _, _, position, _ ->
            val report = items[position]
            speak(report.take(300))
        }
    }

    private fun loadReports() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reports = FirestoreHelper.getReports()
                runOnUiThread {
                    items.clear()
                    if (reports.isEmpty()) {
                        Toast.makeText(this@ForecastsActivity, "कोई रिपोर्ट नहीं मिली।", Toast.LENGTH_SHORT).show()
                    } else {
                        reports.forEach { r ->
                            val date = r["date"] ?: "Unknown"
                            val text = r["reportText"] ?: "Empty"
                            items.add("📅 $date\n$text")
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@ForecastsActivity, "रिपोर्ट लोड करने में त्रुटि।", Toast.LENGTH_SHORT).show()
                }
            }
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
