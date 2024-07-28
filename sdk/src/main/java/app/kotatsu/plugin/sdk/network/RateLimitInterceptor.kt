package app.kotatsu.plugin.sdk.network

import app.kotatsu.plugin.sdk.core.exception.TooManyRequestExceptions
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly

class RateLimitInterceptor : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response {
		val response = chain.proceed(chain.request())
		if (response.code == 429) {
			val retryDate = response.header(CommonHeaders.RETRY_AFTER)?.toLongOrNull() ?: 0L
			val request = response.request
			response.closeQuietly()
			throw TooManyRequestExceptions(
				url = request.url.toString(),
				retryAt = retryDate,
			)
		}
		return response
	}
}
