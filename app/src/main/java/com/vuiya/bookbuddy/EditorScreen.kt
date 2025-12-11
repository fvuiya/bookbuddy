package com.vuiya.bookbuddy

import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(bookTitle: String, viewModel: EditorViewModel) {
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current
    val isLoading = viewModel.isLoading.value
    var showExportDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.saveComplete) {
        viewModel.saveComplete.collectLatest {
            Toast.makeText(context, "Book published to library!", Toast.LENGTH_SHORT).show()
        }
    }

    if (showExportDialog) {
        ExportDialog(
            bookTitle = bookTitle,
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                viewModel.exportBook(format, context)
                showExportDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            bookTitle,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { activity?.finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // 3-dot menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Publish") },
                                    onClick = {
                                        viewModel.commitCurrentPage()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_book),
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export") },
                                    onClick = {
                                        showExportDialog = true
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
            }
        },
        bottomBar = {
            EditorControls(
                onPreviousClick = { viewModel.saveAndNavigate(false) },
                onNextClick = { viewModel.saveAndNavigate(true) },
                onJumpToPage = { viewModel.jumpToPage(it) },
                onAddPageBefore = { viewModel.insertPageBefore() },
                onInsertPageAfter = { viewModel.insertPageAfter() },
                onAddPageAtEnd = { viewModel.addNewPage() },
                currentPage = viewModel.currentPageIndex.value,
                pageCount = viewModel.pageCount.value,
                isLoading = isLoading
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading page...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                RichTextEditor(
                    key = viewModel.currentPageIndex.value,
                    content = viewModel.currentPageContent.value
                ) {
                    viewModel.onContentChanged(it)
                }
            }
        }
    }
}

@Composable
fun EditorControls(
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    onAddPageBefore: () -> Unit,
    onInsertPageAfter: () -> Unit,
    onAddPageAtEnd: () -> Unit,
    currentPage: Int,
    pageCount: Int,
    isLoading: Boolean = false
) {
    var jumpToPageValue by remember { mutableStateOf((currentPage + 1).toString()) }
    var isEditingJumpField by remember { mutableStateOf(false) }

    // Only update displayed page number when user navigates (not while typing)
    LaunchedEffect(currentPage) {
        if (!isEditingJumpField) {
            jumpToPageValue = (currentPage + 1).toString()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)

            // Page Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Previous button
                IconButton(
                    onClick = onPreviousClick,
                    enabled = currentPage > 0,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Previous Page",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Page indicator
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Page ${currentPage + 1} of $pageCount",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    } else {
                        Text(
                            "Page ${currentPage + 1} of $pageCount",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Jump field
                TextField(
                    value = jumpToPageValue,
                    onValueChange = {
                        jumpToPageValue = it
                        isEditingJumpField = it.isNotEmpty()
                    },
                    label = { Text("Page", style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(70.dp).height(40.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                // Go button
                Button(
                    onClick = {
                        jumpToPageValue.toIntOrNull()?.let {
                            if (it in 1..pageCount) {
                                onJumpToPage(it - 1)
                                isEditingJumpField = false
                            }
                        }
                    },
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Go", style = MaterialTheme.typography.labelSmall)
                }

                // Next button
                IconButton(
                    onClick = onNextClick,
                    enabled = currentPage < pageCount - 1,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Next Page",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Page Management Buttons Row
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add Page Before
                Button(
                    onClick = onAddPageBefore,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Before", style = MaterialTheme.typography.labelMedium)
                }

                // Insert Page After
                Button(
                    onClick = onInsertPageAfter,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("After", style = MaterialTheme.typography.labelMedium)
                }

                // Add Page at End
                Button(
                    onClick = onAddPageAtEnd,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("At End", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RichTextEditor(key: Int, content: String, onContentChanged: (String) -> Unit) {
    var isPageLoaded by remember { mutableStateOf(false) }
    var lastPageKey by remember { mutableStateOf(key) }

    val webViewRef = remember {
        mutableStateOf<WebView?>(null)
    }

    // When page changes, reload HTML and reset state
    LaunchedEffect(key) {
        if (lastPageKey != key) {
            lastPageKey = key
            isPageLoaded = false
            webViewRef.value?.loadUrl("file:///android_asset/editor.html")
        }
    }

    // When page finishes loading, set the content
    LaunchedEffect(isPageLoaded, key) {
        if (isPageLoaded && content.isNotEmpty()) {
            val escapedContent = JSONObject.quote(content)
            webViewRef.value?.evaluateJavascript("javascript:setContent($escapedContent);", null)
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef.value = this
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onContentChanged(newContent: String) {
                        onContentChanged(newContent)
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isPageLoaded = true
                    }
                }
                loadUrl("file:///android_asset/editor.html")
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    bookTitle: String,
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Book") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Choose export format for publishing:",
                    style = MaterialTheme.typography.bodyMedium
                )

                ExportFormat.values().forEach { format ->
                    Card(
                        onClick = { onExport(format) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                format.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                format.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class ExportFormat(val displayName: String, val description: String, val extension: String) {
    PLAIN_TEXT("Plain Text", "Simple text file, no formatting", "txt"),
    MARKDOWN("Markdown", "Formatted text for GitHub, editors", "md"),
    HTML("Clean HTML", "For PDF conversion or web publishing", "html")
}
