package com.vuiya.bookbuddy

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(libraryItems: List<LibraryItem>, onRemoveItem: (LibraryItem) -> Unit) {
    val context = LocalContext.current
    val activity = (context as? Activity)

    var sortOption by remember { mutableStateOf(SortOption.TITLE) }
    var languageFilter by remember { mutableStateOf<String?>(null) }

    val filteredAndSortedItems = libraryItems
        .filter { languageFilter == null || it.language == languageFilter }
        .sortedWith(
            when (sortOption) {
                SortOption.TITLE -> compareBy { it.title }
                SortOption.AUTHOR -> compareBy { it.author }
            }
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SortAndFilterControls(sortOption, onSortChange = { sortOption = it }, languageFilter, onLanguageChange = { languageFilter = it }, libraryItems)
            if (filteredAndSortedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your library is empty\nStart scanning books or opening PDFs to build your library",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(filteredAndSortedItems) { item ->
                        LibraryItem(item = item, onRemoveClick = { onRemoveItem(item) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortAndFilterControls(sortOption: SortOption, onSortChange: (SortOption) -> Unit, languageFilter: String?, onLanguageChange: (String?) -> Unit, libraryItems: List<LibraryItem>) {
    val languages = listOf("All") + libraryItems.map { it.language }.distinct()
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            FilterChip(selected = sortOption == SortOption.TITLE, onClick = { onSortChange(SortOption.TITLE) }, label = { Text("Sort by Title") })
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(selected = sortOption == SortOption.AUTHOR, onClick = { onSortChange(SortOption.AUTHOR) }, label = { Text("Sort by Author") })
        }

        Box {
            TextButton(onClick = { expanded = true }) {
                Text(languageFilter ?: "All Languages")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(text = { Text(language) }, onClick = {
                        onLanguageChange(if (language == "All") null else language)
                        expanded = false
                    })
                }
            }
        }
    }
}

enum class SortOption {
    TITLE,
    AUTHOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryItem(item: LibraryItem, onRemoveClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = {
            val intent = Intent(context, ReaderActivity::class.java).apply {
                putExtra("book_title", item.title)
            }
            context.startActivity(intent)
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_book),
                contentDescription = "Book",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = item.author, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(
                        text = item.language,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = item.type,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            IconButton(onClick = onRemoveClick) {
                Icon(painter = painterResource(id = R.drawable.ic_delete), contentDescription = "Delete")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LibraryScreenPreview() {
    val sampleItems = remember {
        mutableStateListOf(
            LibraryItem("The Great Gatsby", "F. Scott Fitzgerald", "English", "Book"),
            LibraryItem("To Kill a Mockingbird", "Harper Lee", "English", "Book"),
            LibraryItem("1984", "George Orwell", "English", "Book"),
            LibraryItem("Pride and Prejudice", "Jane Austen", "Spanish", "Book")
        )
    }
    BookBuddyTheme {
        LibraryScreen(libraryItems = sampleItems, onRemoveItem = { sampleItems.remove(it) })
    }
}
