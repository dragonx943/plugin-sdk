package app.kotatsu.plugin.sdk.network

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response

class CommonHeadersInterceptor : Interceptor {

	override fun intercept(chain: Chain): Response {
		val request = chain.request()
		val headersBuilder = request.headers.newBuilder()
		if (headersBuilder[CommonHeaders.USER_AGENT] == null) {
			headersBuilder[CommonHeaders.USER_AGENT] = CommonHeaders.UA_FIREFOX_MOBILE
		}
		val newRequest = request.newBuilder().headers(headersBuilder.build()).build()
		return chain.proceed(newRequest)
	}
}
