package com.vuiya.bookbuddy

import android.app.Activity
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    engines: List<TextToSpeech.EngineInfo>,
    selectedEngine: String,
    languageStatus: Map<String, LanguageInfo>,
    onEngineSelected: (String) -> Unit,
    onEngineSettingsClick: (String) -> Unit,
    onInstallClick: () -> Unit
) {
    val activity = (LocalContext.current as? Activity)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Language Settings") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Select TTS Engine:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            engines.forEach { engine ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (engine.name == selectedEngine),
                        onClick = { onEngineSelected(engine.name) }
                    )
                    Text(
                        text = engine.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .clickable { onEngineSettingsClick(engine.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Language Status:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (languageStatus.isEmpty()) {
                Text("No languages found for this engine.")
            } else {
                languageStatus.toSortedMap().forEach { (_, langInfo) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = langInfo.name)
                        if (langInfo.isInstalled) {
                            Text("Installed")
                        } else {
                            Button(onClick = onInstallClick) {
                                Text("Install")
                            }
                        }
                    }
                }
            }
        }
    }
}
