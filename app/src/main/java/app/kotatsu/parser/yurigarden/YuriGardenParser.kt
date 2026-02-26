package app.kotatsu.parser.yurigarden

import androidx.collection.arraySetOf
import app.kotatsu.plugin.sdk.core.Manga
import app.kotatsu.plugin.sdk.core.MangaChapter
import app.kotatsu.plugin.sdk.core.MangaListFilter
import app.kotatsu.plugin.sdk.core.MangaListFilterCapabilities
import app.kotatsu.plugin.sdk.core.MangaListFilterOptions
import app.kotatsu.plugin.sdk.core.MangaPage
import app.kotatsu.plugin.sdk.core.MangaParser
import app.kotatsu.plugin.sdk.core.MangaState
import app.kotatsu.plugin.sdk.core.MangaTag
import app.kotatsu.plugin.sdk.core.SortOrder
import app.kotatsu.plugin.sdk.network.CommonHeaders
import app.kotatsu.plugin.sdk.network.Paginator
import app.kotatsu.plugin.sdk.util.generateUid
import app.kotatsu.plugin.sdk.util.json.asTypedList
import app.kotatsu.plugin.sdk.util.json.getBooleanOrDefault
import app.kotatsu.plugin.sdk.util.json.getFloatOrDefault
import app.kotatsu.plugin.sdk.util.json.mapJSON
import app.kotatsu.plugin.sdk.util.mapChapters
import app.kotatsu.plugin.sdk.util.mapNotNullToSet
import app.kotatsu.plugin.sdk.util.oneOrThrowIfMany
import app.kotatsu.plugin.sdk.util.parseJson
import app.kotatsu.plugin.sdk.util.parseJsonArray
import app.kotatsu.plugin.sdk.util.toTitleCase
import app.kotatsu.plugin.sdk.util.urlEncoded
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import java.util.EnumSet
import java.util.Locale

class YuriGardenParser(
    authority: String,
) : MangaParser(authority) {

    private val paginator = Paginator(initialPageSize = 18)
    private val isR18Enable: Boolean = false
    private val sourceLocale = Locale.Builder()
        .setLanguage("vi")
        .setRegion("VN")
        .build()

    override val filterCapabilities = MangaListFilterCapabilities(
        availableSortOrders = EnumSet.of(
            SortOrder.NEWEST,
            SortOrder.NEWEST_ASC,
        ),
        isSearchSupported = true,
        isMultipleTagsSupported = true,
        isSearchWithFiltersSupported = true,
    )

    override val domain = "yurigarden.com"
    private val apiSuffix = "api.$domain"
    private val cdnSuffix = "db.$domain/storage/v1/object/public/yuri-garden-store"

    init {
        paginator.firstPage = 1
    }

    override fun getFilterOptions() = MangaListFilterOptions(
        availableTags = fetchTags(),
        availableStates = EnumSet.of(
            MangaState.ONGOING,
            MangaState.FINISHED,
            MangaState.ABANDONED,
            MangaState.PAUSED,
            MangaState.UPCOMING,
        ),
    )

    override fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = buildString {
            append("https://")
            append(apiSuffix)
            append("/api/comics")
            append("?page=")
            append(offset / 18 + 1)
            append("&limit=18")
            append("&r18=").append(isR18Enable)
            append("&sort=").append(
                when (order) {
                    SortOrder.NEWEST -> "newest"
                    SortOrder.NEWEST_ASC -> "oldest"
                    else -> "newest"
                }
            )
            if (!filter.query.isNullOrEmpty()) {
                append("&search=").append(filter.query!!.urlEncoded())
            }
            if (filter.states.isNotEmpty()) {
                filter.states.oneOrThrowIfMany()?.let { state ->
                    append("&status=").append(
                        when (state) {
                            MangaState.ONGOING -> "ongoing"
                            MangaState.FINISHED -> "completed"
                            MangaState.PAUSED -> "hiatus"
                            MangaState.ABANDONED -> "cancelled"
                            MangaState.UPCOMING -> "oncoming"
                        }
                    )
                }
            } else {
                append("&status=all")
            }
            append("&full=true")
            if (filter.tags.isNotEmpty()) {
                append("&genre=").append(filter.tags.joinToString(",") { it.key })
            }
        }

        val json = webClient.httpGet(url.toHttpUrl()).parseJson()
        val data = json.getJSONArray("comics")

        return data.mapJSON { jo ->
            val id = jo.getLong("id")
            val thumbnail = jo.getString("thumbnail")
            val altTitles = jo.optJSONArray("anotherNames")
                ?.asTypedList<String>()?.joinToString(", ") { it }
            val tags = fetchTags().let { allTags ->
                jo.optJSONArray("genres")?.asTypedList<String>()?.mapNotNullToSet { g ->
                    allTags.find { x -> x.key == g }
                }
            }.orEmpty()
            Manga(
                id = generateUid(id),
                url = "/comics/$id",
                publicUrl = "https://$domain/comic/$id",
                title = jo.getString("title"),
                altTitle = altTitles,
                coverUrl = if (thumbnail.startsWith("comics/"))
                    "https://$cdnSuffix/$thumbnail" else thumbnail,
                largeCoverUrl = if (thumbnail.startsWith("comics/"))
                    "https://$cdnSuffix/$thumbnail" else thumbnail,
                author = null,
                tags = tags,
                state = when (jo.optString("status")) {
                    "ongoing" -> MangaState.ONGOING
                    "completed" -> MangaState.FINISHED
                    "hiatus" -> MangaState.PAUSED
                    "cancelled" -> MangaState.ABANDONED
                    "oncoming" -> MangaState.UPCOMING
                    else -> null
                },
                description = jo.optString("description").orEmpty(),
                isNsfw = jo.getBooleanOrDefault("r18", false),
                rating = jo.getFloatOrDefault("rating", -5f) / 5f,
            )
        }
    }

    override fun getDetails(url: String): Manga {
        val json = webClient.httpGet("https://$apiSuffix/api$url".toHttpUrl()).parseJson()

        val authors = json.optJSONArray("authors")?.asTypedList<JSONObject>()
            ?.mapTo(HashSet()) { jo -> jo.getString("name") }.orEmpty()

        val altTitles = json.optJSONArray("anotherNames")
            ?.asTypedList<String>()?.joinToString(", ") { it }

        val allTags = fetchTags()
        val tags = json.optJSONArray("genres")?.asTypedList<String>()?.mapNotNullToSet { g ->
            allTags.find { x -> x.key == g }
        }.orEmpty()

        return Manga(
            id = generateUid(json.getLong("id")),
            title = json.getString("title"),
            altTitle = altTitles,
            isNsfw = json.getBooleanOrDefault("r18", false),
            author = authors.joinToString(", "),
            tags = tags,
            description = json.getString("description"),
            state = when (json.getString("status")) {
                "ongoing" -> MangaState.ONGOING
                "completed" -> MangaState.FINISHED
                "hiatus" -> MangaState.PAUSED
                "cancelled" -> MangaState.ABANDONED
                else -> null
            },
            rating = json.getFloatOrDefault("rating", -5f) / 5f,
            url = url,
            publicUrl = "https://$domain/$url",
            coverUrl = json.getString("thumbnail"),
            largeCoverUrl = null,
        )
    }

    override fun getChapters(url: String): List<MangaChapter> {
        val json = webClient.httpGet("https://$apiSuffix/api$url".toHttpUrl()).parseJson()
        val team = json.optJSONArray("groups")?.asTypedList<JSONObject>()?.flatMap { group ->
            group.optJSONArray("teams")
                ?.asTypedList<JSONObject>()
                ?.mapNotNull { it.optString("name") }
                ?: emptyList()
        }?.joinToString(", ").orEmpty()

        val chapters = webClient
            .httpGet("https://$apiSuffix/api/chapters/comic/${url.substringAfterLast("/")}".toHttpUrl())
            .parseJsonArray()
            .asTypedList<JSONObject>()

        return chapters.mapChapters { _, jo ->
            val chapId = jo.getLong("id")
            val chapterJson = webClient
                .httpGet("https://$apiSuffix/api/chapters/$chapId".toHttpUrl())
                .parseJson()

            val pageUrls = chapterJson
                .getJSONArray("pages")
                .asTypedList<JSONObject>()
                .map { page ->
                    val rawUrl = page.getString("url").replace("_credit", "")
                    val key = page.optString("key", "")

                    val imageUrl = if (rawUrl.startsWith("comics/")) {
                        "https://$cdnSuffix/$rawUrl"
                    } else {
                        rawUrl.toHttpUrlOrNull()?.toString() ?: rawUrl
                    }

                    if (key.isNotEmpty()) {
                        "content://$IMAGE_AUTHORITY/image?url=${imageUrl.urlEncoded()}&key=${key.urlEncoded()}"
                    } else {
                        imageUrl
                    }
                }

            MangaChapter(
                id = generateUid(chapId),
                name = jo.getString("name"),
                number = jo.getFloatOrDefault("order", 0f),
                volume = jo.optInt("volume", 0),
                url = pageUrls.joinToString("\n"),
                scanlator = team,
                uploadDate = jo.getLong("lastUpdated"),
                branch = null,
            )
        }.sortedBy { it.number }
    }

    override fun getPages(url: String): List<MangaPage> {
        return url.split("\n").map { pageUrl ->
            MangaPage(
                id = generateUid(pageUrl),
                url = pageUrl,
                preview = null,
            )
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .header("x-app-origin", "https://$domain")
            .header(CommonHeaders.USER_AGENT, "Kotatsu/9.0 (Android 16;;; en)")
            .header(CommonHeaders.REFERER, "https://$domain/")
            .header("Origin", "https://$domain")
            .build()
        return super.intercept(object : Interceptor.Chain by chain {
            override fun request() = newRequest
        })
    }

    private fun fetchTags(): Set<MangaTag> {
        val json = webClient.httpGet("https://$apiSuffix/resources/systems_vi.json".toHttpUrl()).parseJson()
        val genres = json.getJSONObject("genres")
        return genres.keys().asSequence().mapTo(arraySetOf()) { key ->
            val genre = genres.getJSONObject(key)
            MangaTag(
                title = genre.getString("name").toTitleCase(sourceLocale),
                key = genre.getString("slug"),
            )
        }
    }

    companion object {
        const val IMAGE_AUTHORITY = "app.kotatsu.parser.yurigarden.images"
    }
}