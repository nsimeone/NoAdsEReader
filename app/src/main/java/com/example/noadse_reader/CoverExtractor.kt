package com.example.noadse_reader

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.services.cover

private const val MAX_COVER_HEIGHT = 600

suspend fun extractMissingCovers(context: Context): Int = withContext(Dispatchers.IO) {
    val dao = LibraryDatabase.get(context).books()
    val pending = dao.booksNeedingCovers()
    if (pending.isEmpty()) return@withContext 0

    val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
    val booksDir = File(context.filesDir, "books")

    for (book in pending) {
        var savedPath = ""
        val publication = openPublication(context, File(booksDir, book.fileName))
        if (publication != null) {
            val bitmap = publication.cover()
            if (bitmap != null) {
                val scaled = if (bitmap.height > MAX_COVER_HEIGHT) {
                    val width = (bitmap.width * MAX_COVER_HEIGHT.toFloat() / bitmap.height).toInt()
                    Bitmap.createScaledBitmap(bitmap, width, MAX_COVER_HEIGHT, true)
                } else {
                    bitmap
                }
                val out = File(coversDir, book.fileName + ".jpg")
                out.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                savedPath = out.absolutePath
            }
            publication.close()
        }
        dao.setCover(book.fileName, savedPath)
    }
    pending.size
}