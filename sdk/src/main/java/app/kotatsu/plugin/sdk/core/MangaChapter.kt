package app.kotatsu.plugin.sdk.core

data class MangaChapter(
    val id: Long,
    val name: String,
    val number: Float,
    val volume: Int,
    val url: String,
    val scanlator: String?,
    val uploadDate: Long,
    val branch: String?,
)
