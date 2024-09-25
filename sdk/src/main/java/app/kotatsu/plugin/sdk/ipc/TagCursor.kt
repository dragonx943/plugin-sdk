package app.kotatsu.plugin.sdk.ipc

import app.kotatsu.plugin.sdk.core.MangaTag

internal class TagCursor(
    dataset: List<MangaTag>,
) : MapperCursor<MangaTag>(dataset) {

    override fun getColumnNames(): Array<String> = COLUMNS

    override fun MangaTag.getColumnValue(column: String) = when (column) {
        COLUMN_KEY -> key
        COLUMN_TITLE -> title
        else -> throw IndexOutOfBoundsException()
    }

    companion object {

        const val COLUMN_KEY = "key"
        const val COLUMN_TITLE = "title"

        val COLUMNS = arrayOf(
            COLUMN_KEY,
            COLUMN_TITLE,
        )
    }
}