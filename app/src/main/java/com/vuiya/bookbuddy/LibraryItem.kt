package com.vuiya.bookbuddy

data class LibraryItem(
    var title: String,
    var author: String,
    var language: String,
    var type: String,
    var filePath: String? = null,
    var dateAdded: Long = System.currentTimeMillis()
)
