package app.kotatsu.plugin.sdk.core

import java.util.Locale

data class MangaSourceCapabilities(
    val availableSortOrders: Set<SortOrder>,
    val availableStates: Set<MangaState>,
    val availableContentRating: Set<ContentRating>,
    val isMultipleTagsSupported: Boolean,
    val isTagsExclusionSupported: Boolean,
    val isSearchSupported: Boolean,
    val contentType: ContentType,
    val defaultSortOrder: SortOrder,
    val sourceLocale: Locale,
)