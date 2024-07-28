package app.kotatsu.plugin.sdk.core.exception

import okio.IOException

class TooManyRequestExceptions(
	val url: String,
	val retryAt: Long,
) : IOException()
