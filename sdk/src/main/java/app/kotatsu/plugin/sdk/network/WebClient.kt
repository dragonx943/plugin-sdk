package app.kotatsu.plugin.sdk.network

import app.kotatsu.plugin.sdk.core.MangaParser
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.HttpStatusException

class WebClient(parser: MangaParser) {

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(CommonHeadersInterceptor())
        .addInterceptor(CloudFlareInterceptor())
        .addInterceptor(RateLimitInterceptor())
        .addInterceptor(parser)
        .build()

    fun httpGet(url: HttpUrl, extraHeaders: Headers? = null): Response {
        val request = Request.Builder()
            .get()
            .url(url)
            .addTags()
            .addExtraHeaders(extraHeaders)
        return httpClient.newCall(request.build()).execute().ensureSuccess()
    }

    fun httpHead(url: HttpUrl): Response {
        val request = Request.Builder()
            .head()
            .url(url)
            .addTags()
        return httpClient.newCall(request.build()).execute().ensureSuccess()
    }

    fun httpPost(url: HttpUrl, form: Map<String, String>, extraHeaders: Headers? = null): Response {
        val body = FormBody.Builder()
        form.forEach { (k, v) ->
            body.addEncoded(k, v)
        }
        val request = Request.Builder()
            .post(body.build())
            .url(url)
            .addTags()
            .addExtraHeaders(extraHeaders)
        return httpClient.newCall(request.build()).execute().ensureSuccess()
    }

    fun httpPost(url: HttpUrl, payload: String, extraHeaders: Headers? = null): Response {
        val body = FormBody.Builder()
        payload.split('&').forEach {
            val pos = it.indexOf('=')
            if (pos != -1) {
                val k = it.substring(0, pos)
                val v = it.substring(pos + 1)
                body.addEncoded(k, v)
            }
        }
        val request = Request.Builder()
            .post(body.build())
            .url(url)
            .addTags()
            .addExtraHeaders(extraHeaders)
        return httpClient.newCall(request.build()).execute().ensureSuccess()
    }

    fun httpPost(url: HttpUrl, body: JSONObject, extraHeaders: Headers? = null): Response {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = body.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .post(requestBody)
            .url(url)
            .addTags()
            .addExtraHeaders(extraHeaders)
        return httpClient.newCall(request.build()).execute().ensureSuccess()
    }

    private fun Request.Builder.addTags(): Request.Builder {
//		tag(MangaSource::class.java, mangaSource)
        return this
    }

    private fun Request.Builder.addExtraHeaders(headers: Headers? = null): Request.Builder {
        if (headers != null) {
            headers(headers)
        }
        return this
    }

    private fun Response.ensureSuccess(): Response {
        val exception: Exception? = when (code) { // Catch some error codes, not all
//			HttpURLConnection.HTTP_NOT_FOUND -> NotFoundException(message, request.url.toString())
//			HttpURLConnection.HTTP_UNAUTHORIZED -> request.tag(MangaSource::class.java)?.let {
//				AuthRequiredException(it)
//			} ?: HttpStatusException(message, code, request.url.toString())

            in 400..599 -> HttpStatusException(message, code, request.url.toString())
            else -> null
        }
        if (exception != null) {
            runCatching {
                close()
            }.onFailure {
                exception.addSuppressed(it)
            }
            throw exception
        }
        return this
    }
}
