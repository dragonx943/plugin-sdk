package app.kotatsu.plugin.sdk.core

data class Manga(
    val id: Long,
    val title: String,
    val altTitle: String?,
    val url: String,
    val publicUrl: String,
    val rating: Float,
    val isNsfw: Boolean,
    val coverUrl: String,
    val tags: Set<MangaTag>,
    val state: MangaState?,
    val author: String?,
    val largeCoverUrl: String? = null,
    val description: String? = null,
)
