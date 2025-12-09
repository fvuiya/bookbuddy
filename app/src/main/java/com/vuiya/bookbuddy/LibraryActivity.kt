package com.vuiya.bookbuddy

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme

class LibraryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // TODO: Load library items from a ViewModel
            val libraryItems = remember {
                mutableStateListOf(
                    LibraryItem("The Great Gatsby", "F. Scott Fitzgerald", "English", "Book"),
                    LibraryItem("To Kill a Mockingbird", "Harper Lee", "English", "Book"),
                    LibraryItem("1984", "George Orwell", "English", "Book"),
                    LibraryItem("Pride and Prejudice", "Jane Austen", "Spanish", "Book"),
                    LibraryItem("The Catcher in the Rye", "J.D. Salinger", "English", "Book")
                )
            }

            BookBuddyTheme {
                LibraryScreen(libraryItems = libraryItems, onRemoveItem = {
                    libraryItems.remove(it)
                    Toast.makeText(this, "Removed: ${it.title}", Toast.LENGTH_SHORT).show()
                })
            }
        }
    }
}
