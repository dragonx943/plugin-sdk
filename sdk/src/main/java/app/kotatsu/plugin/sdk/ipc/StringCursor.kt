package app.kotatsu.plugin.sdk.ipc

internal class StringCursor(
    dataset: List<String>,
) : MapperCursor<String>(dataset) {

    override fun getColumnNames(): Array<String> = COLUMNS

    override fun String.getColumnValue(column: String) = when (column) {
        COLUMN_VALUE -> this
        else -> throw IndexOutOfBoundsException()
    }

    companion object {

        const val COLUMN_VALUE = "value"

        val COLUMNS = arrayOf(
            COLUMN_VALUE,
        )
    }
}