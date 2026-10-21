package com.example.noadse_reader

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
val BOOK_EXTENSIONS = setOf("epub")

data class ImportResult(val imported: Int, val skipped: Int, val otherFiles: Int)

private class Counter {
    var imported = 0
    var skipped = 0
    var other = 0

    fun toResult() = ImportResult(imported, skipped, other)
}

private fun handleFile(
    context: Context,
    uri: Uri,
    name: String,
    booksDir: File,
    counter: Counter
) {
    val extension = name.substringAfterLast('.', "").lowercase()
    when {
        extension in BOOK_EXTENSIONS -> {
            val target = File(booksDir, name)
            if (target.exists()) {
                counter.skipped++
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                counter.imported++
            }
        }
        extension == "pdf" || extension == "mobi" || extension == "azw3" -> counter.other++
    }
}

suspend fun importFolder(context: Context, treeUri: Uri): ImportResult =
    withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext ImportResult(0, 0, 0)
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val counter = Counter()

        fun walk(folder: DocumentFile) {
            for (item in folder.listFiles()) {
                if (item.isDirectory) {
                    walk(item)
                } else {
                    val name = item.name ?: continue
                    handleFile(context, item.uri, name, booksDir, counter)
                }
            }
        }

        walk(root)
        counter.toResult()
    }

suspend fun importFiles(context: Context, uris: List<Uri>): ImportResult =
    withContext(Dispatchers.IO) {
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val counter = Counter()
        for (uri in uris) {
            val name = DocumentFile.fromSingleUri(context, uri)?.name ?: continue
            handleFile(context, uri, name, booksDir, counter)
        }
        counter.toResult()
    }