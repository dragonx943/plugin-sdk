package app.kotatsu.plugin.sdk.ipc

import app.kotatsu.plugin.sdk.core.MangaSourceCapabilities
import app.kotatsu.plugin.sdk.util.asInt

class CapabilitiesCursor(
    capabilities: MangaSourceCapabilities,
) : MapperCursor<MangaSourceCapabilities>(listOf(capabilities)) {

    override fun getColumnNames(): Array<String> = COLUMNS

    override fun MangaSourceCapabilities.getColumnValue(column: String): Any? = when (column) {
        COLUMN_SORT_ORDERS -> availableSortOrders.namesToString()
        COLUMN_STATES -> availableStates.namesToString()
        COLUMN_CONTENT_RATING -> availableContentRating.namesToString()
        COLUMN_MULTIPLE_TAGS_SUPPORTED -> isMultipleTagsSupported.asInt()
        COLUMN_TAGS_EXCLUSION_SUPPORTED -> isTagsExclusionSupported.asInt()
        COLUMN_SEARCH_SUPPORTED -> isSearchSupported.asInt()
        COLUMN_CONTENT_TYPE -> contentType.name
        COLUMN_DEFAULT_SORT_ORDER -> defaultSortOrder.name
        COLUMN_LOCALE -> sourceLocale.language
        else -> throw IndexOutOfBoundsException()
    }

    private fun <E : Enum<E>> Iterable<E>.namesToString() = joinToString(",") { it.name }

    companion object {

        const val COLUMN_SORT_ORDERS = "sort_orders"
        const val COLUMN_STATES = "states"
        const val COLUMN_CONTENT_RATING = "content_rating"
        const val COLUMN_MULTIPLE_TAGS_SUPPORTED = "multiple_tags_supported"
        const val COLUMN_TAGS_EXCLUSION_SUPPORTED = "tags_exclusion_supported"
        const val COLUMN_SEARCH_SUPPORTED = "search_supported"
        const val COLUMN_CONTENT_TYPE = "content_type"
        const val COLUMN_DEFAULT_SORT_ORDER = "default_sort_order"
        const val COLUMN_LOCALE = "locale"

        val COLUMNS = arrayOf(
            COLUMN_SORT_ORDERS,
            COLUMN_STATES,
            COLUMN_CONTENT_RATING,
            COLUMN_MULTIPLE_TAGS_SUPPORTED,
            COLUMN_TAGS_EXCLUSION_SUPPORTED,
            COLUMN_SEARCH_SUPPORTED,
            COLUMN_CONTENT_TYPE,
            COLUMN_DEFAULT_SORT_ORDER,
            COLUMN_LOCALE,
        )
    }
}