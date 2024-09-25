package app.kotatsu.plugin.sdk.ipc

import app.kotatsu.plugin.sdk.core.Manga
import app.kotatsu.plugin.sdk.util.asInt

internal class MangaCursor(
    dataset: List<Manga>,
) : MapperCursor<Manga>(dataset) {

    override fun getColumnNames(): Array<String> = COLUMNS

    override fun Manga.getColumnValue(column: String) = when (column) {
        COLUMN_ID -> id
        COLUMN_TITLE -> title
        COLUMN_ALT_TITLE -> altTitle
        COLUMN_URL -> url
        COLUMN_PUBLIC_URL -> publicUrl
        COLUMN_RATING -> rating
        COLUMN_IS_NSFW -> isNsfw.asInt()
        COLUMN_COVER_URL -> coverUrl
        COLUMN_TAGS -> tags.joinToString(":") { it.key + "=" + it.title }
        COLUMN_STATE -> state?.name
        COLUMN_AUTHOR -> author
        COLUMN_LARGE_COVER_URL -> largeCoverUrl
        COLUMN_DESCRIPTION -> description
        else -> throw IndexOutOfBoundsException()
    }

    companion object {

        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_ALT_TITLE = "alt_title"
        const val COLUMN_URL = "url"
        const val COLUMN_PUBLIC_URL = "public_url"
        const val COLUMN_RATING = "rating"
        const val COLUMN_IS_NSFW = "is_nsfw"
        const val COLUMN_COVER_URL = "cover_url"
        const val COLUMN_TAGS = "tags"
        const val COLUMN_STATE = "state"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_LARGE_COVER_URL = "large_cover_url"
        const val COLUMN_DESCRIPTION = "description"

        val COLUMNS = arrayOf(
            COLUMN_ID,
            COLUMN_TITLE,
            COLUMN_ALT_TITLE,
            COLUMN_URL,
            COLUMN_PUBLIC_URL,
            COLUMN_RATING,
            COLUMN_IS_NSFW,
            COLUMN_COVER_URL,
            COLUMN_TAGS,
            COLUMN_STATE,
            COLUMN_AUTHOR,
            COLUMN_LARGE_COVER_URL,
            COLUMN_DESCRIPTION,
        )
    }
}