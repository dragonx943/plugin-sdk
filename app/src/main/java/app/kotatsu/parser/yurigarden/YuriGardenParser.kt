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
import app.kotatsu.plugin.sdk.network.Paginator
import app.kotatsu.plugin.sdk.util.generateUid
import app.kotatsu.plugin.sdk.util.json.asTypedList
import app.kotatsu.plugin.sdk.util.json.getBooleanOrDefault
import app.kotatsu.plugin.sdk.util.json.getFloatOrDefault
import app.kotatsu.plugin.sdk.util.json.getStringOrNull
import app.kotatsu.plugin.sdk.util.json.mapJSON
import app.kotatsu.plugin.sdk.util.mapNotNullToSet
import app.kotatsu.plugin.sdk.util.nullIfEmpty
import app.kotatsu.plugin.sdk.util.oneOrThrowIfMany
import app.kotatsu.plugin.sdk.util.parseJson
import app.kotatsu.plugin.sdk.util.parseJsonArray
import app.kotatsu.plugin.sdk.util.toTitleCase
import app.kotatsu.plugin.sdk.util.urlEncoded
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.util.EnumSet
import java.util.Locale

class YuriGardenParser(
    authority: String,
) : MangaParser(authority) {

    private val paginator = Paginator(initialPageSize = 18)
    private val sourceLocale = Locale("vi_VN")

    override val filterCapabilities = MangaListFilterCapabilities(
        availableSortOrders = EnumSet.of(
            SortOrder.POPULARITY,
            SortOrder.RATING,
            SortOrder.UPDATED,
            SortOrder.NEWEST,
        ),
        isMultipleTagsSupported = true,
        isSearchSupported = true,
        isSearchWithFiltersSupported = true,
    )

    override val domain = "yurigarden.com"
    protected val apiSuffix = "api.$domain"

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
        ),
    )

    override fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = buildString {
            append("https://")
            append(apiSuffix)
            append("/comics")
            append("?page=")
            append(offset / 20)
            append("&limit=")
            append(20)
            append("&r18=")
            append("false") // true

            append("&sort=")
            append(when (order) {
                SortOrder.RELEVANCE -> "relevance"
                SortOrder.NEWEST -> "newest"
                SortOrder.NEWEST_ASC -> "oldest"
                SortOrder.RATING -> "rating"
                SortOrder.POPULARITY -> "popularity"
                else -> "relevance"
            })

            if (!filter.query.isNullOrEmpty()) {
                append("&search=")
                append(checkNotNull(filter.query).urlEncoded())
            }

            if (filter.states.isNotEmpty()) {
                filter.states.oneOrThrowIfMany()?.let {
                    append("&status=")
                    append(when (it) {
                        MangaState.ONGOING -> "ongoing"
                        MangaState.FINISHED -> "completed"
                        MangaState.PAUSED -> "hiatus"
                        MangaState.ABANDONED -> "cancelled"
                        else -> ""
                    })
                }
            }

            if (filter.tags.isNotEmpty()) {
				append("&genre=")
				append(filter.tags.joinToString(separator = ",") { it.key })
			}
        }

        val json = webClient.httpGet(url.toHttpUrl()).parseJson()
        val data = json.getJSONArray("comics")

        return data.mapJSON { jo ->
            val id = jo.getLong("id")
            Manga(
                id = generateUid(id),
                url = "/comics/$id",
                publicUrl = "https://$domain/comic/$id",
                title = jo.getString("title"),
                altTitle = null,
                coverUrl = jo.getString("thumbnail"),
                author = null,
                tags = emptySet(),
                state = when(jo.getString("status")) {
                    "ongoing" -> MangaState.ONGOING
                    "completed" -> MangaState.FINISHED
                    "hiatus" -> MangaState.PAUSED
                    "cancelled" -> MangaState.ABANDONED
                    else -> null
                },
                isNsfw = if (jo.getBooleanOrDefault("r18", false)) true else false,
                rating = jo.getFloatOrDefault("rating", -5f) / 5f,
            )
        }
    }

    override fun getDetails(url: String): Manga {
        val baseUrl = "https://$apiSuffix$url"
        val json = webClient.httpGet(baseUrl.toHttpUrl()).parseJson()

        val authors = json.optJSONArray("authors")?.asTypedList<JSONObject>()?.mapNotNullToSet {
            it.getStringOrNull("name")
        }

        val allTags = fetchTags()
        val tags = allTags.let { allTags ->
            json.optJSONArray("genres")?.asTypedList<String>()?.mapNotNullToSet { g ->
                allTags.find { x -> x.key == g }
            }
        }.orEmpty()

        return Manga(
            id = generateUid(json.getLong("id")),
            title = json.getString("title"),
            altTitle = json.getString("anotherName"),
            isNsfw = if (json.getBooleanOrDefault("r18", false)) {
                true
            } else {
                false
            },
            author = authors?.joinToString()?.nullIfEmpty(),
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
        val baseUrl = "https://$apiSuffix$url"
        val chapters = webClient.httpGet("$baseUrl/chapters".toHttpUrl()).parseJsonArray()
        return chapters.asTypedList<JSONObject>().map { jo ->
            val chapterId = jo.getLong("id")
            val pageUrls = jo.getJSONArray("pages").mapJSON { page ->
                page.getString("url")
            }
            MangaChapter(
                id = generateUid(chapterId),
                name = jo.getString("name"),
                number = jo.getString("order").toFloat(),
                volume = 0,
                url = pageUrls.joinToString("\n"),
                scanlator = null, // team
                uploadDate = jo.getLong("lastUpdated"),
                branch = null,
            )
        }
    }

    override fun getPages(url: String): List<MangaPage> {
        return url.split("\n").mapIndexed { index, url ->
            MangaPage(
                id = generateUid(index.toLong()),
                url = url,
                preview = null,
            )
        }
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
}