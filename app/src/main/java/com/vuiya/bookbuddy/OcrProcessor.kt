package com.vuiya.bookbuddy

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class OcrProcessor(private val context: Context) {

    private val executorService = Executors.newSingleThreadExecutor()
    private val languageRecognizers = mutableMapOf<String, TextRecognizer>()
    private val defaultRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        initializeCommonRecognizers()
    }

    private fun initializeCommonRecognizers() {
        Log.d(TAG, "Initialized common language recognizers")
    }

    fun processImage(bitmap: Bitmap, forceLanguageCode: String?): CompletableFuture<OcrResult> {
        return CompletableFuture.supplyAsync({ 
            try {
                Log.d(TAG, "Starting OCR processing. Force language: $forceLanguageCode")

                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = selectRecognizer(forceLanguageCode)

                val visionText = recognizer.process(image).result

                val recognizedText = StringBuilder()
                for (block in visionText.textBlocks) {
                    recognizedText.append(block.text).append("\n")
                }

                val resultText = recognizedText.toString().trim()
                Log.d(TAG, "OCR processing completed. Text length: ${resultText.length}")
                Log.d(TAG, "Recognized text snippet: ${if (resultText.length > 100) resultText.substring(0, 100) + "..." else resultText}")

                OcrResult(true, "OCR successful", resultText, forceLanguageCode)

            } catch (e: Exception) {
                Log.e(TAG, "Error during OCR processing", e)
                OcrResult(false, "OCR failed: ${e.message}", "", forceLanguageCode)
            }
        }, executorService)
    }

    private fun selectRecognizer(languageHint: String?): TextRecognizer {
        if (languageHint.isNullOrEmpty()) {
            Log.d(TAG, "No language hint provided, using default multi-language recognizer.")
            return defaultRecognizer
        }

        languageRecognizers[languageHint.lowercase()]?.let {
            Log.d(TAG, "Using pre-initialized recognizer for language: $languageHint")
            return it
        }

        Log.d(TAG, "Using default recognizer with hint for language: $languageHint")
        return defaultRecognizer
    }

    fun release() {
        Log.d(TAG, "Releasing OCR processor resources")
        defaultRecognizer.close()
        languageRecognizers.values.forEach { it.close() }
        if (!executorService.isShutdown) {
            executorService.shutdown()
        }
        Log.d(TAG, "OCR processor resources released")
    }

    data class OcrResult(
        val success: Boolean,
        val message: String,
        val extractedText: String,
        val languageUsed: String?
    )

    companion object {
        private const val TAG = "OcrProcessor"
    }
}
