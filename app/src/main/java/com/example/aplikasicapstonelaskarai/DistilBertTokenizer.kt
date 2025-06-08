package com.example.aplikasicapstonelaskarai

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class DistilBertTokenizer(context: Context) {
    private val vocab: Map<String, Int>
    private val maxLength = 128

    init {
        val vocabMap = mutableMapOf<String, Int>()
        try {
            context.assets.open("vocab.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines().forEachIndexed { index, token ->
                        vocabMap[token.trim()] = index
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Gagal memuat vocab.txt: ${e.message}")
        }
        vocab = vocabMap
    }

    fun tokenize(text: String): Pair<IntArray, IntArray> {
        val tokens = mutableListOf("[CLS]")
        val words = text.lowercase().split(" ").filter { it.isNotEmpty() }
        words.forEach { word ->
            if (vocab.containsKey(word)) {
                tokens.add(word)
            } else {

                val subwords = word.chunked(3).map { "##$it" }
                subwords.forEach { subword ->
                    tokens.add(vocab[subword]?.let { vocab[subword].toString() } ?: "[UNK]")
                }
            }
        }
        tokens.add("[SEP]")

        val inputIds = IntArray(maxLength) { vocab["[PAD]"] ?: 0 }
        val attentionMask = IntArray(maxLength) { 0 }
        val endIndex = minOf(tokens.size, maxLength - 1)

        for (i in 0 until endIndex) {
            inputIds[i] = vocab[tokens[i]] ?: vocab["[UNK]"] ?: 100
            attentionMask[i] = 1
        }

        return Pair(inputIds, attentionMask)
    }
}