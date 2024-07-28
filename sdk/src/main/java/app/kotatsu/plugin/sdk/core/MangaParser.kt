package app.kotatsu.plugin.sdk.core

import androidx.annotation.VisibleForTesting
import app.kotatsu.plugin.sdk.ipc.CapabilitiesCursor
import app.kotatsu.plugin.sdk.ipc.ChapterCursor
import app.kotatsu.plugin.sdk.ipc.MangaCursor
import app.kotatsu.plugin.sdk.ipc.PageCursor
import app.kotatsu.plugin.sdk.ipc.TagCursor
import app.kotatsu.plugin.sdk.network.CommonHeaders
import app.kotatsu.plugin.sdk.network.WebClient
import app.kotatsu.plugin.sdk.util.toAbsoluteUrl
import app.kotatsu.plugin.sdk.util.trySet
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.net.IDN
import java.util.Locale

abstract class MangaParser(
    val authority: String,
    val contentType: ContentType,
    val contentLocale: String?,
) : Interceptor {

    protected abstract val domain: String

    abstract val availableSortOrders: Set<SortOrder>

    /**
     * Supported [MangaState] variants for filtering. May be empty.
     *
     * For better performance use [EnumSet] for more than one item.
     */
    open val availableStates: Set<MangaState>
        get() = emptySet()


    open val availableContentRating: Set<ContentRating>
        get() = emptySet()

    /**
     * Whether parser supports filtering by more than one tag
     */
    open val isMultipleTagsSupported: Boolean = true

    /**
     * Whether parser supports tagsExclude field in filter
     */
    open val isTagsExclusionSupported: Boolean = false

    /**
     * Whether parser supports searching by string query using [MangaListFilter.Search]
     */
    open val isSearchSupported: Boolean = true

    open val sourceLocale: Locale
        get() = if (contentLocale == null) Locale.ROOT else Locale(contentLocale)

    val isNsfwSource = contentType == ContentType.HENTAI

    /**
     * Used as fallback if value of `sortOrder` passed to [getList] is null
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open val defaultSortOrder: SortOrder
        get() {
            val supported = availableSortOrders
            return SortOrder.entries.first { it in supported }
        }

    @JvmField
    @Suppress("LeakingThis")
    protected val webClient: WebClient = WebClient(this)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val headersBuilder = request.headers.newBuilder()
        if (headersBuilder[CommonHeaders.REFERER] == null) {
            val idn = IDN.toASCII(domain)
            headersBuilder.trySet(CommonHeaders.REFERER, "https://$idn/")
        }
        val newRequest = request.newBuilder().headers(headersBuilder.build()).build()
        return chain.proceed(newRequest)
    }

    abstract fun getList(offset: Int, filter: MangaListFilter?): MangaCursor

    /**
     * Parse details for [Manga]: chapters list, description, large cover, etc.
     * Must return the same manga, may change any fields excepts id, url and source
     * @see Manga.copy
     */
    abstract fun getDetails(url: String): MangaCursor

    abstract fun getChapters(url: String): ChapterCursor

    /**
     * Parse pages list for specified chapter.
     * @see MangaPage for details
     */
    abstract fun getPages(url: String): PageCursor

    /**
     * Fetch direct link to the page image.
     */
    open fun getPageUrl(url: String): String = url.toAbsoluteUrl(domain)

    /**
     * Fetch available tags (genres) for source
     */
    abstract fun getAvailableTags(): TagCursor

    /**
     * Fetch available locales for multilingual sources
     */
    open fun getAvailableLocales(): Set<Locale> = emptySet()

    protected fun urlBuilder(subdomain: String? = null): HttpUrl.Builder {
        return HttpUrl.Builder()
            .scheme("https")
            .host(if (subdomain == null) domain else "$subdomain.$domain")
    }

    fun getCapabilities() = CapabilitiesCursor(
        MangaSourceCapabilities(
            availableSortOrders = availableSortOrders,
            availableStates = availableStates,
            availableContentRating = availableContentRating,
            isMultipleTagsSupported = isMultipleTagsSupported,
            isTagsExclusionSupported = isTagsExclusionSupported,
            isSearchSupported = isSearchSupported,
            contentType = contentType,
            defaultSortOrder = defaultSortOrder,
            sourceLocale = sourceLocale
        )
    )
}