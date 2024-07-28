package app.kotatsu.plugin.sdk.core.exception

class ParseException @JvmOverloads constructor(
	val shortMessage: String?,
	val url: String,
	cause: Throwable? = null,
) : RuntimeException("$shortMessage at $url", cause)