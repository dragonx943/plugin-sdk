package app.kotatsu.plugin.sdk.ipc

import app.kotatsu.plugin.sdk.core.MangaListFilterCapabilities
import app.kotatsu.plugin.sdk.util.asInt
import app.kotatsu.plugin.sdk.util.namesToString

internal class CapabilitiesCursor(
    capabilities: MangaListFilterCapabilities,
) : MapperCursor<MangaListFilterCapabilities>(listOf(capabilities)) {

    override fun getColumnNames(): Array<String> = COLUMNS

    override fun MangaListFilterCapabilities.getColumnValue(column: String): Any = when (column) {
        COLUMN_SORT_ORDERS -> availableSortOrders.namesToString()
        COLUMN_MULTIPLE_TAGS -> isMultipleTagsSupported.asInt()
        COLUMN_TAGS_EXCLUSION -> isTagsExclusionSupported.asInt()
        COLUMN_SEARCH -> isSearchSupported.asInt()
        COLUMN_SEARCH_WITH_FILTERS -> isSearchWithFiltersSupported.asInt()
        COLUMN_YEAR -> isYearSupported.asInt()
        COLUMN_YEAR_RANGE -> isYearRangeSupported.asInt()
        COLUMN_ORIGINAL_LOCALE -> isOriginalLocaleSupported.asInt()
        else -> throw IndexOutOfBoundsException()
    }

    companion object {

        const val COLUMN_SORT_ORDERS = "sort_orders"
        const val COLUMN_MULTIPLE_TAGS = "multiple_tags"
        const val COLUMN_TAGS_EXCLUSION = "tags_exclusion"
        const val COLUMN_SEARCH = "search"
        const val COLUMN_SEARCH_WITH_FILTERS = "search_with_filters"
        const val COLUMN_YEAR = "year"
        const val COLUMN_YEAR_RANGE = "year_range"
        const val COLUMN_ORIGINAL_LOCALE = "original_locale"

        val COLUMNS = arrayOf(
            COLUMN_SORT_ORDERS,
            COLUMN_MULTIPLE_TAGS,
            COLUMN_TAGS_EXCLUSION,
            COLUMN_SEARCH,
            COLUMN_SEARCH_WITH_FILTERS,
            COLUMN_YEAR,
            COLUMN_YEAR_RANGE,
            COLUMN_ORIGINAL_LOCALE,
        )
    }
}