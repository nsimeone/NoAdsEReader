package com.example.noadse_reader

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private suspend fun finishImport(context: Context, result: ImportResult): String {
    val indexed = syncLibrary(context)
    extractMissingCovers(context)
    return "Copied ${result.imported} new book(s); added $indexed to the library.\n" +
            "Already in library: ${result.skipped}.\n" +
            "PDF/MOBI/AZW3 found (not supported; convert to EPUB with Calibre): ${result.otherFiles}."
}

@Composable
fun ImportScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember {
        mutableStateOf("Add a folder or pick files to fill your library.")
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                message = "Importing... this can take a moment."
                message = finishImport(context, importFolder(context, uri))
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                message = "Importing... this can take a moment."
                message = finishImport(context, importFiles(context, uris))
            }
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Books are copied into this app. Your originals stay where they are.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { folderPicker.launch(null) }) {
            Text("Add a folder")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { filePicker.launch(arrayOf("*/*")) }) {
            Text("Add files")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}