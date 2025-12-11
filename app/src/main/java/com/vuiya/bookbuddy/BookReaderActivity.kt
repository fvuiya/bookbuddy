package com.vuiya.bookbuddy

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme
import java.io.File
import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView

class BookReaderActivity : ComponentActivity() {
    private lateinit var viewModel: BookReaderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bookPath = intent.getStringExtra("book_path")
        val bookTitle = intent.getStringExtra("book_title") ?: "Book"

        viewModel = BookReaderViewModel(bookPath)

        setContent {
            BookBuddyTheme {
                BookReaderScreen(
                    bookTitle = bookTitle,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(bookTitle: String, viewModel: BookReaderViewModel) {
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current
    val isLoading = viewModel.isLoading.value
    var showMenu by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = viewModel.fontSize.value,
            onDismiss = { showFontSizeDialog = false },
            onSizeSelected = { size ->
                viewModel.setFontSize(size)
                showFontSizeDialog = false
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
                                    text = { Text("Edit") },
                                    onClick = {
                                        showMenu = false
                                        // Open editor at current page
                                        val intent = android.content.Intent(context, EditorActivity::class.java).apply {
                                            putExtra("book_path", viewModel.getBookPath())
                                            putExtra("book_title", bookTitle)
                                            putExtra("initial_page", viewModel.currentPageIndex.value)
                                        }
                                        context.startActivity(intent)
                                        activity?.finish()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Font Size") },
                                    onClick = {
                                        showFontSizeDialog = true
                                        showMenu = false
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
            BookReaderControls(
                onPreviousClick = { viewModel.previousPage() },
                onNextClick = { viewModel.nextPage() },
                onJumpToPage = { viewModel.jumpToPage(it) },
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
                RichTextReader(
                    key = viewModel.currentPageIndex.value,
                    content = viewModel.currentPageContent.value,
                    fontSize = viewModel.fontSize.value
                )
            }
        }
    }
}

@Composable
fun BookReaderControls(
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    currentPage: Int,
    pageCount: Int,
    isLoading: Boolean
) {
    var jumpToPageValue by remember { mutableStateOf((currentPage + 1).toString()) }
    var isEditingJumpField by remember { mutableStateOf(false) }

    // Update displayed page number when user navigates
    LaunchedEffect(currentPage) {
        if (!isEditingJumpField) {
            jumpToPageValue = (currentPage + 1).toString()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column {
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)

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
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
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
        }
    }
}

@Composable
fun FontSizeDialog(
    currentSize: Int,
    onDismiss: () -> Unit,
    onSizeSelected: (Int) -> Unit
) {
    val fontSizes = listOf(12, 14, 16, 18, 20, 22, 24, 28, 32)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Font Size") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fontSizes.forEach { size ->
                    TextButton(
                        onClick = { onSizeSelected(size) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${size}pt", style = MaterialTheme.typography.bodyLarge)
                            if (size == currentSize) {
                                Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RichTextReader(key: Int, content: String, fontSize: Int) {
    var isPageLoaded by remember { mutableStateOf(false) }
    var lastPageKey by remember { mutableStateOf(key) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    // When page changes, reload HTML
    LaunchedEffect(key) {
        if (lastPageKey != key) {
            lastPageKey = key
            isPageLoaded = false
            webViewRef.value?.loadUrl("file:///android_asset/reader.html")
        }
    }

    // When page finishes loading, set the content
    LaunchedEffect(isPageLoaded, key, content) {
        if (isPageLoaded && content.isNotEmpty()) {
            val escapedContent = org.json.JSONObject.quote(content)
            webViewRef.value?.evaluateJavascript("javascript:setContent($escapedContent, $fontSize);", null)
        } else if (isPageLoaded && content.isEmpty()) {
            // Empty page
            webViewRef.value?.evaluateJavascript("javascript:setContent('', $fontSize);", null)
        }
    }

    // Update font size when it changes
    LaunchedEffect(fontSize) {
        if (isPageLoaded) {
            webViewRef.value?.evaluateJavascript("javascript:setFontSize($fontSize);", null)
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

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isPageLoaded = true
                    }
                }

                loadUrl("file:///android_asset/reader.html")
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
