package app.kotatsu.parser.mimihentai

import app.kotatsu.plugin.sdk.core.Manga
import app.kotatsu.plugin.sdk.core.MangaChapter
import app.kotatsu.plugin.sdk.core.MangaListFilter
import app.kotatsu.plugin.sdk.core.MangaListFilterCapabilities
import app.kotatsu.plugin.sdk.core.MangaListFilterOptions
import app.kotatsu.plugin.sdk.core.MangaPage
import app.kotatsu.plugin.sdk.core.MangaParser
import app.kotatsu.plugin.sdk.core.MangaTag
import app.kotatsu.plugin.sdk.core.SortOrder
import app.kotatsu.plugin.sdk.network.Paginator
import app.kotatsu.plugin.sdk.util.generateUid
import app.kotatsu.plugin.sdk.util.json.asTypedList
import app.kotatsu.plugin.sdk.util.json.getStringOrNull
import app.kotatsu.plugin.sdk.util.json.mapJSON
import app.kotatsu.plugin.sdk.util.json.mapJSONToSet
import app.kotatsu.plugin.sdk.util.parseJson
import app.kotatsu.plugin.sdk.util.parseJsonArray
import app.kotatsu.plugin.sdk.util.toAbsoluteUrl
import app.kotatsu.plugin.sdk.util.toTitleCase
import app.kotatsu.plugin.sdk.util.urlEncoded
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.EnumSet
import java.util.Locale

class MimiHentaiParser(
    authority: String,
) : MangaParser(authority) {

    private val paginator = Paginator(initialPageSize = 18)
    private val sourceLocale = Locale("vi_VN") // Deprecated in Java

    override val filterCapabilities = MangaListFilterCapabilities(
        availableSortOrders = EnumSet.of(
            SortOrder.UPDATED,
            SortOrder.ALPHABETICAL,
            SortOrder.POPULARITY,
            SortOrder.POPULARITY_TODAY,
            SortOrder.POPULARITY_WEEK,
            SortOrder.POPULARITY_MONTH,
            SortOrder.RATING,
        ),
        isMultipleTagsSupported = true,
        isSearchSupported = true,
        isSearchWithFiltersSupported = true,
        isTagsExclusionSupported = true,
    )

    override val domain = "mimihentai.com"
    private val apiSuffix = "api/v1/manga"

    init {
        paginator.firstPage = 0
    }

    override fun getFilterOptions() = MangaListFilterOptions(
        availableTags = fetchTags(),
    )

    override fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = buildString {
            append("https://")
            append("$domain/$apiSuffix")

            if (!filter.query.isNullOrEmpty() || filter.tags.isNotEmpty()) {
                append("/advance-search?page=")
                append(offset / 18)
                append("&max=18")

                when {
                    !filter.query.isNullOrEmpty() -> {
                        append("&name=")
                        append(checkNotNull(filter.query).urlEncoded())
                    }

                    filter.tags.isNotEmpty() -> {
                        append("&genre=")
                        append(filter.tags.joinToString(",") { it.key })
                    }

                    filter.tagsExclude.isNotEmpty() -> {
                        append("&ex=")
                        append(filter.tagsExclude.joinToString(",") { it.key })
                    }
                }

                append("&sort=")
                append(
                    when (order) {
                        SortOrder.UPDATED -> "updated_at"
                        SortOrder.ALPHABETICAL -> "title"
                        SortOrder.POPULARITY -> "follows"
                        SortOrder.POPULARITY_TODAY,
                        SortOrder.POPULARITY_WEEK,
                        SortOrder.POPULARITY_MONTH -> "views"
                        SortOrder.RATING -> "likes"
                        else -> ""
                    }
                )
            } else {
                append(
                    when (order) {
                        SortOrder.UPDATED -> "/tatcatruyen?page=${offset / 18}&sort=updated_at"
                        SortOrder.ALPHABETICAL -> "/tatcatruyen?page=${offset / 18}&sort=title"
                        SortOrder.POPULARITY -> "/tatcatruyen?page=${offset / 18}&sort=follows"
                        SortOrder.POPULARITY_TODAY -> "/tatcatruyen?page=${offset / 18}&sort=views"
                        SortOrder.POPULARITY_WEEK -> "/top-manga?page=${offset / 18}&timeType=1&limit=18"
                        SortOrder.POPULARITY_MONTH -> "/top-manga?page=${offset / 18}&timeType=2&limit=18"
                        SortOrder.RATING -> "/tatcatruyen?page=${offset / 18}&sort=likes"
                        else -> "/tatcatruyen?page=${offset / 18}&sort=updated_at" // default
                    }
                )

                if (filter.tagsExclude.isNotEmpty()) {
                    append("&ex=")
                    append(filter.tagsExclude.joinToString(",") { it.key })
                }
            }
        }

        val raw = webClient.httpGet(url.toHttpUrl())
        return if (url.contains("/top-manga")) {
            val data = raw.parseJsonArray()
            parseTopMangaList(data)
        } else {
            val data = raw.parseJson().getJSONArray("data")
            parseMangaList(data)
        }
    }

    private fun parseTopMangaList(data: JSONArray): List<Manga> {
        return data.mapJSON { jo ->
            val id = jo.getLong("id")
            val title = jo.getString("title").takeIf { it.isNotEmpty() } ?: "Web chưa đặt tên"
            val description = jo.getStringOrNull("description")

            val differentNames = mutableSetOf<String>().apply {
                jo.optJSONArray("differentNames")?.let { namesArray ->
                    for (i in 0 until namesArray.length()) {
                        namesArray.optString(i)?.takeIf { it.isNotEmpty() }?.let { name ->
                            add(name)
                        }
                    }
                }.toString()
            }

            val authors = jo.optJSONArray("authors")?.let { authorsArray ->
                val authorNames = mutableListOf<String>()
                for (i in 0 until authorsArray.length()) {
                    authorsArray.optJSONObject(i)?.getString("name")?.takeIf { it.isNotEmpty() }
                        ?.let { name ->
                            authorNames.add(name)
                        }
                }
                authorNames.joinToString(", ").takeIf { it.isNotEmpty() }
            }

            val tags = jo.getJSONArray("genres").mapJSON { genre ->
                MangaTag(
                    key = genre.getLong("id").toString(),
                    title = genre.getString("name"),
                )
            }.toSet()

            Manga(
                id = generateUid(id),
                title = title,
                altTitle = differentNames.joinToString(", ").takeIf { it.isNotEmpty() },
                url = "/$apiSuffix/info/$id",
                publicUrl = "https://$domain/g/$id",
                isNsfw = true,
                coverUrl = jo.getString("coverUrl"),
                state = null,
                description = description,
                tags = tags,
                author = authors,
                rating = 0f,
            )
        }
    }

    private fun parseMangaList(data: JSONArray): List<Manga> {
        return data.mapJSON { jo ->
            val id = jo.getLong("id")
            val title = jo.getString("title").takeIf { it.isNotEmpty() } ?: "Web chưa đặt tên"
            val description = jo.getStringOrNull("description")

            val differentNames = mutableSetOf<String>().apply {
                jo.optJSONArray("differentNames")?.let { namesArray ->
                    for (i in 0 until namesArray.length()) {
                        namesArray.optString(i)?.takeIf { it.isNotEmpty() }?.let { name ->
                            add(name)
                        }
                    }
                }.toString()
            }

            val authors = jo.optJSONArray("authors")?.let { authorsArray ->
                val authorNames = mutableListOf<String>()
                for (i in 0 until authorsArray.length()) {
                    authorsArray.optJSONObject(i)?.getString("name")?.takeIf { it.isNotEmpty() }
                        ?.let { name ->
                            authorNames.add(name)
                        }
                }
                authorNames.joinToString(", ").takeIf { it.isNotEmpty() }
            }

            val tags = jo.getJSONArray("genres").mapJSON { genre ->
                MangaTag(
                    key = genre.getLong("id").toString(),
                    title = genre.getString("name"),
                )
            }.toSet()

            Manga(
                id = generateUid(id),
                title = title,
                altTitle = differentNames.joinToString(", ").takeIf { it.isNotEmpty() },
                url = "/$apiSuffix/info/$id",
                publicUrl = "https://$domain/g/$id",
                rating = 0f,
                isNsfw = true,
                coverUrl = jo.getString("coverUrl"),
                state = null,
                tags = tags,
                description = description,
                author = authors,
            )
        }
    }

    override fun getDetails(url: String): Manga {
        val jo = webClient.httpGet(url.toAbsoluteUrl(domain).toHttpUrl()).parseJson()
        val id = jo.getLong("id")
        val title = jo.getString("title").takeIf { it.isNotEmpty() } ?: "Web chưa đặt tên"
        val description = jo.optString("description").takeIf { it.isNotEmpty() }

        val differentNames = mutableSetOf<String>().apply {
            jo.optJSONArray("differentNames")?.let { namesArray ->
                for (i in 0 until namesArray.length()) {
                    namesArray.optString(i)?.takeIf { it.isNotEmpty() }?.let { name ->
                        add(name)
                    }
                }
            }
        }

        val authors = jo.optJSONArray("authors")?.let { authorsArray ->
            val authorNames = mutableListOf<String>()
            for (i in 0 until authorsArray.length()) {
                authorsArray.optJSONObject(i)?.getString("name")?.takeIf { it.isNotEmpty() }
                    ?.let { name ->
                        authorNames.add(name)
                    }
            }
            authorNames.joinToString(", ").takeIf { it.isNotEmpty() }
        }

        val tags = jo.getJSONArray("genres").mapJSON { genre ->
            MangaTag(
                key = genre.getLong("id").toString(),
                title = genre.getString("name"),
            )
        }.toSet()

        return Manga(
            id = generateUid(id),
            title = title,
            altTitle = differentNames.joinToString(", ").takeIf { it.isNotEmpty() },
            url = "/$apiSuffix/info/$id",
            publicUrl = "https://$domain/g/$id",
            isNsfw = true,
            coverUrl = jo.getString("coverUrl"),
            state = null,
            description = description,
            tags = tags,
            author = authors,
            rating = 0f,
        )
    }

    override fun getChapters(url: String): List<MangaChapter> {
        // for uploader
        val jo = webClient.httpGet(url.toAbsoluteUrl(domain).toHttpUrl()).parseJson()
        val id = jo.getLong("id")
        val uploaderName = jo.getJSONObject("uploader").getString("displayName")

        // for get chapters
        val urlChaps = "/$apiSuffix/gallery/$id".toAbsoluteUrl(domain).toHttpUrl()
        val parsedChapters = webClient.httpGet(urlChaps).parseJsonArray()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US)
        return parsedChapters.mapJSON { jo ->
            MangaChapter(
                id = generateUid(jo.getLong("id")),
                name = jo.getString("title"),
                number = jo.getString("order").toFloat(),
                url = "/$apiSuffix/chapter?id=${jo.getLong("id")}",
                uploadDate = dateFormat.parse(jo.getString("createdAt"))?.time ?: 0L,
                scanlator = uploaderName,
                branch = null,
                volume = 0,
            )
        }.reversed()
    }

    override fun getPages(url: String): List<MangaPage> {
        val json = webClient.httpGet("https://$domain$url".toHttpUrl()).parseJson()
        val imageUrls = json.getJSONArray("pages").asTypedList<String>()
        return imageUrls.map { url ->
            MangaPage(
                id = generateUid(url),
                url = url,
                preview = null,
            )
        }
    }

    private fun fetchTags(): Set<MangaTag> {
        val response =
            webClient.httpGet("https://$domain/$apiSuffix/genres".toHttpUrl()).parseJsonArray()
        return response.mapJSONToSet { jo ->
            MangaTag(
                title = jo.getString("name").toTitleCase(sourceLocale),
                key = jo.getLong("id").toString(),
            )
        }
    }
}