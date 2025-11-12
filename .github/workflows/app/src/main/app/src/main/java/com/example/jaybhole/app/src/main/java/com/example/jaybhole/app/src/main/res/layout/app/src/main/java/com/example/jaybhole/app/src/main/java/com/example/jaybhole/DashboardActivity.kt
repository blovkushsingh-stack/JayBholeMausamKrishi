package com.example.jaybhole

import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvAdvice: TextView
    private lateinit var btnAI: Button
    private lateinit var btnPDF: Button
    private lateinit var tts: TextToSpeech
    private var aiText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvAdvice = findViewById(R.id.tvAdvice)
        btnAI = findViewById(R.id.btnAI)
        btnPDF = findViewById(R.id.btnPDF)
        tts = TextToSpeech(this, this)

        btnAI.setOnClickListener {
            speak("कृपया प्रतीक्षा करें, एआई कृषि सलाह तैयार की जा रही है।")
            CoroutineScope(Dispatchers.IO).launch {
                val prompt = "आज के मौसम के अनुसार किसान को कौन सी फसल बोनी चाहिए और क्या सावधानी रखनी चाहिए?"
                val result = NetworkModule.askAI(prompt)
                aiText = result
                withContext(Dispatchers.Main) {
                    tvAdvice.text = aiText
                    speak("सलाह तैयार है। ${aiText.take(200)}")
                    FirestoreHelper.saveReport("पोरसा, मध्यप्रदेश", aiText)
                }
            }
        }

        btnPDF.setOnClickListener {
            if (aiText.isEmpty()) {
                speak("पहले रिपोर्ट बनाएं फिर पीडीएफ तैयार करें।")
            } else {
                createPDF(aiText)
            }
        }
    }

    private fun createPDF(text: String) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        paint.color = Color.parseColor("#2E7D32")
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("🌾 Jay Bhole Mausam Krishi Report", 80f, 50f, paint)

        val textPaint = Paint()
        textPaint.textSize = 14f
        textPaint.color = Color.BLACK

        val lines = text.split("\n")
        var y = 100f
        for (line in lines) {
            canvas.drawText(line, 40f, y, textPaint)
            y += 25
        }

        val footerPaint = Paint()
        footerPaint.color = Color.parseColor("#2E7D32")
        canvas.drawText(
            "जय भोले — समृद्ध फसल की शुभकामनाएँ",
            80f, 800f, footerPaint
        )

        pdf.finishPage(page)
        val file = File(cacheDir, "JayBholeReport.pdf")
        pdf.writeTo(FileOutputStream(file))
        pdf.close()
        speak("पीडीएफ रिपोर्ट तैयार हो गई है।")

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
        intent.type = "application/pdf"
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )
        intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(android.content.Intent.createChooser(intent, "Share Report"))
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
