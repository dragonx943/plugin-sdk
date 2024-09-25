package app.kotatsu.plugin.sdk.ipc

import java.util.Locale

internal class LocaleCursor(
    dataset: List<Locale>,
) : MapperCursor<Locale>(dataset) {

    override fun getColumnNames(): Array<String> = COLUMNS

    override fun Locale.getColumnValue(column: String) = when (column) {
        COLUMN_NAME -> toString()
        else -> throw IndexOutOfBoundsException()
    }

    companion object {

        const val COLUMN_NAME = "name"

        val COLUMNS = arrayOf(
            COLUMN_NAME,
        )
    }
}