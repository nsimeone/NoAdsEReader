package com.example.noadse_reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    NAME("Name"),
    SIZE("Size"),
    DATE("Date added")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var sortBy by rememberSaveable { mutableStateOf(SortOption.NAME) }
    var ascending by rememberSaveable { mutableStateOf(true) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    val booksFlow = remember { LibraryDatabase.get(context).books().observeAll() }
    val books by booksFlow.collectAsState(initial = emptyList())

    val visibleBooks = remember(books, query, sortBy, ascending) {
        val filtered = if (query.isBlank()) {
            books
        } else {
            books.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
            }
        }
        val sorted = when (sortBy) {
            SortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
            SortOption.SIZE -> filtered.sortedBy { it.sizeBytes }
            SortOption.DATE -> filtered.sortedBy { it.addedAt }
        }
        if (ascending) sorted else sorted.reversed()
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Remove this book?") },
            text = {
                Text(
                    "\"${book.title}\" will be removed from this app, along with your " +
                            "reading position. Your original file isn't touched."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { deleteBook(context, book) }
                    bookToDelete = null
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search title or author") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortOption.entries.forEach { option ->
                FilterChip(
                    selected = sortBy == option,
                    onClick = {
                        if (sortBy == option) {
                            ascending = !ascending
                        } else {
                            sortBy = option
                            ascending = option == SortOption.NAME
                        }
                    },
                    label = {
                        val arrow = if (sortBy == option) {
                            if (ascending) " ↑" else " ↓"
                        } else {
                            ""
                        }
                        Text(option.label + arrow)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Showing ${visibleBooks.size} of ${books.size} book(s)",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Long-press a cover to remove a book.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (books.isEmpty()) {
            Text("Your library is empty. Open the Import tab to add books.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visibleBooks, key = { it.fileName }) { book ->
                    val progress = book.progress ?: 0.0
                    Column(
                        modifier = Modifier.combinedClickable(
                            onClick = { ReaderActivity.start(context, book.fileName) },
                            onLongClick = { bookToDelete = book }
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            BookCover(
                                coverPath = book.coverPath,
                                title = book.title,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (progress > 0.0) {
                                LinearProgressIndicator(
                                    progress = { progress.toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = when (sortBy) {
                            SortOption.NAME -> book.author
                            SortOption.SIZE -> "%.1f MB".format(book.sizeBytes / 1_048_576.0)
                            SortOption.DATE -> DateFormat.getDateInstance(DateFormat.MEDIUM)
                                .format(Date(book.addedAt))
                        }
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (progress > 0.0) {
                            val label = if (progress >= 0.99) {
                                "Finished"
                            } else {
                                "${(progress * 100).toInt()}% read"
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}