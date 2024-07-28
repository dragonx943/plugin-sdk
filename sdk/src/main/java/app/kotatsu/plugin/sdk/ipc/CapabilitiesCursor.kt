package app.kotatsu.plugin.sdk.ipc

import app.kotatsu.plugin.sdk.core.MangaSourceCapabilities
import app.kotatsu.plugin.sdk.util.asInt

class CapabilitiesCursor(
    capabilities: MangaSourceCapabilities,
) : MapperCursor<MangaSourceCapabilities>(listOf(capabilities)) {

    override fun getColumnNames(): Array<String> = COLUMNS

    override fun MangaSourceCapabilities.getColumnValue(column: String): Any? = when (column) {
        AVAILABLE_SORT_ORDERS -> availableSortOrders.namesToString()
        AVAILABLE_STATES -> availableStates.namesToString()
        AVAILABLE_CONTENT_RATING -> availableContentRating.namesToString()
        IS_MULTIPLE_TAGS_SUPPORTED -> isMultipleTagsSupported.asInt()
        IS_TAGS_EXCLUSION_SUPPORTED -> isTagsExclusionSupported.asInt()
        IS_SEARCH_SUPPORTED -> isSearchSupported.asInt()
        CONTENT_TYPE -> contentType.name
        DEFAULT_SORT_ORDER -> defaultSortOrder.name
        SOURCE_LOCALE -> sourceLocale.language
        else -> throw IndexOutOfBoundsException()
    }

    private fun <E : Enum<E>> Iterable<E>.namesToString() = joinToString(",") { it.name }

    companion object {

        const val AVAILABLE_SORT_ORDERS = "availableSortOrders"
        const val AVAILABLE_STATES = "availableStates"
        const val AVAILABLE_CONTENT_RATING = "availableContentRating"
        const val IS_MULTIPLE_TAGS_SUPPORTED = "isMultipleTagsSupported"
        const val IS_TAGS_EXCLUSION_SUPPORTED = "isTagsExclusionSupported"
        const val IS_SEARCH_SUPPORTED = "isSearchSupported"
        const val CONTENT_TYPE = "contentType"
        const val DEFAULT_SORT_ORDER = "defaultSortOrder"
        const val SOURCE_LOCALE = "sourceLocale"

        val COLUMNS = arrayOf(
            AVAILABLE_SORT_ORDERS,
            AVAILABLE_STATES,
            AVAILABLE_CONTENT_RATING,
            IS_MULTIPLE_TAGS_SUPPORTED,
            IS_TAGS_EXCLUSION_SUPPORTED,
            IS_SEARCH_SUPPORTED,
            CONTENT_TYPE,
            DEFAULT_SORT_ORDER,
            SOURCE_LOCALE,
        )
    }
}