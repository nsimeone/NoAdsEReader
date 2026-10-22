package com.example.noadse_reader

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun deleteBook(context: Context, book: BookEntity) = withContext(Dispatchers.IO) {
    val file = File(File(context.filesDir, "books"), book.fileName)
    val gone = file.delete() || !file.exists()
    if (gone) {
        book.coverPath
            ?.takeIf { it.isNotEmpty() }
            ?.let { File(it).delete() }
        LibraryDatabase.get(context).books().delete(book.fileName)
    }
}