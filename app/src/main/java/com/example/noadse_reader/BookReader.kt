package com.example.noadse_reader

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

private class ReadiumTools(context: Context) {
    private val httpClient = DefaultHttpClient()
    val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    val opener = PublicationOpener(
        DefaultPublicationParser(
            context,
            httpClient,
            assetRetriever,
            null
        )
    )
}

private suspend fun openWith(tools: ReadiumTools, file: File): Publication? {
    val url = Uri.fromFile(file).toAbsoluteUrl() ?: return null
    val asset = tools.assetRetriever.retrieve(url).getOrElse { return null }
    return tools.opener.open(asset, allowUserInteraction = false).getOrElse { return null }
}

suspend fun openPublication(context: Context, file: File): Publication? =
    openWith(ReadiumTools(context), file)

suspend fun syncLibrary(context: Context): Int = withContext(Dispatchers.IO) {
    val dao = LibraryDatabase.get(context).books()
    val known = dao.allFileNames().toSet()
    val booksDir = File(context.filesDir, "books")
    val newFiles = booksDir
        .listFiles { f -> f.extension.lowercase() in BOOK_EXTENSIONS && f.name !in known }
        ?.toList()
        ?: emptyList()
    if (newFiles.isEmpty()) return@withContext 0

    val tools = ReadiumTools(context)

    val entities = newFiles.map { file ->
        val fallback = BookEntity(
            fileName = file.name,
            title = file.nameWithoutExtension,
            author = "",
            sizeBytes = file.length(),
            addedAt = file.lastModified(),
            format = file.extension.lowercase()
        )
        val publication = openWith(tools, file) ?: return@map fallback
        val entity = fallback.copy(
            title = publication.metadata.title
                ?.takeIf { it.isNotBlank() }
                ?: file.nameWithoutExtension,
            author = publication.metadata.authors.joinToString { it.name }
        )
        publication.close()
        entity
    }

    entities.forEach { dao.insert(it) }
    entities.size
}