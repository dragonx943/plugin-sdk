package app.kotatsu.plugin.sdk

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.annotation.CallSuper
import app.kotatsu.plugin.sdk.core.ContentRating
import app.kotatsu.plugin.sdk.core.ContentType
import app.kotatsu.plugin.sdk.core.Demographic
import app.kotatsu.plugin.sdk.core.MangaListFilter
import app.kotatsu.plugin.sdk.core.MangaParser
import app.kotatsu.plugin.sdk.core.MangaState
import app.kotatsu.plugin.sdk.core.MangaTag
import app.kotatsu.plugin.sdk.core.YEAR_UNKNOWN
import app.kotatsu.plugin.sdk.ipc.CapabilitiesCursor
import app.kotatsu.plugin.sdk.ipc.ChapterCursor
import app.kotatsu.plugin.sdk.ipc.EnumCursor
import app.kotatsu.plugin.sdk.ipc.LocaleCursor
import app.kotatsu.plugin.sdk.ipc.MangaCursor
import app.kotatsu.plugin.sdk.ipc.PageCursor
import app.kotatsu.plugin.sdk.ipc.StringCursor
import app.kotatsu.plugin.sdk.ipc.TagCursor
import app.kotatsu.plugin.sdk.util.find
import app.kotatsu.plugin.sdk.util.mapNotNullToSet
import app.kotatsu.plugin.sdk.util.splitTwoParts
import java.util.Locale

public abstract class KotatsuParserContentProvider<P : MangaParser>(
    protected val authority: String,
) : ContentProvider() {

    protected lateinit var parser: P
        private set

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    @CallSuper
    override fun onCreate(): Boolean {
        parser = onCreateParser()
        uriMatcher.addURI(authority, "manga", URI_MANGA_LIST)
        uriMatcher.addURI(authority, "filter/tags", URI_TAGS)
        uriMatcher.addURI(authority, "manga/chapters/*", URI_CHAPTERS)
        uriMatcher.addURI(authority, "manga/pages/*", URI_PAGE_URL)
        uriMatcher.addURI(authority, "manga/*", URI_MANGA_DETAILS)
        uriMatcher.addURI(authority, "chapters/*", URI_PAGES)
        uriMatcher.addURI(authority, "capabilities", URI_CAPABILITIES)
        uriMatcher.addURI(authority, "filter/states", URI_STATES)
        uriMatcher.addURI(authority, "filter/content_ratings", URI_CONTENT_RATINGS)
        uriMatcher.addURI(authority, "filter/content_types", URI_CONTENT_TYPES)
        uriMatcher.addURI(authority, "filter/demographics", URI_DEMOGRAPHICS)
        uriMatcher.addURI(authority, "filter/locales", URI_LOCALES)
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
        URI_MANGA_LIST -> MangaCursor(
            dataset = parser.getList(
                offset = uri.getQueryParameter("offset")?.toIntOrNull() ?: 0,
                order = parser.filterCapabilities.availableSortOrders.find(sortOrder)
                    ?: parser.filterCapabilities.availableSortOrders.first(),
                filter = getFilter(uri)
            )
        )

        URI_TAGS -> TagCursor(
            dataset = parser.filterOptionsLazy.availableTags.toList()
        )

        URI_MANGA_DETAILS -> MangaCursor(
            dataset = listOf(parser.getDetails(requireNotNull(uri.lastPathSegment)))
        )

        URI_CHAPTERS -> ChapterCursor(
            dataset = parser.getChapters(requireNotNull(uri.lastPathSegment))
        )

        URI_PAGES -> PageCursor(
            dataset = parser.getPages(requireNotNull(uri.lastPathSegment))
        )

        URI_CAPABILITIES -> CapabilitiesCursor(
            capabilities = parser.filterCapabilities
        )

        URI_STATES -> EnumCursor(parser.filterOptionsLazy.availableStates.toList())
        URI_CONTENT_RATINGS -> EnumCursor(parser.filterOptionsLazy.availableContentRating.toList())
        URI_CONTENT_TYPES -> EnumCursor(parser.filterOptionsLazy.availableContentTypes.toList())
        URI_DEMOGRAPHICS -> EnumCursor(parser.filterOptionsLazy.availableDemographics.toList())
        URI_LOCALES -> LocaleCursor(parser.filterOptionsLazy.availableLocales.toList())

        URI_PAGE_URL -> uri.getQueryParameter("url")?.let { pageUrl ->
            StringCursor(listOf(parser.getPageUrl(pageUrl)))
        }

        else -> null
    }

    protected abstract fun onCreateParser(): P

    private fun getFilter(uri: Uri): MangaListFilter = MangaListFilter(
        query = uri.getQueryParameter("query"),
        tags = uri.getQueryParameters("tags_include").mapNotNullToSet {
            val parts = it.splitTwoParts('=') ?: return@mapNotNullToSet null
            MangaTag(key = parts.first, title = parts.second)
        },
        tagsExclude = uri.getQueryParameters("tags_exclude").mapNotNullToSet {
            val parts = it.splitTwoParts('=') ?: return@mapNotNullToSet null
            MangaTag(key = parts.first, title = parts.second)
        },
        locale = uri.getQueryParameter("locale")?.let { Locale(it) },
        originalLocale = uri.getQueryParameter("locale_original")?.let { Locale(it) },
        states = uri.getQueryParameters("state").mapNotNullToSet {
            MangaState.entries.find(it)
        },
        contentRating = uri.getQueryParameters("content_rating").mapNotNullToSet {
            ContentRating.entries.find(it)
        },
        types = uri.getQueryParameters("content_type").mapNotNullToSet {
            ContentType.entries.find(it)
        },
        demographics = uri.getQueryParameters("demographic").mapNotNullToSet {
            Demographic.entries.find(it)
        },
        year = uri.getQueryParameter("year")?.toIntOrNull() ?: YEAR_UNKNOWN,
        yearFrom = uri.getQueryParameter("year_from")?.toIntOrNull() ?: YEAR_UNKNOWN,
        yearTo = uri.getQueryParameter("year_to")?.toIntOrNull() ?: YEAR_UNKNOWN
    )

    private companion object {

        const val URI_MANGA_LIST = 1
        const val URI_TAGS = 2
        const val URI_MANGA_DETAILS = 3
        const val URI_CHAPTERS = 4
        const val URI_PAGES = 5
        const val URI_CAPABILITIES = 6
        const val URI_STATES = 7
        const val URI_CONTENT_RATINGS = 8
        const val URI_CONTENT_TYPES = 9
        const val URI_DEMOGRAPHICS = 10
        const val URI_LOCALES = 11
        const val URI_PAGE_URL = 12
    }
}