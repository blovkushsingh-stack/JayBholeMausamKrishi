package com.example.jaybhole

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*

class DiseaseDetectionActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var imgPreview: ImageView
    private lateinit var tvResult: TextView
    private lateinit var btnPick: Button
    private lateinit var btnAnalyze: Button
    private lateinit var tts: TextToSpeech
    private var imageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease)

        imgPreview = findViewById(R.id.imgPreview)
        tvResult = findViewById(R.id.tvResult)
        btnPick = findViewById(R.id.btnPickImage)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        tts = TextToSpeech(this, this)

        btnPick.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(512)
                .maxResultSize(512, 512)
                .start()
        }

        btnAnalyze.setOnClickListener {
            imageBitmap?.let {
                analyzeImageWithAI(it)
            } ?: speak("कृपया पहले पत्ती की फोटो चुनें।")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            imgPreview.setImageURI(uri)
            imageBitmap = android.provider.MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        }
    }

    private fun analyzeImageWithAI(bitmap: Bitmap) {
        speak("कृपया प्रतीक्षा करें, फोटो का विश्लेषण किया जा रहा है।")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val base64Image = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.DEFAULT)

                val prompt = """
                    आप एक कृषि वैज्ञानिक हैं।
                    इस पत्ती की फोटो देखकर बताइए कि कौन सी बीमारी है,
                    उसका कारण क्या है और उसका उपचार क्या होगा।
                    उत्तर केवल हिंदी में दीजिए।
                """.trimIndent()

                val result = NetworkModule.askAI("फसल बीमारी पहचानें:\n$prompt\n\n[Image Data]")
                runOnUiThread {
                    tvResult.text = result
                    speak(result.take(250))
                }
                CoroutineScope(Dispatchers.IO).launch {
                    FirestoreHelper.saveReport("पोरसा, मध्यप्रदेश", "बीमारी रिपोर्ट: $result")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    speak("क्षमा करें, बीमारी पहचानने में त्रुटि हुई।")
                }
            }
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
