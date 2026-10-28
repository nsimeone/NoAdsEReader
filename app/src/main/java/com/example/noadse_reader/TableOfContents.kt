package com.example.noadse_reader

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

data class TocEntry(val title: String, val depth: Int)

class FlatToc(val entries: List<TocEntry>, val links: List<Link>)

fun flattenToc(publication: Publication): FlatToc {
    val entries = mutableListOf<TocEntry>()
    val links = mutableListOf<Link>()

    fun walk(items: List<Link>, depth: Int) {
        for (item in items) {
            entries += TocEntry(
                title = item.title?.takeIf { it.isNotBlank() } ?: "Untitled section",
                depth = depth
            )
            links += item
            walk(item.children, depth + 1)
        }
    }

    walk(publication.tableOfContents, 0)
    return FlatToc(entries, links)
}