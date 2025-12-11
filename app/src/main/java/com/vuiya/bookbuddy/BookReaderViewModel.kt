package com.vuiya.bookbuddy

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BookReaderViewModel(private val bookPath: String?) : ViewModel() {
    val currentPageIndex = mutableStateOf(0)
    val pageCount = mutableStateOf(0)
    val currentPageContent = mutableStateOf("")
    val isLoading = mutableStateOf(false)
    val fontSize = mutableStateOf(16)

    private var pageFiles: List<File> = emptyList()
    private val bookDirectory: File? = bookPath?.let { File(it) }

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch(Dispatchers.IO) {
            bookDirectory?.let { dir ->
                // Load all page files, sorted by page number
                pageFiles = dir.listFiles()
                    ?.filter { it.name.startsWith("page_") && it.name.endsWith(".txt") && !it.name.endsWith(".draft.txt") }
                    ?.mapNotNull { file ->
                        PageNumber.fromFileName(file.name)?.let { pageNum -> pageNum to file }
                    }
                    ?.sortedBy { it.first }
                    ?.map { it.second }
                    ?: emptyList()

                withContext(Dispatchers.Main) {
                    pageCount.value = pageFiles.size
                    if (pageFiles.isNotEmpty()) {
                        loadPage(0)
                    }
                }
            }
        }
    }

    private fun loadPage(index: Int) {
        if (index < 0 || index >= pageFiles.size) return

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isLoading.value = true
            }

            val content = pageFiles[index].readText()

            withContext(Dispatchers.Main) {
                currentPageIndex.value = index
                currentPageContent.value = content
                isLoading.value = false
            }
        }
    }

    fun nextPage() {
        if (currentPageIndex.value < pageFiles.size - 1) {
            loadPage(currentPageIndex.value + 1)
        }
    }

    fun previousPage() {
        if (currentPageIndex.value > 0) {
            loadPage(currentPageIndex.value - 1)
        }
    }

    fun jumpToPage(index: Int) {
        if (index >= 0 && index < pageFiles.size) {
            loadPage(index)
        }
    }

    fun setFontSize(size: Int) {
        fontSize.value = size
    }

    fun getBookPath(): String? {
        return bookPath
    }
}

