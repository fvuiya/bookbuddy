package com.vuiya.bookbuddy

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditorViewModel : ViewModel() {

    private var pageFiles = listOf<File>()
    private var bookDirectory: File? = null
    val currentPageIndex = mutableStateOf(0)
    val currentPageContent = mutableStateOf("")
    val pageCount = mutableStateOf(0)
    val isLoading = mutableStateOf(false)
    val saveComplete = MutableSharedFlow<Unit>()  // Only for manual saves
    private var autoSaveJob: Job? = null

    fun loadBook(path: String?, initialPage: Int = 0) {
        if (path == null) return
        viewModelScope.launch {
            isLoading.value = true

            withContext(Dispatchers.IO) {
                bookDirectory = File(path)
                // Create .draft marker file to indicate this book is being edited
                File(bookDirectory, ".draft").createNewFile()
            }

            pageFiles = withContext(Dispatchers.IO) {
                File(path).listFiles()?.filter {
                    !it.name.endsWith(".draft.txt") && it.name != ".draft"
                }?.mapNotNull { file ->
                    PageNumber.fromFileName(file.name)?.let { pageNum -> pageNum to file }
                }?.sortedBy { it.first }?.map { it.second } ?: emptyList()
            }
            pageCount.value = pageFiles.size

            if (pageFiles.isNotEmpty()) {
                // Jump to the specified initial page (from reader) or page 0
                val targetPage = initialPage.coerceIn(0, pageFiles.size - 1)
                jumpToPage(targetPage)  // This will set isLoading to false after content loads
            } else {
                isLoading.value = false
            }
        }
    }

    fun onContentChanged(newContent: String) {
        currentPageContent.value = newContent
    }

    fun saveAndNavigate(next: Boolean) {
        autoSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            // Save current page
            saveDraft()

            withContext(Dispatchers.Main) {
                if (next) {
                    jumpToPage(currentPageIndex.value + 1)
                } else {
                    jumpToPage(currentPageIndex.value - 1)
                }
            }
        }
    }

    fun jumpToPage(pageIndex: Int) {
        if (pageIndex !in pageFiles.indices) return

        autoSaveJob?.cancel()
        viewModelScope.launch {
            isLoading.value = true

            // Save current page BEFORE changing pageIndex
            withContext(Dispatchers.IO) {
                saveDraft()
            }

            // NOW update page index
            currentPageIndex.value = pageIndex

            // Load new page content
            withContext(Dispatchers.IO) {
                val pageFile = pageFiles[pageIndex]
                val draftFile = File(pageFile.parent, "${pageFile.nameWithoutExtension}.draft.txt")
                val contentToLoad = if (draftFile.exists()) draftFile.readText() else pageFile.readText()
                withContext(Dispatchers.Main) {
                    currentPageContent.value = contentToLoad
                    startAutoSave()
                    isLoading.value = false
                }
            }
        }
    }

    fun commitCurrentPage() {
        autoSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            // Commit all draft files to main files
            pageFiles.forEach { pageFile ->
                val draftFile = File(pageFile.parent, "${pageFile.nameWithoutExtension}.draft.txt")
                if (draftFile.exists()) {
                    // Copy draft content to main file
                    pageFile.writeText(draftFile.readText())
                    // Delete the draft file
                    draftFile.delete()
                }
            }

            // Remove .draft marker file to mark book as published
            bookDirectory?.let {
                File(it, ".draft").delete()
            }

            withContext(Dispatchers.Main) {
                saveComplete.emit(Unit)  // Only emit for manual saves
                startAutoSave()
            }
        }
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(10000)
            saveDraft()  // Silent auto-save, no toast
            startAutoSave()
        }
    }

    private suspend fun saveDraft() {
        withContext(Dispatchers.IO) {
            val pageFile = pageFiles[currentPageIndex.value]
            val draftFile = File(pageFile.parent, "${pageFile.nameWithoutExtension}.draft.txt")
            draftFile.writeText(currentPageContent.value)
        }
    }

    fun onActivityStop() {
        autoSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            saveDraft()
        }
    }

    fun addNewPage() {
        autoSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            // Save current page first
            saveDraft()

            // Get the last page number and increment
            val lastPageNumber = if (pageFiles.isNotEmpty()) {
                PageNumber.fromFileName(pageFiles.last().name) ?: PageNumber("0")
            } else {
                PageNumber("0")
            }
            val newPageNumber = lastPageNumber.next()

            // Create new page file
            val newPageFile = File(bookDirectory, PageNumber.toFileName(newPageNumber))
            newPageFile.writeText("")

            // Reload page files list with proper sorting
            pageFiles = bookDirectory?.listFiles()?.filter {
                !it.name.endsWith(".draft.txt") && it.name != ".draft"
            }?.mapNotNull { file ->
                PageNumber.fromFileName(file.name)?.let { pageNum -> pageNum to file }
            }?.sortedBy { it.first }?.map { it.second } ?: emptyList()

            withContext(Dispatchers.Main) {
                pageCount.value = pageFiles.size
                // Jump to the new page
                jumpToPage(pageFiles.size - 1)
            }
        }
    }

    /**
     * Inserts a new page after the current page
     * Example: current page is 1, creates 1.1
     */
    fun insertPageAfter() {
        autoSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            // Save current page first
            saveDraft()

            if (pageFiles.isEmpty()) {
                // If no pages, create page 1
                val newPageFile = File(bookDirectory, "page_1.txt")
                newPageFile.writeText("")
            } else {
                // Get current page number and create sub-page
                val currentPageFile = pageFiles[currentPageIndex.value]
                val currentPageNumber = PageNumber.fromFileName(currentPageFile.name) ?: PageNumber("1")
                val newPageNumber = currentPageNumber.insertAfter()

                // Create new page file
                val newPageFile = File(bookDirectory, PageNumber.toFileName(newPageNumber))
                newPageFile.writeText("")
            }

            // Reload page files list with proper sorting
            pageFiles = bookDirectory?.listFiles()?.filter {
                !it.name.endsWith(".draft.txt") && it.name != ".draft"
            }?.mapNotNull { file ->
                PageNumber.fromFileName(file.name)?.let { pageNum -> pageNum to file }
            }?.sortedBy { it.first }?.map { it.second } ?: emptyList()

            withContext(Dispatchers.Main) {
                pageCount.value = pageFiles.size
                // Jump to the newly inserted page (right after current)
                jumpToPage(currentPageIndex.value + 1)
            }
        }
    }

    /**
     * Inserts a new page before the current page
     * Example: if current page is 2, finds the previous page (1 or 1.x) and inserts between them
     */
    fun insertPageBefore() {
        autoSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            // Save current page first
            saveDraft()

            if (pageFiles.isEmpty()) {
                // If no pages, create page 1
                val newPageFile = File(bookDirectory, "page_1.txt")
                newPageFile.writeText("")
            } else if (currentPageIndex.value == 0) {
                // If we're on the first page, create a page before it
                val firstPageFile = pageFiles[0]
                val firstPageNumber = PageNumber.fromFileName(firstPageFile.name) ?: PageNumber("1")

                // Create 0.9 or similar page number that comes before
                val parts = firstPageNumber.value.split(".")
                val newPageNumber = if (parts.size == 1) {
                    // If it's a whole number like "1", create "0.9"
                    PageNumber("0.9")
                } else {
                    // If it's like "1.1", create a page just before it
                    val lastPart = parts.last().toIntOrNull() ?: 1
                    val prefix = parts.dropLast(1).joinToString(".")
                    PageNumber("$prefix.${lastPart - 1}.9")
                }

                val newPageFile = File(bookDirectory, PageNumber.toFileName(newPageNumber))
                newPageFile.writeText("")
            } else {
                // Get previous page number and insert after it
                val previousPageFile = pageFiles[currentPageIndex.value - 1]
                val previousPageNumber = PageNumber.fromFileName(previousPageFile.name) ?: PageNumber("1")
                val newPageNumber = previousPageNumber.insertAfter()

                // Create new page file
                val newPageFile = File(bookDirectory, PageNumber.toFileName(newPageNumber))
                newPageFile.writeText("")
            }

            // Reload page files list with proper sorting
            pageFiles = bookDirectory?.listFiles()?.filter {
                !it.name.endsWith(".draft.txt") && it.name != ".draft"
            }?.mapNotNull { file ->
                PageNumber.fromFileName(file.name)?.let { pageNum -> pageNum to file }
            }?.sortedBy { it.first }?.map { it.second } ?: emptyList()

            withContext(Dispatchers.Main) {
                pageCount.value = pageFiles.size
                // Stay on the same logical position (which is now index + 1 because we inserted before)
                jumpToPage(currentPageIndex.value)
            }
        }
    }

    /**
     * Exports the book to a publishable format
     */
    fun exportBook(format: ExportFormat, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                bookDirectory?.let { dir ->
                    val exporter = BookExporter(context)
                    val bookTitle = dir.name
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val outputFile = File(downloadsDir, "$bookTitle.${format.extension}")

                    when (format) {
                        ExportFormat.PLAIN_TEXT -> exporter.exportToPlainText(dir, outputFile)
                        ExportFormat.MARKDOWN -> exporter.exportToMarkdown(dir, outputFile)
                        ExportFormat.HTML -> exporter.exportToCleanHtml(dir, outputFile, bookTitle, "Unknown Author")
                    }

                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Exported to: ${outputFile.absolutePath}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Export failed: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
