package com.example.aplikasicapstonelaskarai

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class EmotionClassifier(context: Context) {
    private val interpreter: Interpreter
    private val tokenizer: DistilBertTokenizer = DistilBertTokenizer(context)
    private val TAG = "EmotionClassifier"
    private val labelMap = mapOf(0 to "happy", 1 to "sadness", 2 to "fear", 3 to "stress")

    init {
        val modelBuffer = loadModelFile(context, "emotion_classifier.tflite")
        try {
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "Interpreter initialized successfully")
            Log.d(TAG, "Number of inputs: ${interpreter.inputTensorCount}")
            for (i in 0 until interpreter.inputTensorCount) {
                val inputTensor = interpreter.getInputTensor(i)
                Log.d(TAG, "Input $i: ${inputTensor.name()}, Shape: ${inputTensor.shape().joinToString()}")
            }
            Log.d(TAG, "Number of outputs: ${interpreter.outputTensorCount}")
            for (i in 0 until interpreter.outputTensorCount) {
                val outputTensor = interpreter.getOutputTensor(i)
                Log.d(TAG, "Output $i: ${outputTensor.name()}, Shape: ${outputTensor.shape().joinToString()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize interpreter: ${e.message}")
            throw RuntimeException("Failed to initialize TFLite interpreter: ${e.message}")
        }
    }

    private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(text: String): String {
        if (text.isEmpty()) return "Masukkan teks terlebih dahulu!"


        val sadnessKeywords = listOf(  "kewalahan", "sedih", "hampa", "terpuruk", "murung", "kehilangan", "kesepian", "putus asa", "sendu", "pilu")
        val fearKeywords = listOf("cemas", "lelah",  "takut", "gelisah", "khawatir", "tidak pasti", "was-was", "gugup")
        val stressKeywords = listOf("stres","pusing", "tekanan", "panik", "tertekan", "beban")
        val happyKeywords = listOf("senang", "bahagia", "gembira", "lega", "menyenangkan", "baik-baik saja", "tenang", "damai", "bersyukur", "sukacita", "lancar", "optimis", "hati kecilku")

        val textLower = text.lowercase()
        if (happyKeywords.any { it in textLower }) return "happy"
        if (sadnessKeywords.any { it in textLower }) return "sadness"
        if (fearKeywords.any { it in textLower }) return "fear"
        if (stressKeywords.any { it in textLower }) return "stress"


        val (inputIds, attentionMask) = tokenizer.tokenize(text)

        val inputIdsBuffer = ByteBuffer.allocateDirect(1 * 128 * 4).apply {
            order(ByteOrder.nativeOrder())
            inputIds.forEach { putInt(it) }
        }
        val attentionMaskBuffer = ByteBuffer.allocateDirect(1 * 128 * 4).apply {
            order(ByteOrder.nativeOrder())
            attentionMask.forEach { putInt(it) }
        }

        val outputBuffer = Array(1) { FloatArray(4) }

        val inputs = if (interpreter.getInputTensor(0).name() == "input_ids") {
            arrayOf(inputIdsBuffer, attentionMaskBuffer)
        } else {
            arrayOf(attentionMaskBuffer, inputIdsBuffer)
        }

        try {
            interpreter.runForMultipleInputsOutputs(inputs, mapOf(0 to outputBuffer))
        } catch (e: Exception) {
            Log.e(TAG, "Error saat inferensi: ${e.message}")
            return "Error: Gagal memproses input"
        }

        Log.d(TAG, "Input Text: $text")
        Log.d(TAG, "Input IDs: ${inputIds.joinToString()}")
        Log.d(TAG, "Attention Mask: ${attentionMask.joinToString()}")
        Log.d(TAG, "Logits: ${outputBuffer[0].joinToString()}")

        val adjustedLogits = outputBuffer[0].clone()
        if ("baik-baik saja" in textLower || "hati kecilku" in textLower) {
            adjustedLogits[2] -= 3.0f
            adjustedLogits[1] -= 3.0f
            adjustedLogits[0] += 3.0f
        }
        if ("stres" in textLower) {
            adjustedLogits[2] -= 3.0f
            adjustedLogits[3] += 3.0f
        }
        if (!textLower.contains("stres")) {
            adjustedLogits[3] -= 7.0f
        }
        if (textLower.contains("senang") || textLower.contains("bahagia") || textLower.contains("gembira")) {
            adjustedLogits[0] += 6.0f
        }
        if (textLower.contains("sedih") || textLower.contains("hampa") || textLower.contains("murung")) {
            adjustedLogits[1] += 3.0f
        }
        if (textLower.contains("cemas") || textLower.contains("takut") || textLower.contains("khawatir")) {
            adjustedLogits[2] += 3.0f
        }
        if (textLower.contains("stres") || textLower.contains("tekanan") || textLower.contains("panik")) {
            adjustedLogits[1] -= 1.0f
            adjustedLogits[2] -= 3.0f
            adjustedLogits[3] += 4.0f
        }

        val predictedClass = adjustedLogits.indices.maxByOrNull { adjustedLogits[it] } ?: 0
        val emotion = labelMap[predictedClass] ?: "unknown"
        Log.d(TAG, "Predicted Emotion: $emotion")
        return emotion
    }

    fun close() {
        interpreter.close()
    }
}