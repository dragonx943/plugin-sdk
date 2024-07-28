package app.kotatsu.plugin.sdk.ipc

import app.kotatsu.plugin.sdk.core.MangaPage

class PageCursor(
    dataset: List<MangaPage>,
) : MapperCursor<MangaPage>(dataset) {

    override fun getColumnNames(): Array<String> = COLUMNS

    override fun MangaPage.getColumnValue(column: String) = when (column) {
        COLUMN_ID -> id
        COLUMN_URL -> url
        COLUMN_PREVIEW -> preview
        else -> throw IndexOutOfBoundsException()
    }

    companion object {

        const val COLUMN_ID = "id"
        const val COLUMN_URL = "url"
        const val COLUMN_PREVIEW = "preview"

        val COLUMNS = arrayOf(
            COLUMN_ID,
            COLUMN_URL,
            COLUMN_PREVIEW,
        )
    }
}