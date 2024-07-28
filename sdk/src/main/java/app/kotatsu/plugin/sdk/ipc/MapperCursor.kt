package app.kotatsu.plugin.sdk.ipc

import android.database.AbstractCursor

abstract class MapperCursor<T>(
    protected val dataset: List<T>,
) : AbstractCursor() {

    private var index = 0

    override fun getCount(): Int = dataset.size

    override fun getString(column: Int): String = getColumnValue(column).toString()

    override fun getShort(column: Int): Short = (getColumnValue(column) as Number).toShort()

    override fun getInt(column: Int): Int = (getColumnValue(column) as Number).toInt()

    override fun getLong(column: Int): Long = (getColumnValue(column) as Number).toLong()

    override fun getFloat(column: Int): Float = (getColumnValue(column) as Number).toFloat()

    override fun getDouble(column: Int): Double = (getColumnValue(column) as Number).toDouble()

    override fun isNull(column: Int): Boolean = getColumnValue(column) == null

    override fun getType(column: Int): Int = when (getColumnValue(column)) {
        null -> FIELD_TYPE_NULL
        is String -> FIELD_TYPE_STRING
        is Double,
        is Float,
        -> FIELD_TYPE_FLOAT

        is Long,
        is Int,
        is Byte,
        is Short,
        -> FIELD_TYPE_INTEGER


        else -> FIELD_TYPE_BLOB
    }

    override fun onMove(oldPosition: Int, newPosition: Int): Boolean {
        index = newPosition
        return true
    }

    private fun getColumnValue(column: Int): Any? =
        dataset[index].getColumnValue(getColumnName(column))

    protected abstract fun T.getColumnValue(column: String): Any?
}