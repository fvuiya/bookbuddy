package com.vuiya.bookbuddy

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vuiya.bookbuddy.ui.theme.*

enum class ReaderTheme(val backgroundColor: Color, val textColor: Color) {
    LIGHT(White, Black),
    DARK(Black, White),
    SEPIA(Sepia, DarkBrown)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookTitle: String,
    currentPageText: String,
    currentPage: Int,
    pageCount: Int,
    fontSize: Float,
    readerTheme: ReaderTheme,
    isAutoplayEnabled: Boolean,
    onFontSizeChange: (Float) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onAutoplayChange: (Boolean) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onTranslateClick: () -> Unit,
    onListenAudioClick: () -> Unit
) {
    val activity = (LocalContext.current as? Activity)
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = readerTheme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(bookTitle, color = readerTheme.textColor) },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = "Back", tint = readerTheme.textColor)
                    }
                },
                actions = {
                    IconButton(onClick = onListenAudioClick) {
                        Icon(painter = painterResource(id = R.drawable.ic_play_arrow), contentDescription = "Listen Audio", tint = readerTheme.textColor)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = "Settings", tint = readerTheme.textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = readerTheme.backgroundColor)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentPageText,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.5).sp,
                    color = readerTheme.textColor
                )
            }
            ReaderControls(currentPage = currentPage, pageCount = pageCount, onPreviousClick, onNextClick, onCopyClick, onShareClick, onTranslateClick)
        }
    }

    if (showSettingsDialog) {
        ReaderSettingsDialog(
            fontSize = fontSize,
            onFontSizeChange = onFontSizeChange,
            readerTheme = readerTheme,
            onThemeChange = onThemeChange,
            isAutoplayEnabled = isAutoplayEnabled,
            onAutoplayChange = onAutoplayChange,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsDialog(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    readerTheme: ReaderTheme,
    onThemeChange: (ReaderTheme) -> Unit,
    isAutoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reader Settings") },
        text = {
            Column {
                Text("Font Size: ${fontSize.toInt()}")
                Slider(
                    value = fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 12f..32f,
                    steps = 19
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Theme")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = readerTheme == ReaderTheme.LIGHT, onClick = { onThemeChange(ReaderTheme.LIGHT) }, label = { Text("Light") })
                    FilterChip(selected = readerTheme == ReaderTheme.DARK, onClick = { onThemeChange(ReaderTheme.DARK) }, label = { Text("Dark") })
                    FilterChip(selected = readerTheme == ReaderTheme.SEPIA, onClick = { onThemeChange(ReaderTheme.SEPIA) }, label = { Text("Sepia") })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Autoplay", modifier = Modifier.weight(1f))
                    Switch(checked = isAutoplayEnabled, onCheckedChange = onAutoplayChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ReaderControls(
    currentPage: Int,
    pageCount: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onTranslateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Page ${currentPage + 1} of $pageCount", modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onPreviousClick, enabled = currentPage > 0) {
                Text("Previous")
            }
            Button(onClick = onNextClick, enabled = currentPage < pageCount - 1) {
                Text("Next")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onCopyClick) {
                Text("Copy")
            }
            Button(onClick = onShareClick) {
                Text("Share")
            }
            Button(onClick = onTranslateClick) {
                Text("Translate")
            }
        }
    }
}
