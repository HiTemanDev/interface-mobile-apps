package com.example.aplikasicapstonelaskarai

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.util.Random
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ActScriptManager(private val context: Context) {
    private val scripts = mutableMapOf<String, List<String>>(
        "happy" to emptyList(),
        "sadness" to emptyList(),
        "fear" to emptyList(),
        "stress" to emptyList()
    )
    private val githubUrls = mapOf(
        "happy" to "https://raw.githubusercontent.com/Sidqiamn/Dataset_Capstone_LaskarAI/refs/heads/master/act_happy.txt",
        "sadness" to "https://raw.githubusercontent.com/Sidqiamn/Dataset_Capstone_LaskarAI/refs/heads/master/act_sadness.txt",
        "fear" to "https://raw.githubusercontent.com/Sidqiamn/Dataset_Capstone_LaskarAI/refs/heads/master/act_fear.txt",
        "stress" to "https://raw.githubusercontent.com/Sidqiamn/Dataset_Capstone_LaskarAI/refs/heads/master/act_stress.txt"
    )
    private val queue = Volley.newRequestQueue(context)
    private val TAG = "ActScriptManager"
    private var isLoadingComplete = false
    private val loadingPromises = mutableListOf<(Boolean) -> Unit>()

    init {
        loadScripts()
    }

    private fun loadScripts() {
        var pendingRequests = githubUrls.size
        githubUrls.forEach { (emotion, url) ->
            val request = StringRequest(Request.Method.GET, url,
                { response ->
                    val parsedScripts = parseScripts(response)
                    scripts[emotion] = parsedScripts
                    Log.d(TAG, "Loaded ${parsedScripts.size} scripts for $emotion")
                    pendingRequests--
                    if (pendingRequests == 0) {
                        isLoadingComplete = true
                        notifyLoadingComplete(true)
                    }
                },
                { error ->
                    Log.e(TAG, "Failed to load scripts for $emotion: ${error.message}")
                    scripts[emotion] = listOf("Skrip default untuk $emotion...")
                    pendingRequests--
                    if (pendingRequests == 0) {
                        isLoadingComplete = true
                        notifyLoadingComplete(false)
                    }
                })
            queue.add(request)
        }
    }

    private fun parseScripts(content: String): List<String> {
        val scripts = mutableListOf<String>()
        var currentScript = mutableListOf<String>()
        content.split("\n").forEach { line ->
            val trimmedLine = line.trim()
            // Abaikan baris metadata yang dimulai dengan "#"
            if (trimmedLine.startsWith("#")) {
                return@forEach
            }
            if (trimmedLine.startsWith("## Skrip") && currentScript.isNotEmpty()) {
                val scriptText = currentScript.joinToString("\n").trim()
                if (scriptText.isNotEmpty()) {
                    scripts.add(scriptText)
                }
                currentScript = mutableListOf()
            } else if (trimmedLine.isNotEmpty()) {
                currentScript.add(trimmedLine)
            }
        }
        if (currentScript.isNotEmpty()) {
            val scriptText = currentScript.joinToString("\n").trim()
            if (scriptText.isNotEmpty()) {
                scripts.add(scriptText)
            }
        }
        return scripts
    }

    private fun notifyLoadingComplete(success: Boolean) {
        loadingPromises.forEach { it(success) }
        loadingPromises.clear()
    }

    suspend fun getRandomScript(emotion: String): String = suspendCoroutine { continuation ->
        if (isLoadingComplete) {
            val script = fetchRandomScript(emotion)
            continuation.resume(script)
        } else {
            loadingPromises.add { success ->
                val script = fetchRandomScript(emotion)
                continuation.resume(script)
            }
        }
    }

    private fun fetchRandomScript(emotion: String): String {
        val emotionLower = emotion.lowercase()
        val emotionScripts = scripts[emotionLower] ?: listOf("Maaf, skrip tidak tersedia untuk $emotion.")
        return if (emotionScripts.isNotEmpty()) {
            val selectedScript = emotionScripts[Random().nextInt(emotionScripts.size)]
            Log.d(TAG, "Selected script for $emotion: $selectedScript")
            selectedScript
        } else {
            Log.w(TAG, "No scripts available for $emotion")
            "Maaf, skrip tidak tersedia untuk $emotion."
        }
    }
}