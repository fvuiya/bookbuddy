package com.vuiya.bookbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme

class EditorActivity : ComponentActivity() {
    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookPath = intent.getStringExtra("book_path")
        val bookTitle = intent.getStringExtra("book_title") ?: "Editor"
        val initialPage = intent.getIntExtra("initial_page", 0)

        viewModel.loadBook(bookPath, initialPage)

        setContent {
            BookBuddyTheme {
                EditorScreen(
                    bookTitle = bookTitle,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.onActivityStop()
    }
}
