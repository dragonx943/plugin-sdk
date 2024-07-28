package app.kotatsu.plugin.sdk

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.annotation.CallSuper
import app.kotatsu.plugin.sdk.core.ContentRating
import app.kotatsu.plugin.sdk.core.MangaListFilter
import app.kotatsu.plugin.sdk.core.MangaParser
import app.kotatsu.plugin.sdk.core.MangaState
import app.kotatsu.plugin.sdk.core.MangaTag
import app.kotatsu.plugin.sdk.core.SortOrder
import app.kotatsu.plugin.sdk.util.find
import app.kotatsu.plugin.sdk.util.mapNotNullToSet
import app.kotatsu.plugin.sdk.util.splitTwoParts
import java.util.Locale

abstract class KotatsuParserContentProvider<P : MangaParser>(
    protected val authority: String,
) : ContentProvider() {

    protected lateinit var parser: P
        private set

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    @CallSuper
    override fun onCreate(): Boolean {
        parser = onCreateParser()
        uriMatcher.addURI(authority, "manga", URI_MANGA_LIST)
        uriMatcher.addURI(authority, "tags", URI_TAGS)
        uriMatcher.addURI(authority, "manga/chapters/*", URI_CHAPTERS)
        uriMatcher.addURI(authority, "manga/*", URI_MANGA_DETAILS)
        uriMatcher.addURI(authority, "chapters/*", URI_PAGES)
        uriMatcher.addURI(authority, "capabilities", URI_CAPABILITIES)
        return true
    }

    final override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    final override fun getType(uri: Uri): String? = null

    final override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    final override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = when (uriMatcher.match(uri)) {
        URI_MANGA_LIST -> parser.getList(
            offset = uri.getQueryParameter("offset")?.toIntOrNull() ?: 0,
            filter = getFilter(uri, sortOrder)
        )

        URI_TAGS -> parser.getAvailableTags()
        URI_MANGA_DETAILS -> parser.getDetails(requireNotNull(uri.lastPathSegment))
        URI_CHAPTERS -> parser.getChapters(requireNotNull(uri.lastPathSegment))
        URI_PAGES -> parser.getPages(requireNotNull(uri.lastPathSegment))
        URI_CAPABILITIES -> parser.getCapabilities()
        else -> null
    }

    protected abstract fun onCreateParser(): P

    private fun getFilter(
        uri: Uri,
        orderBy: String?,
    ): MangaListFilter? {
        val query = uri.getQueryParameter("query")
        if (!query.isNullOrEmpty()) {
            return MangaListFilter.Search(query)
        }
        val tagsInclude = uri.getQueryParameters("tags_include").mapNotNullToSet {
            val parts = it.splitTwoParts('=') ?: return@mapNotNullToSet null
            MangaTag(key = parts.first, title = parts.second)
        }
        val tagsExclude = uri.getQueryParameters("tags_exclude").mapNotNullToSet {
            val parts = it.splitTwoParts('=') ?: return@mapNotNullToSet null
            MangaTag(key = parts.first, title = parts.second)
        }
        val locale = uri.getQueryParameter("locale")?.let { Locale(it) }
        val states = uri.getQueryParameters("state").mapNotNullToSet {
            MangaState.entries.find(it)
        }
        val contentRating = uri.getQueryParameters("content_rating").mapNotNullToSet {
            ContentRating.entries.find(it)
        }

        return if (tagsInclude.isNotEmpty() || tagsExclude.isNotEmpty() || locale != null || orderBy != null) {
            MangaListFilter.Advanced(
                sortOrder = SortOrder.entries.find(orderBy) ?: parser.defaultSortOrder,
                tags = tagsInclude,
                tagsExclude = tagsExclude,
                locale = locale,
                states = states,
                contentRating = contentRating,
            )
        } else {
            null
        }
    }

    private companion object {

        const val URI_MANGA_LIST = 1
        const val URI_TAGS = 2
        const val URI_MANGA_DETAILS = 3
        const val URI_CHAPTERS = 4
        const val URI_PAGES = 5
        const val URI_CAPABILITIES = 6
    }
}