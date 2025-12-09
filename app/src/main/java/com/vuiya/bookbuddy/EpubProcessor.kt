package com.vuiya.bookbuddy

import android.content.Context
import android.net.Uri
import nl.siegmann.epublib.epub.EpubReader
import java.io.InputStream

class EpubProcessor(private val context: Context) {

    fun processEpub(uri: Uri): EpubProcessingResult {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val book = EpubReader().readEpub(inputStream)
            val stringBuilder = StringBuilder()

            book.contents.forEach { resource ->
                val htmlContent = resource.reader.readText()
                val plainText = htmlContent.replace("<[^>]*>".toRegex(), "")
                stringBuilder.append(plainText)
                stringBuilder.append("\n\n")
            }
            EpubProcessingResult.Success(stringBuilder.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            EpubProcessingResult.Error("Error processing EPUB file: ${e.message}")
        }
    }
}

sealed class EpubProcessingResult {
    data class Success(val content: String) : EpubProcessingResult()
    data class Error(val message: String) : EpubProcessingResult()
}
