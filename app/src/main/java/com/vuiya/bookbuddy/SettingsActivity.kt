package com.vuiya.bookbuddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookBuddyTheme {
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val activity = (context as? Activity)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AppearanceSettings()
            Spacer(modifier = Modifier.height(16.dp))
            LanguageManagementSettings()
            Spacer(modifier = Modifier.height(16.dp))
            ExportSettings()
        }
    }
}

@Composable
fun AppearanceSettings() {
    SettingsCard(title = "Appearance") {
        SettingRowSpinner(label = "Theme", options = listOf("System Default", "Light", "Dark"))
        SettingRowSpinner(label = "Font", options = listOf("Default", "Serif", "Sans-serif"))
    }
}

@Composable
fun LanguageManagementSettings() {
    val context = LocalContext.current
    SettingsCard(title = "Language Management") {
        Button(
            onClick = { context.startActivity(Intent(context, LanguageActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Downloaded Languages")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { Toast.makeText(context, "Download languages feature coming soon!", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download New Languages")
        }
    }
}

@Composable
fun ExportSettings() {
    var isSearchable by remember { mutableStateOf(true) }
    var isAutoExport by remember { mutableStateOf(false) }

    SettingsCard(title = "Export Preferences") {
        SettingRowSwitch(label = "Default to Searchable PDF", checked = isSearchable, onCheckedChange = { isSearchable = it })
        SettingRowSwitch(label = "Auto-export after translation", checked = isAutoExport, onCheckedChange = { isAutoExport = it })
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingRowSpinner(label: String, options: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(options[0]) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selectedOption)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        selectedOption = option
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
fun SettingRowSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    BookBuddyTheme {
        SettingsScreen()
    }
}
