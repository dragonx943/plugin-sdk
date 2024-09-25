package app.kotatsu.plugin.sdk.core

import app.kotatsu.plugin.sdk.network.CommonHeaders
import app.kotatsu.plugin.sdk.network.WebClient
import app.kotatsu.plugin.sdk.util.toAbsoluteUrl
import app.kotatsu.plugin.sdk.util.trySet
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.net.IDN

public abstract class MangaParser(internal val authority: String) : Interceptor {

    protected abstract val domain: String

    internal val filterOptionsLazy by lazy(::getFilterOptions)

    public abstract fun getFilterOptions(): MangaListFilterOptions

    public abstract val filterCapabilities: MangaListFilterCapabilities

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

    public abstract fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga>

    /**
     * Parse details for [Manga]: chapters list, description, large cover, etc.
     * Must return the same manga, may change any fields excepts id, url and source
     * @see Manga.copy
     */
    public abstract fun getDetails(url: String): Manga

    public abstract fun getChapters(url: String): List<MangaChapter>

    /**
     * Parse pages list for specified chapter.
     * @see MangaPage for details
     */
    public abstract fun getPages(url: String): List<MangaPage>

    /**
     * Fetch direct link to the page image.
     */
    public open fun getPageUrl(url: String): String = url.toAbsoluteUrl(domain)

    protected fun urlBuilder(subdomain: String? = null): HttpUrl.Builder {
        return HttpUrl.Builder()
            .scheme("https")
            .host(if (subdomain == null) domain else "$subdomain.$domain")
    }
}