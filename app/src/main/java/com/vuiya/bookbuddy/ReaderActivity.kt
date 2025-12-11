package com.vuiya.bookbuddy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme
import java.io.File
import java.util.*

class ReaderActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var bookTitle = "Sample Book"
    private var bookLanguage = "latin"
    private var bookPath: String? = null
    private var pageFiles = listOf<File>()
    private var currentPageIndex by mutableStateOf(0)
    private var fontSize by mutableStateOf(16f)
    private var readerTheme by mutableStateOf(ReaderTheme.LIGHT)
    private var isAutoplayEnabled by mutableStateOf(false)

    private lateinit var textToSpeech: TextToSpeech
    private var ttsInitialized = false
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("BookBuddyPrefs", Context.MODE_PRIVATE)
        val selectedEngine = prefs.getString("selectedTtsEngine", null)

        textToSpeech = TextToSpeech(this, this, selectedEngine)

        bookPath = intent.getStringExtra("book_path")
        bookTitle = intent.getStringExtra("book_title") ?: "Untitled Document"
        bookLanguage = intent.getStringExtra("book_language") ?: "latin"

        if (bookPath != null) {
            val bookDir = File(bookPath!!)
            pageFiles = bookDir.listFiles()?.sortedBy { it.name.substringAfter('_').substringBefore('.').toInt() } ?: emptyList()
        }

        setContent {
            BookBuddyTheme {
                val currentPageText = if (pageFiles.isNotEmpty()) {
                    pageFiles[currentPageIndex].readText()
                } else {
                    "No content available. This is a placeholder page."
                }

                ReaderScreen(
                    bookTitle = bookTitle,
                    currentPageText = currentPageText,
                    currentPage = currentPageIndex,
                    pageCount = pageFiles.size,
                    fontSize = fontSize,
                    readerTheme = readerTheme,
                    isAutoplayEnabled = isAutoplayEnabled,
                    onFontSizeChange = { fontSize = it },
                    onThemeChange = { readerTheme = it },
                    onAutoplayChange = { isAutoplayEnabled = it },
                    onPreviousClick = { if (currentPageIndex > 0) currentPageIndex-- },
                    onNextClick = { if (currentPageIndex < pageFiles.size - 1) currentPageIndex++ },
                    onCopyClick = { copyCurrentPageText() },
                    onShareClick = { shareCurrentPageText() },
                    onTranslateClick = { translateCurrentPageText() },
                    onListenAudioClick = { enterTtsMode() },
                    onEditClick = { navigateToEditor() }
                )
            }
        }
    }

    private fun navigateToEditor() {
        val intent = Intent(this, EditorActivity::class.java).apply {
            putExtra("book_path", bookPath)
            putExtra("book_title", bookTitle)
        }
        startActivity(intent)
    }

    private fun getCurrentPageText(): String {
        return if (pageFiles.isNotEmpty()) {
            pageFiles[currentPageIndex].readText()
        } else {
            ""
        }
    }

    private fun copyCurrentPageText() {
        val currentPageText = getCurrentPageText()
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Page Content", currentPageText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Page content copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun shareCurrentPageText() {
        val currentPageText = getCurrentPageText()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, currentPageText)
            putExtra(Intent.EXTRA_SUBJECT, "$bookTitle - Page ${currentPageIndex + 1}")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun translateCurrentPageText() {
        Toast.makeText(this, "Translation functionality will be implemented in full version", Toast.LENGTH_SHORT).show()
    }

    private fun enterTtsMode() {
        if (!ttsInitialized) {
            Toast.makeText(this, "TTS engine is initializing, please try again shortly", Toast.LENGTH_SHORT).show()
            return
        }
        playCurrentPage()
    }

    private fun playCurrentPage() {
        if (!ttsInitialized) {
            Toast.makeText(this, "TTS engine is not ready yet", Toast.LENGTH_SHORT).show()
            return
        }

        val textToRead = getCurrentPageText()
        if (textToRead.trim().isNotEmpty()) {
            textToSpeech.stop()
            textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "PAGE_UTTERANCE")
        } else {
            Toast.makeText(this, "No text to read on this page", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = when (bookLanguage) {
                "bengali" -> Locale("bn")
                else -> Locale.getDefault()
            }
            val result = textToSpeech.setLanguage(locale)

            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "PAGE_UTTERANCE" && isAutoplayEnabled) {
                        runOnUiThread {
                            if (currentPageIndex < pageFiles.size - 1) {
                                currentPageIndex++
                                playCurrentPage()
                            }
                        }
                    }
                }
            })

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("ReaderActivity", "TTS language is not supported for $bookLanguage")
            } else {
                ttsInitialized = true
                Log.d("ReaderActivity", "TTS engine initialized successfully for $bookLanguage")
            }
        } else {
            Log.e("ReaderActivity", "Failed to initialize TTS engine")
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
            bookPath?.let { File(it).deleteRecursively() }
        }
        super.onDestroy()
    }
}
