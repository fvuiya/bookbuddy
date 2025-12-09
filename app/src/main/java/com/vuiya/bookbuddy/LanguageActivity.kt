package com.vuiya.bookbuddy

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme

data class LanguageInfo(val name: String, val isInstalled: Boolean)

class LanguageActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private val engines = mutableStateOf<List<TextToSpeech.EngineInfo>>(emptyList())
    private val selectedEngine = mutableStateOf("")
    private val languageStatus = mutableStateOf<Map<String, LanguageInfo>>(emptyMap())
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("BookBuddyPrefs", Context.MODE_PRIVATE)
        selectedEngine.value = prefs.getString("selectedTtsEngine", "") ?: ""

        tts = TextToSpeech(this, this, selectedEngine.value.ifEmpty { null })

        setContent {
            BookBuddyTheme {
                LanguageScreen(
                    engines = engines.value,
                    selectedEngine = selectedEngine.value,
                    languageStatus = languageStatus.value,
                    onEngineSelected = { engineName ->
                        selectedEngine.value = engineName
                        prefs.edit().putString("selectedTtsEngine", engineName).apply()
                        tts.shutdown()
                        tts = TextToSpeech(this, this, engineName)
                    },
                    onEngineSettingsClick = { engineName ->
                        try {
                            val intent = Intent("com.android.settings.TTS_SETTINGS")
                            intent.setPackage(engineName)
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(this, "Could not open settings for this engine.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onInstallClick = {
                        val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            engines.value = tts.engines
            val currentEngine = selectedEngine.value.ifEmpty { tts.defaultEngine }
            selectedEngine.value = currentEngine

            val languages = tts.voices.map { it.locale }.distinct().associate { locale ->
                val isInstalled = tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
                locale.displayName to LanguageInfo(locale.displayName, isInstalled)
            }
            languageStatus.value = languages
        } else {
            languageStatus.value = mapOf("Error" to LanguageInfo("TTS engine failed to initialize", false))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
