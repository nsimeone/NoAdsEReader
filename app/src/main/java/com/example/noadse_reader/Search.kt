package com.example.noadse_reader

import kotlin.math.roundToInt
import org.readium.r2.shared.publication.Locator

// One search result: where it is, plus a little text before and after the match
// so you can recognize it in the list.
data class SearchHit(
    val locator: Locator,
    val before: String,
    val match: String,
    val after: String,
    val where: String
)

// Everything the Search dialog needs to know about the current search.
data class SearchUiState(
    val query: String = "",
    val hits: List<SearchHit> = emptyList(),
    val searching: Boolean = false,
    val searched: Boolean = false,
    val hasMore: Boolean = false,
    val unavailable: Boolean = false,
    val wholeWord: Boolean = true
)

private val whitespace = Regex("\\s+")

// Squashes line breaks and runs of spaces into single spaces, so snippets read cleanly.
private fun String?.tidy(): String = this?.replace(whitespace, " ") ?: ""

// Turns a Readium search result (a Locator) into a SearchHit for our list.
fun Locator.toSearchHit(): SearchHit {
    val percent = locations.totalProgression?.let { "${(it * 100).roundToInt()}% through the book" }
    val where = listOfNotNull(title?.takeIf { it.isNotBlank() }, percent).joinToString(" · ")
    return SearchHit(
        locator = this,
        before = text.before.tidy().takeLast(50),
        match = text.highlight.tidy(),
        after = text.after.tidy().take(70),
        where = where
    )
}
fun Locator.isWholeWord(): Boolean {
    val charBefore = text.before?.lastOrNull()
    val charAfter = text.after?.firstOrNull()
    return (charBefore == null || !charBefore.isLetterOrDigit()) &&
            (charAfter == null || !charAfter.isLetterOrDigit())
}