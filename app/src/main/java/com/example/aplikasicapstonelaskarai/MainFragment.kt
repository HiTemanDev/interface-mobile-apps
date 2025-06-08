package com.example.aplikasicapstonelaskarai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplikasicapstonelaskarai.databinding.FragmentMainBinding
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.concurrent.TimeUnit

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var classifier: EmotionClassifier
    private lateinit var scriptManager: ActScriptManager
    private lateinit var ttsManager: TtsManager
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var currentScript: String? = null
    private var initialEmotion: String? = null
    private var isFirstMessage: Boolean = true
    private val TAG = "MainFragment"

    private val fragmentScope = CoroutineScope(Dispatchers.Main)
    private var apiJob: Job? = null // Job untuk API call
    private var geminiJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        messages.clear()
        currentScript = null
        initialEmotion = null
        isFirstMessage = true
        Log.d(TAG, "onCreateView: Data reset - messages cleared, isFirstMessage=$isFirstMessage")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            classifier = EmotionClassifier(requireContext())
            Log.d(TAG, "EmotionClassifier initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EmotionClassifier: ${e.message}")
            _binding?.textViewError?.text = "Error: Gagal menginisialisasi model"
            return
        }

        scriptManager = ActScriptManager(requireContext())
        ttsManager = TtsManager(requireContext()) { success, errorMessage ->
            activity?.runOnUiThread {
                if (success) {
                    _binding?.textViewError?.text = errorMessage ?: ""
                    if (currentScript != null) {
                        _binding?.buttonPlayScript?.isEnabled = true
                        _binding?.buttonPlayScript?.setBackgroundColor(android.graphics.Color.parseColor("#34C759"))
                    }
                } else {
                    _binding?.textViewError?.text = errorMessage ?: "Error: Gagal menginisialisasi Text-to-Speech"
                    _binding?.buttonPlayScript?.isEnabled = false
                    _binding?.buttonPlayScript?.setBackgroundColor(android.graphics.Color.parseColor("#B0BEC5"))
                    showInstallTtsButton()
                }
            }
        }

        chatAdapter = ChatAdapter(messages)
        _binding?.chatRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        _binding?.chatRecyclerView?.adapter = chatAdapter

        addMessage("Ceritakan perasaanmu saat ini!", false)
        _binding?.buttonPlayScript?.isEnabled = false

        _binding?.sendButton?.setOnClickListener {
            val userMessage = _binding?.inputMessage?.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessage(userMessage, true)
                _binding?.inputMessage?.text?.clear()
                processUserMessage(userMessage)
            } else {
                _binding?.textViewError?.text = "Masukkan teks terlebih dahulu!"
            }
        }

        _binding?.buttonPlayScript?.setOnClickListener {
            if (currentScript != null && currentScript!!.isNotEmpty() && currentScript != "Maaf, skrip tidak tersedia.") {
                apiJob = fragmentScope.launch {
                    val audioData = callTherapyApi(initialEmotion ?: "neutral")
                    if (audioData != null) {
                        val bundle = Bundle().apply {
                            putString("script", currentScript!!)
                            putByteArray("audioData", audioData)
                        }
                        withContext(Dispatchers.Main) {
                            _binding?.let {
                                findNavController().navigate(R.id.action_mainFragment_to_audioFragment, bundle)
                                it.textViewError.text = "Memutar audio terapi..."
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _binding?.textViewError?.text = "Tidak ada audio untuk diputar"
                        }
                    }
                }
            } else {
                _binding?.textViewError?.text = "Tidak ada audio untuk diputar"
            }
        }

        _binding?.infofragment?.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToInfoFragment()
            findNavController().navigate(action)
        }
    }

    private fun showInstallTtsButton() {
        val buttonInstallTts = Button(requireContext()).apply {
            text = "Install TTS"
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts")))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }
        _binding?.root?.addView(buttonInstallTts)
    }

    private fun addMessage(message: String, isUser: Boolean) {
        messages.add(ChatMessage(message, isUser))
        _binding?.chatRecyclerView?.post {
            chatAdapter.notifyDataSetChanged()
            _binding?.chatRecyclerView?.scrollToPosition(messages.size - 1)
        }
    }

    private fun processUserMessage(inputText: String) {
        try {
            if (isFirstMessage) {
                val emotion = classifier.predict(inputText)
                initialEmotion = emotion
                Log.d(TAG, "Initial emotion predicted: $initialEmotion")

                apiJob = fragmentScope.launch {
                    val audioData = callTherapyApi(emotion)
                    withContext(Dispatchers.Main) {
                        _binding?.let {
                            if (audioData != null) {
                                currentScript = "audio_wav"
                                it.buttonPlayScript.isEnabled = true
                                it.buttonPlayScript.setBackgroundColor(android.graphics.Color.parseColor("#34C759"))
                                it.textViewError.text = "Audio terapi siap diputar"
                            } else {
                                it.buttonPlayScript.isEnabled = false
                                it.buttonPlayScript.setBackgroundColor(android.graphics.Color.parseColor("#B0BEC5"))
                                it.textViewError.text = "Audio terapi tidak tersedia"
                            }
                            isFirstMessage = false
                        }
                    }
                }
            }
            val emotionToUse = initialEmotion ?: "neutral"
            modelCall(inputText, emotionToUse)
        } catch (e: Exception) {
            Log.e(TAG, "Prediction failed: ${e.message}")
            addMessage("Gagal memproses pesan: ${e.message}", false)
            _binding?.let {
                it.buttonPlayScript.isEnabled = false
                it.buttonPlayScript.setBackgroundColor(android.graphics.Color.parseColor("#B0BEC5"))
                it.textViewError.text = "Error: ${e.message}"
            }
        }
    }

    private suspend fun callTherapyApi(emotion: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build()
                val requestBody = okhttp3.RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    """{"emotion": "$emotion"}"""
                )
                val request = okhttp3.Request.Builder()
                    .url("https://2b27-34-23-42-238.ngrok-free.app/generate_therapy_audio")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Log.e(TAG, "API failed with code: ${response.code} - ${response.message}")
                    withContext(Dispatchers.Main) {
                        _binding?.textViewError?.text = "Error: Server gagal (Kode ${response.code})"
                    }
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "API call failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    _binding?.textViewError?.text = "Error: Koneksi gagal - ${e.message}"
                }
                null
            }
        }
    }

    private fun modelCall(prompt: String, emotion: String) {
        _binding?.loadingProgressBar?.visibility = View.VISIBLE

        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = ""
        )

        geminiJob = fragmentScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    "Bayangkan kamu adalah seorang teman yang peduli dan empatik. Berdasarkan emosi '$emotion' yang dirasakan pengguna, berikan satu respons yang sangat mendukung, relevan dengan konteks, dan penuh empati untuk: '$prompt'. Jangan memberikan opsi atau daftar respons, tetapi langsung berikan satu respons terbaik yang alami dan terasa seperti percakaran dengan teman dekat. Jika konteks tidak jelas, buat asumsi yang masuk akal berdasarkan emosi dan input pengguna, lalu tawarkan bantuan spesifik."
                )

                if (geminiJob?.isActive == true) {
                    withContext(Dispatchers.Main) {
                        _binding?.let {
                            it.loadingProgressBar.visibility = View.GONE
                            addMessage(response.text.toString(), false)
                        }
                    }
                } else {
                    Log.d(TAG, "Coroutine cancelled, skipping UI update")
                }
            } catch (e: Exception) {
                if (geminiJob?.isActive == true) {
                    withContext(Dispatchers.Main) {
                        _binding?.let {
                            it.loadingProgressBar.visibility = View.GONE
                            addMessage("Error: ${e.message}", false)
                        }
                    }
                } else {
                    Log.d(TAG, "Coroutine cancelled, skipping error UI update")
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        apiJob?.cancel()
        geminiJob?.cancel()
        apiJob = null
        geminiJob = null
        _binding?.loadingProgressBar?.visibility = View.GONE
        Log.d(TAG, "onStop: Jobs cancelled")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        classifier.close()
        ttsManager.shutdown()
        _binding = null
    }
}