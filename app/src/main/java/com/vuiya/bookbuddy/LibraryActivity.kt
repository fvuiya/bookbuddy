package com.vuiya.bookbuddy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme
import java.io.File

class LibraryActivity : ComponentActivity() {
    private var refreshTrigger = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val items = remember { mutableStateListOf<LibraryItem>() }
            val trigger by refreshTrigger

            // Reload items whenever trigger changes
            LaunchedEffect(trigger) {
                loadLibraryItems(items)
            }

            BookBuddyTheme {
                LibraryScreen(
                    libraryItems = items,
                    onRemoveItem = { book ->
                        items.remove(book)
                        book.filePath?.let { File(it).deleteRecursively() }
                    },
                    onOpenBook = { book ->
                        // Open published books in BookReaderActivity, drafts in EditorActivity
                        val intent = if (book.category == "Draft") {
                            Intent(this@LibraryActivity, EditorActivity::class.java).apply {
                                putExtra("book_path", book.filePath)
                                putExtra("book_title", book.title)
                            }
                        } else {
                            Intent(this@LibraryActivity, BookReaderActivity::class.java).apply {
                                putExtra("book_path", book.filePath)
                                putExtra("book_title", book.title)
                            }
                        }
                        startActivity(intent)
                    },
                    onPublishBook = { book ->
                        book.filePath?.let { path ->
                            File(path, ".draft").delete()
                            book.category = "Book"
                            // Reload library to reflect changes
                            loadLibraryItems(items)
                        }
                    },
                    onCreateBook = { title, author, language ->
                        createNewBook(title, author, language)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Trigger a refresh by incrementing the counter
        refreshTrigger.value++
    }

    private fun loadLibraryItems(items: MutableList<LibraryItem>) {
        items.clear()
        val libraryDir = File(filesDir, "library")
        if (!libraryDir.exists()) {
            libraryDir.mkdirs()
        }
        val books = libraryDir.listFiles()?.filter { it.isDirectory }?.map {
            val isDraft = File(it, ".draft").exists()
            LibraryItem(it.name, "Unknown Author", "English", if (isDraft) "Draft" else "Book", it.absolutePath)
        } ?: emptyList()
        items.addAll(books)
    }

    private fun createNewBook(title: String, author: String, language: String) {
        val libraryDir = File(filesDir, "library")
        if (!libraryDir.exists()) {
            libraryDir.mkdirs()
        }

        // Create book directory
        val bookDir = File(libraryDir, title)
        if (bookDir.exists()) {
            // If book with same name exists, append a number
            var counter = 1
            var uniqueBookDir = File(libraryDir, "$title ($counter)")
            while (uniqueBookDir.exists()) {
                counter++
                uniqueBookDir = File(libraryDir, "$title ($counter)")
            }
            uniqueBookDir.mkdirs()
            createInitialPage(uniqueBookDir)
            openBookInEditor(uniqueBookDir.absolutePath, uniqueBookDir.name)
        } else {
            bookDir.mkdirs()
            createInitialPage(bookDir)
            openBookInEditor(bookDir.absolutePath, title)
        }
    }

    private fun createInitialPage(bookDir: File) {
        // Create first page with proper numbering (page_1.txt)
        val page1 = File(bookDir, "page_1.txt")
        page1.writeText("")

        // Mark as draft
        File(bookDir, ".draft").createNewFile()
    }

    private fun openBookInEditor(bookPath: String, bookTitle: String) {
        val intent = Intent(this, EditorActivity::class.java).apply {
            putExtra("book_path", bookPath)
            putExtra("book_title", bookTitle)
        }
        startActivity(intent)
    }
}
