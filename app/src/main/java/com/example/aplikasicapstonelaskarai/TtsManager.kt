package com.example.aplikasicapstonelaskarai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TtsManager(context: Context, private val onInitializationListener: (Boolean, String?) -> Unit) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val TAG = "TtsManager"
    private var onSpeechCompleteListener: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context, this, "com.google.android.tts")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            var result = tts?.setLanguage(Locale("id", "ID")) ?: TextToSpeech.LANG_NOT_SUPPORTED
            var errorMessage: String? = null
            var success = true

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Bahasa Indonesia tidak didukung, mencoba fallback ke bahasa Inggris")
                result = tts?.setLanguage(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    success = false
                    errorMessage = "Gagal memutar audio. Silakan instal Google Text-to-Speech dari Play Store atau periksa pengaturan TTS di perangkat Anda."
                } else {
                    errorMessage = "Bahasa Indonesia tidak tersedia. Audio akan diputar dalam bahasa Inggris."
                }
            }

            if (success && errorMessage == null) {
                Log.d(TAG, "TextToSpeech initialized successfully for Indonesian")
            }

            isInitialized = success
            onInitializationListener(success, errorMessage)

            // Set listener untuk mendeteksi penyelesaian pemutaran
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "Speech started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "Speech completed: $utteranceId")
                    onSpeechCompleteListener?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "Speech error: $utteranceId")
                }
            })
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status: $status")
            onInitializationListener(false, "Gagal menginisialisasi Text-to-Speech. Pastikan Google Text-to-Speech terinstal.")
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (isInitialized) {
            onSpeechCompleteListener = onComplete
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TtsManagerUtterance")
            Log.d(TAG, "Playing speech: $text")
        } else {
            Log.w(TAG, "TextToSpeech belum diinisialisasi, tidak dapat memutar: $text")
            onInitializationListener(false, "Audio belum siap. Silakan tunggu atau periksa pengaturan TTS.")
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            Log.d(TAG, "Stopping speech")
        } else {
            Log.w(TAG, "TextToSpeech belum diinisialisasi, tidak bisa menghentikan speech")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
        onSpeechCompleteListener = null
        Log.d(TAG, "TextToSpeech shutdown")
    }
}