package com.example.emotionclassifierapp_laskarai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.emotionclassifierapp_laskarai.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var classifier: EmotionClassifier
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi EmotionClassifier
        classifier = EmotionClassifier(this)

        // Set listener untuk tombol prediksi
        binding.predictButton.setOnClickListener {
            val textInput = binding.textInput.text.toString()
            val result = classifier.predictWithKeywords(textInput)
            binding.resultText.text = "Emosi: $result"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
    }
}