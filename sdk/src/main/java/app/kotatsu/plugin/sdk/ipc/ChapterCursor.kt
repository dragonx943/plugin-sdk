package app.kotatsu.plugin.sdk.ipc

import app.kotatsu.plugin.sdk.core.MangaChapter

class ChapterCursor(
    dataset: List<MangaChapter>,
) : MapperCursor<MangaChapter>(dataset) {

    override fun getColumnNames(): Array<String> = COLUMNS

    override fun MangaChapter.getColumnValue(column: String) = when (column) {
        COLUMN_ID -> id
        COLUMN_NAME -> name
        COLUMN_NUMBER -> number
        COLUMN_VOLUME -> volume
        COLUMN_URL -> url
        COLUMN_SCANLATOR -> scanlator
        COLUMN_UPLOAD_DATE -> uploadDate
        COLUMN_BRANCH -> branch
        else -> throw IndexOutOfBoundsException()
    }

    companion object {

        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_NUMBER = "number"
        const val COLUMN_VOLUME = "volume"
        const val COLUMN_URL = "url"
        const val COLUMN_SCANLATOR = "scanlator"
        const val COLUMN_UPLOAD_DATE = "upload_date"
        const val COLUMN_BRANCH = "branch"

        val COLUMNS = arrayOf(
            COLUMN_ID,
            COLUMN_NAME,
            COLUMN_NUMBER,
            COLUMN_VOLUME,
            COLUMN_URL,
            COLUMN_SCANLATOR,
            COLUMN_UPLOAD_DATE,
            COLUMN_BRANCH,
        )
    }
}