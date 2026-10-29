package com.example.noadse_reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ReaderControls(
    visible: Boolean,
    progress: Float,
    fontSize: Double,
    theme: PageTheme,
    bookmarks: List<BookmarkEntity>,
    contents: List<TocEntry>,
    highlights: List<HighlightEntity>, // NEW
    editingHighlight: HighlightEntity?, // NEW
    onFontSize: (Double) -> Unit,
    onTheme: (PageTheme) -> Unit,
    onSeek: (Float) -> Unit,
    onGoToStart: () -> Unit,
    onAddBookmark: () -> Unit,
    onOpenBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onOpenContents: (Int) -> Unit,
    onOpenHighlight: (HighlightEntity) -> Unit, // NEW
    onEditHighlight: (HighlightEntity) -> Unit, // NEW
    onSaveNote: (HighlightEntity, String) -> Unit, // NEW
    onDeleteHighlight: (HighlightEntity) -> Unit, // NEW
    onDismissEdit: () -> Unit, // NEW
    onClose: () -> Unit
) {
    var showBookmarks by remember { mutableStateOf(false) }
    var showContents by remember { mutableStateOf(false) }
    var showHighlights by remember { mutableStateOf(false) } // NEW

    // NEW: the note editor. It opens when you tap a highlight on the page,
    // or press "Note" in the Highlights list.
    if (editingHighlight != null) {
        var noteText by remember(editingHighlight.id) {
            mutableStateOf(editingHighlight.note ?: "")
        }
        AlertDialog(
            onDismissRequest = onDismissEdit,
            title = { Text("Highlight") },
            text = {
                Column {
                    Text(
                        text = editingHighlight.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Your note") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onSaveNote(editingHighlight, noteText) }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onDeleteHighlight(editingHighlight) }) {
                        Text("Remove")
                    }
                    TextButton(onClick = onDismissEdit) { Text("Cancel") }
                }
            }
        )
    }

    // NEW: the list of all highlights in this book.
    if (showHighlights) {
        AlertDialog(
            onDismissRequest = { showHighlights = false },
            title = { Text("Highlights") },
            text = {
                if (highlights.isEmpty()) {
                    Text(
                        "No highlights yet. Press and hold a word in the book, drag to " +
                                "select, then choose a color."
                    )
                } else {
                    LazyColumn {
                        items(highlights, key = { it.id }) { highlight ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onOpenHighlight(highlight)
                                        showHighlights = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp, end = 10.dp)
                                        .size(14.dp)
                                        .background(
                                            Color(HighlightColor.fromName(highlight.color).argb),
                                            CircleShape
                                        )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = highlight.text,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!highlight.note.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Note: ${highlight.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    highlight.progress?.let {
                                        Text(
                                            "${(it * 100).roundToInt()}% through the book",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row {
                                        TextButton(onClick = { onEditHighlight(highlight) }) {
                                            Text(
                                                if (highlight.note.isNullOrBlank()) "Add note"
                                                else "Edit note"
                                            )
                                        }
                                        TextButton(onClick = { onDeleteHighlight(highlight) }) {
                                            Text("Remove")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHighlights = false }) { Text("Close") }
            }
        )
    }

    if (showBookmarks) {
        AlertDialog(
            onDismissRequest = { showBookmarks = false },
            title = { Text("Bookmarks") },
            text = {
                if (bookmarks.isEmpty()) {
                    Text("No bookmarks yet. Tap \"Add bookmark\" on any page.")
                } else {
                    LazyColumn {
                        items(bookmarks, key = { it.id }) { bookmark ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onOpenBookmark(bookmark)
                                        showBookmarks = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bookmark.label,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val percent = bookmark.progress
                                        ?.let { "${(it * 100).roundToInt()}% through the book" }
                                        ?: ""
                                    if (percent.isNotEmpty()) {
                                        Text(percent, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                TextButton(onClick = { onDeleteBookmark(bookmark) }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarks = false }) { Text("Close") }
            }
        )
    }

    if (showContents) {
        AlertDialog(
            onDismissRequest = { showContents = false },
            title = { Text("Contents") },
            text = {
                if (contents.isEmpty()) {
                    Text(
                        "This book doesn't include a chapter list. If it has a contents " +
                                "page, look for it near the start of the book."
                    )
                } else {
                    LazyColumn {
                        itemsIndexed(contents) { index, entry ->
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onOpenContents(index)
                                        showContents = false
                                    }
                                    .padding(
                                        start = (entry.depth * 16).dp,
                                        top = 10.dp,
                                        bottom = 10.dp
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContents = false }) { Text("Close") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures { } },
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var dragValue by remember { mutableStateOf<Float?>(null) }

                    Text(
                        text = "${((dragValue ?: progress) * 100).roundToInt()}% through the book",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = dragValue ?: progress,
                        onValueChange = { dragValue = it },
                        onValueChangeFinished = {
                            dragValue?.let(onSeek)
                            dragValue = null
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageTheme.entries.forEach { option ->
                            FilterChip(
                                selected = theme == option,
                                onClick = { onTheme(option) },
                                label = { Text(option.label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Text size", modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            onFontSize(((fontSize - 0.1) * 10).roundToInt() / 10.0)
                        }) {
                            Text("A−")
                        }
                        Text("${(fontSize * 100).roundToInt()}%")
                        TextButton(onClick = {
                            onFontSize(((fontSize + 0.1) * 10).roundToInt() / 10.0)
                        }) {
                            Text("A+")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAddBookmark,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add bookmark", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showBookmarks = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bookmarks (${bookmarks.size})", maxLines = 1)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // NEW: Highlights button, next to Contents
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showHighlights = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Highlights (${highlights.size})", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showContents = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Contents", maxLines = 1)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onGoToStart,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Go to start", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Close book", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}