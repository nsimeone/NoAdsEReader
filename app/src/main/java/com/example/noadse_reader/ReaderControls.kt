package com.example.noadse_reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    onFontSize: (Double) -> Unit,
    onTheme: (PageTheme) -> Unit,
    onSeek: (Float) -> Unit,
    onGoToStart: () -> Unit,
    onAddBookmark: () -> Unit,
    onOpenBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onClose: () -> Unit
) {
    var showBookmarks by remember { mutableStateOf(false) }

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
                        Button(onClick = onAddBookmark) { Text("Add bookmark") }
                        OutlinedButton(onClick = { showBookmarks = true }) {
                            Text("Bookmarks (${bookmarks.size})")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onGoToStart) { Text("Go to start") }
                        OutlinedButton(onClick = onClose) { Text("Close book") }
                    }
                }
            }
        }
    }
}