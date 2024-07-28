package app.kotatsu.plugin.sdk.network

import okhttp3.CacheControl

object CommonHeaders {

    const val REFERER = "Referer"
    const val USER_AGENT = "User-Agent"
    const val ACCEPT = "Accept"
    const val CONTENT_TYPE = "Content-Type"
    const val CONTENT_DISPOSITION = "Content-Disposition"
    const val COOKIE = "Cookie"
    const val CONTENT_ENCODING = "Content-Encoding"
    const val ACCEPT_ENCODING = "Accept-Encoding"
    const val AUTHORIZATION = "Authorization"
    const val CACHE_CONTROL = "Cache-Control"
    const val PROXY_AUTHORIZATION = "Proxy-Authorization"
    const val RETRY_AFTER = "Retry-After"

    val CACHE_CONTROL_NO_STORE: CacheControl
        get() = CacheControl.Builder().noStore().build()
    const val UA_FIREFOX_MOBILE =
        "Mozilla/5.0 (Android 14; Mobile; LG-M255; rv:123.0) Gecko/123.0 Firefox/123.0"
}
