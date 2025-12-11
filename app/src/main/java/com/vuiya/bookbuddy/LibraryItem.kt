package com.vuiya.bookbuddy

data class LibraryItem(
    var title: String,
    var author: String,
    var language: String,
    var category: String,  // "Draft" or "Book"
    var filePath: String? = null,
    var dateAdded: Long = System.currentTimeMillis()
)
