package app.kotatsu.plugin.sdk.ipc

internal class EnumCursor<E : Enum<E>>(
    dataset: List<E>,
) : MapperCursor<E>(dataset) {

    override fun getColumnNames(): Array<String> = COLUMNS

    override fun E.getColumnValue(column: String) = when (column) {
        COLUMN_NAME -> name
        else -> throw IndexOutOfBoundsException()
    }

    companion object {

        const val COLUMN_NAME = "name"

        val COLUMNS = arrayOf(
            COLUMN_NAME,
        )
    }
}